package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
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
import dev.toastbits.kotules.binder.runtime.util.PRIMITIVE_TYPES
import dev.toastbits.kotules.binder.runtime.util.appendParameters
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.await
import dev.toastbits.kotules.extension.type.ValueType

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

            if (kotuleInterface.classKind == ClassKind.INTERFACE) {
                addSuperinterface(kotuleInterface.toClassName())
                addProperties(kotuleInterface.getDeclaredProperties())
                addFunctions(kotuleInterface.getDeclaredFunctions())
            }
            else if (kotuleInterface.classKind == ClassKind.CLASS) {
                addFunction(
                    FunSpec.builder("getDataClass")
                        .returns(kotuleInterface.toClassName())
                        .addCode(buildString {
                            append("return with ($instanceName) {\n    ")
                            append(kotuleInterface.simpleName.asString())
                            append('(')
                            appendParameters(kotuleInterface.primaryConstructor!!.parameters)
                            append(")\n}")
                        })
                        .build()
                )
            }
        }.build()

    private fun TypeSpec.Builder.addProperties(properties: Sequence<KSPropertyDeclaration>) {
        for (property in properties) {
            addProperty(
                PropertySpec.builder(property.simpleName.asString(), property.type.toTypeName())
                    .apply {
                        property.getter?.also { getter ->
                            getter(
                                FunSpec.getterBuilder()
                                    .addCode("return $instanceName.${property.simpleName.asString()}")
                                    .build()
                            )
                        }
                    }
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(property.modifiers.mapNotNull { it.toKModifier() })
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
                addModifiers(KModifier.OVERRIDE)
                addModifiers(
                    function.modifiers
                        .filter { it != Modifier.SUSPEND }
                        .mapNotNull { it.toKModifier() }
                )

                function.returnType?.also { returnType ->
                    returns(returnType.toTypeName())
                }

                for (parameter in function.parameters) {
                    addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                }

                addCode(
                    buildString {
                        append("return $instanceName.${function.simpleName.asString()}(")
                        appendParameters(function.parameters)
                        append(')')
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
                        appendParameters(function.parameters)
                        append(").$awaitName()")

                        if (returnType != null) {
                            val returnTypeClassName: ClassName = returnType.resolve().toClassName()
                            if (PRIMITIVE_TYPES.contains(returnTypeClassName.canonicalName)) {
                                append(".value")
                            }
                            else {
                                val mapperName: String = KotuleRuntimeBinderConstants.getMapperName(returnTypeClassName.simpleName)
                                scope.importFromPackage(mapperName)
                                append(".let { $mapperName(it).getDataClass() }")
                            }
                        }
                    }
                )
            }
            .build()
}
