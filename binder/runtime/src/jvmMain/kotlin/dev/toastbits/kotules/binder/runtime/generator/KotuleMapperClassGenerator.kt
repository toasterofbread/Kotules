package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.binder.runtime.util.appendParameters
import dev.toastbits.kotules.binder.runtime.util.resolveTypeAlias
import dev.toastbits.kotules.binder.runtime.util.shouldBeSerialsied
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.await
import dev.toastbits.kotules.core.type.ValueType
import dev.toastbits.kotules.extension.util.LIST_TYPES
import dev.toastbits.kotules.extension.util.PRIMITIVE_TYPES
import dev.toastbits.kotules.extension.util.kotulesJsonInstance

internal class KotuleMapperClassGenerator(
    private val scope: FileGenerator.Scope
) {
    private val instanceName: String = KotuleRuntimeBinderConstants.MAPPER_INSTANCE_NAME

    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration
    ): TypeSpec? =
        if (scope.target == KmpTarget.COMMON || scope.target == KmpTarget.JVM) null
        else TypeSpec.classBuilder(name).apply {
            val inputClassName: ClassName = scope.importFromPackage(KotuleRuntimeBinderConstants.getInputBindingName(kotuleInterface))

            addModifiers(KModifier.INTERNAL)

            primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(instanceName, inputClassName)
                    .build()
            )

            addProperty(
                PropertySpec.builder(instanceName, inputClassName)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(instanceName)
                    .build()
            )

            addSuperinterface(kotuleInterface.toClassName())
            addProperties(kotuleInterface.getDeclaredProperties())
            addFunctions(kotuleInterface.getDeclaredFunctions())
        }.build()

    private fun TypeSpec.Builder.addProperties(properties: Sequence<KSPropertyDeclaration>) {
        for (property in properties) {
            addProperty(
                PropertySpec.builder(property.simpleName.asString(), property.type.toTypeName())
                    .apply {
                        property.getter?.also { getter ->
                            getter(
                                FunSpec.getterBuilder()
                                    .addCode(buildString {
                                        append("return $instanceName.${property.simpleName.asString()}")
                                        append(getPropertyOrFunctionValueTransformSuffix(property.type.resolve(), true))
                                    })
                                    .build()
                            )
                        }
                    }
                    .addModifiers(KModifier.OVERRIDE)
//                    .addModifiers(property.modifiers.mapNotNull { it.toKModifier() })
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addFunctions(functions: Sequence<KSFunctionDeclaration>) {
        for (function in functions) {
            if (function.isConstructor()) {
                continue
            }

            if (function.modifiers.contains(Modifier.SUSPEND)) {
                addFunction(generateSuspendFunction(function))
            }
            else {
                addFunction(generateNonSuspendFunction(function))
            }
        }
    }

    private fun generateNonSuspendFunction(function: KSFunctionDeclaration): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
                val returnType: KSTypeReference? = function.returnType

                addModifiers(KModifier.OVERRIDE)
//                addModifiers(
//                    function.modifiers
//                        .filter { it != Modifier.SUSPEND }
//                        .mapNotNull { it.toKModifier() }
//                )

                if (returnType != null) {
                    returns(returnType.toTypeName())
                }

                for (parameter in function.parameters) {
                    addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                }

                addCode(
                    buildString {
                        if (returnType != null) {
                            append("return ")
                        }

                        append("$instanceName.${function.simpleName.asString()}(")
                        appendParameters(function.parameters) { it + getFunctionParameterTransformSuffix(this) }
                        append(')')

                        if (returnType != null) {
                            append(getPropertyOrFunctionValueTransformSuffix(returnType.resolve(), true))
                        }
                    }
                )
            }
            .build()

    private fun generateSuspendFunction(function: KSFunctionDeclaration): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
                val returnType: KSTypeReference? = function.returnType

                addModifiers(KModifier.OVERRIDE)
                addModifiers(function.modifiers.mapNotNull { it.toKModifier() })

                if (returnType != null) {
                    returns(returnType.toTypeName())
                }

                for (parameter in function.parameters) {
                    addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                }

                addCode(
                    buildString {
                        val awaitName: String = KotulePromise<ValueType>::await.name
                        val awaitPackage: String = KotulePromise::class.java.packageName

                        scope.import(awaitPackage, awaitName)

                        if (returnType != null) {
                            append("return ")
                        }
                        append("$instanceName.${function.simpleName.asString()}(")
                        appendParameters(function.parameters) { it + getFunctionParameterTransformSuffix(this) }
                        append(").$awaitName()")

                        if (returnType != null) {
                            append(getPropertyOrFunctionValueTransformSuffix(returnType.resolve(), false))
                        }
                    }
                )
            }
            .build()

    private fun getFunctionParameterTransformSuffix(type: KSType): String = buildString {
        if (type.declaration.shouldBeSerialsied()) {
            val kotulesJsonInstance: String = (::kotulesJsonInstance).name
            scope.import("dev.toastbits.kotules.extension.util", kotulesJsonInstance)
            scope.import("kotlinx.serialization", "encodeToString")

            append(".let { $kotulesJsonInstance.encodeToString(it) }")
            return@buildString
        }

        val canonicalName: String = type.resolveTypeAlias()
        if (canonicalName.startsWith("kotlin.Function")) {
            val functionName: String = "lambda"
            val argumentPrefix: String = "arg"

            append(".let { $functionName -> { ")

            val argCount: Int =
                if (canonicalName == "kotlin.Function") 0
                else canonicalName.removePrefix("kotlin.Function").toInt()

            if (argCount > 0) {
                for (arg in 0 until argCount) {
                    append("$argumentPrefix$arg")
                    if (arg + 1 != argCount) {
                        append(", ")
                    }
                }
                append(" ->")
            }

            append(functionName)
            append('(')
            for (arg in 0 until argCount) {
                append("$argumentPrefix$arg")
                append(getPropertyOrFunctionValueTransformSuffix(type.arguments[arg].type!!.resolve(), true))

                if (arg + 1 != argCount) {
                    append(", ")
                }
            }
            append(')')

            append(" } }")
            return@buildString
        }
    }

    private fun getPropertyOrFunctionValueTransformSuffix(type: KSType, canBePrimitive: Boolean): String = buildString {
        if (type.declaration.shouldBeSerialsied()) {
            val kotulesJsonInstance: String = (::kotulesJsonInstance).name
            scope.import("dev.toastbits.kotules.extension.util", kotulesJsonInstance)
            append(".let { $kotulesJsonInstance.decodeFromString(it) }")
            return@buildString
        }

        val qualifiedName: String = type.resolveTypeAlias()
        if (LIST_TYPES.contains(qualifiedName)) {
            scope.import("dev.toastbits.kotules.core.type.input", "getListValue")
            append(".getListValue().map { it.value }")
        }
        else if (!canBePrimitive || !PRIMITIVE_TYPES.contains(qualifiedName)) {
            append(".value")
        }
    }
}
