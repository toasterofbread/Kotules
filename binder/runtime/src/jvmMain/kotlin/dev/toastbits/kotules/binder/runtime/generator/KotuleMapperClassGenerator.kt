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
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.core.util.resolveTypeAliasQualifiedName
import dev.toastbits.kotules.binder.core.util.shouldBeSerialised
import dev.toastbits.kotules.core.type.ValueType
import dev.toastbits.kotules.core.util.LIST_TYPES
import dev.toastbits.kotules.core.util.PRIMITIVE_TYPES
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.await
import dev.toastbits.kotules.extension.util.kotulesJsonInstance

class KotuleMapperClassGenerator(
    private val scope: FileGenerator.Scope
) {
    private val instanceName: String = KotuleCoreBinderConstants.MAPPER_INSTANCE_NAME

    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration,
        force: Boolean = false
    ): TypeSpec? =
        if (!force && (scope.target == KmpTarget.COMMON || scope.target == KmpTarget.JVM)) null
        else TypeSpec.classBuilder(name).apply {
            val inputClassName: ClassName = scope.importFromPackage(
                KotuleCoreBinderConstants.getInputBindingName(
                    kotuleInterface
                )
            )

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
                PropertySpec.Companion.builder(
                    property.simpleName.asString(),
                    property.type.toTypeName()
                )
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
                        appendFunctionCallReturn(function, returnType, true)
                    }
                )
            }
            .build()

    private fun StringBuilder.appendFunctionCallReturn(
        function: KSFunctionDeclaration,
        returnType: KSTypeReference?,
        canBePrimitive: Boolean,
        resultTransform: String = ""
    ) {
        val paramTransforms: List<String?> =
            function.parameters.map { param ->
                getFunctionParameterTransformSuffix(param.type.resolve()).ifBlank { null }
            }

        val paramNamePrefix: String = "_parameter"
        for ((index, param) in function.parameters.withIndex()) {
            val transform: String = paramTransforms[index] ?: continue
            val name: String = "$paramNamePrefix$index"
            appendLine("val $name = ${param.name!!.asString()}$transform")
        }

        if (returnType != null) {
            append("return ")
        }

        append("$instanceName.${function.simpleName.asString()}(")

        for ((index, param) in function.parameters.withIndex()) {
            val name: String =
                if (paramTransforms[index] != null) "$paramNamePrefix$index"
                else param.name!!.asString()

            append(name)
            if (index + 1 != function.parameters.size) {
                append(", ")
            }
        }

        append(')')
        append(resultTransform)

        if (returnType != null) {
            append(getPropertyOrFunctionValueTransformSuffix(returnType.resolve(), canBePrimitive))
        }
    }
    
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

                        appendFunctionCallReturn(function, returnType, false, resultTransform = ".await()")
                    }
                )
            }
            .build()

    private fun getFunctionParameterTransformSuffix(type: KSType): String = buildString {
        val qualifiedName: String = type.resolveTypeAliasQualifiedName()

        if (PRIMITIVE_TYPES.contains(qualifiedName)) {
            return@buildString
        }

        if (type.declaration.shouldBeSerialised()) {
            val kotulesJsonInstance: String = (::kotulesJsonInstance).name
            scope.import("dev.toastbits.kotules.extension.util", kotulesJsonInstance)
            scope.import("kotlinx.serialization", "encodeToString")

            append(".let { $kotulesJsonInstance.encodeToString(it) }")
            return@buildString
        }

        if (qualifiedName.startsWith("kotlin.Function")) {
            val functionName: String = "lambda"
            val argumentPrefix: String = "arg"

            append(".let { $functionName -> { ")

            val argCount: Int =
                if (qualifiedName == "kotlin.Function") 0
                else qualifiedName.removePrefix("kotlin.Function").toInt()

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

        val typeDeclaration: KSClassDeclaration = type.declaration as KSClassDeclaration
        val bindingName: String = KotuleCoreBinderConstants.getInputBindingName(typeDeclaration)
        appendLine(".let { $bindingName().apply { ")
        for (function in typeDeclaration.getDeclaredFunctions()) {
            val functionName: String = function.simpleName.asString()
            appendLine("$functionName = it::$functionName")
        }
        append(" } }")
    }

    private fun getPropertyOrFunctionValueTransformSuffix(type: KSType, canBePrimitive: Boolean): String = buildString {
        if (type.declaration.shouldBeSerialised()) {
            val kotulesJsonInstance: String = (::kotulesJsonInstance).name
            scope.import("dev.toastbits.kotules.extension.util", kotulesJsonInstance)
            append(".let { $kotulesJsonInstance.decodeFromString(it) }")
            return@buildString
        }

        val qualifiedName: String = type.resolveTypeAliasQualifiedName()
        if (LIST_TYPES.contains(qualifiedName)) {
            scope.import("dev.toastbits.kotules.core.type.input", "getListValue")
            append(".getListValue().map { it.value }")
        }
        else if (!canBePrimitive || !PRIMITIVE_TYPES.contains(qualifiedName)) {
            append(".value")
        }
    }
}