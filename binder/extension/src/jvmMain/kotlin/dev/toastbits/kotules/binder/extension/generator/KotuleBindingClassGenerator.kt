package dev.toastbits.kotules.binder.extension.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.extension.util.KotuleExtensionBinderConstants
import dev.toastbits.kotules.binder.runtime.generator.FileGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.PRIMITIVE_TYPES
import dev.toastbits.kotules.binder.runtime.util.appendParameters
import dev.toastbits.kotules.extension.OutKotulePromise
import dev.toastbits.kotules.extension.PlatformJsExport
import dev.toastbits.kotules.extension.PlatformJsName
import dev.toastbits.kotules.extension.kotulePromise
import dev.toastbits.kotules.extension.type.OutValue
import kotlin.reflect.KCallable

internal class KotuleBindingClassGenerator(
    private val scope: FileGenerator.Scope
) {
    private val instanceName: String = KotuleExtensionBinderConstants.OUTPUT_BINDING_INSTANCE_NAME

    fun generate(
        name: String,
        kotuleClass: KSClassDeclaration
    ): TypeSpec? =
        if (scope.target != KmpTarget.COMMON) null
        else TypeSpec.classBuilder(name).apply {
            val primaryConstructor: KSFunctionDeclaration? = kotuleClass.primaryConstructor

            val instanceType: TypeName = kotuleClass.asType(emptyList()).toTypeName()
            addProperty(
                PropertySpec.builder(instanceName, instanceType)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(buildString {
                        append("${kotuleClass.simpleName.asString()}(")
                        appendParameters(primaryConstructor?.parameters.orEmpty())
                        append(')')
                    })
                    .build()
            )

            addAnnotation(PlatformJsExport::class)

            addAnnotation(
                AnnotationSpec.builder(PlatformJsName::class)
                    .addMember("\"${kotuleClass.simpleName.asString()}\"")
                    .build()
            )

            addProperties(kotuleClass.getDeclaredProperties())
            addFunctions(kotuleClass.getDeclaredFunctions())

            if (primaryConstructor != null) {
                primaryConstructor(
                    FunSpec.constructorBuilder()
                        .apply {
                            for (param in primaryConstructor.parameters) {
                                addParameter(param.name!!.asString(), param.type.toTypeName())
                            }
                        }
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
                                FunSpec.getterBuilder().addCode(
                                    "return $instanceName.${property.simpleName.asString()}"
                                ).build()
                            )
                        }
                    }
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addFunctions(functions: Sequence<KSFunctionDeclaration>) {
        for (function in functions) {
            if (function.isConstructor()) {
                continue
            }

            if (Any::class.java.declaredMethods.any { it.name == function.simpleName.asString() }) {
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
                addModifiers(
                    function.modifiers
                        .filter { it != Modifier.SUSPEND && it != Modifier.OVERRIDE }
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
                        for ((index, parameter) in function.parameters.withIndex()) {
                            append(parameter.name!!.asString())
                            if (index + 1 != function.parameters.size) {
                                append(", ")
                            }
                        }
                        append(')')
                    }
                )
            }
            .build()

    private fun generateSuspendFunction(function: KSFunctionDeclaration): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
                addModifiers(
                    function.modifiers
                        .filter { it != Modifier.SUSPEND && it != Modifier.OVERRIDE }
                        .mapNotNull { it.toKModifier() }
                )

                returns(OutKotulePromise::class)

                for (parameter in function.parameters) {
                    addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                }

                addCode(
                    buildString {
                        val kotulePromise: (suspend () -> Any) -> Any = ::kotulePromise
                        val kotulePromiseName: String = (kotulePromise as KCallable<*>).name

                        scope.import("dev.toastbits.kotules.extension", kotulePromiseName)
                        append("return $kotulePromiseName { ")

                        scope.import(OutValue::class)

                        val returnType: KSType = function.returnType!!.resolve()
                        val returnTypeDeclaration: KSClassDeclaration = returnType.declaration as KSClassDeclaration
                        val isPrimitive: Boolean = PRIMITIVE_TYPES.contains(returnType.toClassName().canonicalName)

                        if (isPrimitive) {
                            append("${OutValue::class.simpleName!!}(")
                            append("$instanceName.${function.simpleName.asString()}(")
                            for ((index, parameter) in function.parameters.withIndex()) {
                                append(parameter.name!!.asString())
                                if (index + 1 != function.parameters.size) {
                                    append(", ")
                                }
                            }
                            append("))\n}")
                        }
                        else {
                            val outputBindingName: String = KotuleExtensionBinderConstants.getOutputBindingName(returnType.toClassName().simpleName)
                            scope.generateNew(scope.resolveInPackage(outputBindingName)) {
                                file.addType(KotuleBindingClassGenerator(this).generate(outputBindingName, returnTypeDeclaration)!!)
                            }

                            append("with ($instanceName.${function.simpleName.asString()}(")
                            appendParameters(function.parameters)
                            append(")) { $outputBindingName(")
                            appendParameters(returnTypeDeclaration.primaryConstructor!!.parameters)
                            append(")\n} }")
                        }
                    }
                )
            }
            .build()
}
