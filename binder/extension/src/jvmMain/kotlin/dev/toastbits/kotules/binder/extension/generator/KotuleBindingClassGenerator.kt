package dev.toastbits.kotules.binder.extension.generator

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.extension.util.KotuleExtensionBinderConstants
import dev.toastbits.kotules.binder.runtime.generator.FileGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.appendParameters
import dev.toastbits.kotules.extension.OutKotulePromise
import dev.toastbits.kotules.extension.PlatformJsExport
import dev.toastbits.kotules.extension.PlatformJsName
import dev.toastbits.kotules.extension.kotulePromise
import dev.toastbits.kotules.extension.type.OutValue
import dev.toastbits.kotules.extension.type.OutValueContainer
import dev.toastbits.kotules.extension.util.LIST_TYPES
import dev.toastbits.kotules.extension.util.PRIMITIVE_TYPES
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

            val instanceType: TypeName =
                try {
                    kotuleClass.asType(emptyList()).toTypeName()
                }
                catch (e: Throwable) {
                    throw RuntimeException("$name $kotuleClass", e)
                }
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

            addProperties(kotuleClass.getAllProperties().associate { it.simpleName.asString() to it.type.resolve() })

            val functions: Sequence<KSFunctionDeclaration> =
                kotuleClass.getAllFunctions().filterRelevantFunctions(kotuleClass)
            addFunctions(functions)

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

    private fun TypeSpec.Builder.addProperties(properties: Map<String, KSType>) {
        for ((propertyName, propertyType) in properties) {
            val outPropertyType: TypeName
            val outPropertyTypeConstructor: String?

            val isPrimitive: Boolean = PRIMITIVE_TYPES.contains(propertyType.toClassName().canonicalName)
            if (isPrimitive) {
                if (LIST_TYPES.contains(propertyType.toClassName().canonicalName)) {
                    outPropertyType = OutValueContainer::class.asClassName()
                        .plusParameter(WildcardTypeName.producerOf(Any::class.asTypeName().copy(nullable = true)))

                    val outValue: (Any) -> Any = ::OutValue
                    outPropertyTypeConstructor = (outValue as KCallable<*>).name
                    scope.import("dev.toastbits.kotules.extension.type", outPropertyTypeConstructor)
                }
                else {
                    outPropertyType = propertyType.toTypeName()
                    outPropertyTypeConstructor = null
                }
            }
            else {
                val outputBindingName: String = KotuleExtensionBinderConstants.getOutputBindingName(propertyType.toClassName().simpleName)
                outPropertyType =
                    scope.generateNew(scope.resolveInPackage(outputBindingName)) {
                        file.addType(KotuleBindingClassGenerator(this).generate(outputBindingName, propertyType.declaration as KSClassDeclaration)!!)
                    }
                outPropertyTypeConstructor = outputBindingName
            }

            addProperty(
                PropertySpec.builder(propertyName, outPropertyType.copy(nullable = propertyType.isMarkedNullable))
                    .apply {
                        getter(
                            FunSpec.getterBuilder()
                                .addCode(
                                    buildString {
                                        append("return ")
                                        if (outPropertyTypeConstructor == null) {
                                            append("$instanceName.$propertyName")
                                        }
                                        else {
                                            append("$instanceName.$propertyName")
                                            if (propertyType.isMarkedNullable) {
                                                append('?')
                                            }
                                            append(".let {\n$outPropertyTypeConstructor(")

                                            if (isPrimitive) {
                                                append("it")

                                                if (LIST_TYPES.contains(propertyType.toClassName().canonicalName)) {
                                                    val listItemType: KSType = propertyType.arguments.single().type!!.resolve()
                                                    val listItemIsPrimitive: Boolean = PRIMITIVE_TYPES.contains(listItemType.toClassName().canonicalName)

                                                    append(".map { ")
                                                    if (listItemIsPrimitive) {
                                                        append(outPropertyTypeConstructor)
                                                        append("(it)")
                                                    }
                                                    else {
                                                        append(KotuleExtensionBinderConstants.getOutputBindingName(listItemType.toClassName().simpleName))
                                                        append('(')
                                                        appendParameters((listItemType.declaration as KSClassDeclaration).primaryConstructor!!.parameters) { "it.$it" }
                                                        append(')')
                                                    }
                                                    append(" }")
                                                }

                                            }
                                            else {
                                                val params: List<KSValueParameter> = (propertyType.declaration as KSClassDeclaration).primaryConstructor?.parameters.orEmpty()
                                                appendParameters(params) { "it.$it" }
                                            }
                                            append(")\n}")
                                        }
                                    }
                                )
                                .build()
                        )
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

                        val returnType: KSType = function.returnType!!.resolve()
                        val returnTypeDeclaration: KSClassDeclaration = returnType.declaration as KSClassDeclaration
                        val isPrimitive: Boolean = PRIMITIVE_TYPES.contains(returnType.toClassName().canonicalName)

                        if (isPrimitive) {
                            val outValue: (Any) -> Any = ::OutValue
                            val outValueName: String = (outValue as KCallable<*>).name

                            scope.import("dev.toastbits.kotules.extension.type", outValueName)
                            append("$outValueName(")

                            append("$instanceName.${function.simpleName.asString()}(")
                            for ((index, parameter) in function.parameters.withIndex()) {
                                append(parameter.name!!.asString())
                                if (index + 1 != function.parameters.size) {
                                    append(", ")
                                }
                            }
                            append(')')

                            if (LIST_TYPES.contains(returnType.toClassName().canonicalName)) {
                                val listItemType: KSType = returnType.arguments.single().type!!.resolve()
                                val listItemIsPrimitive: Boolean = PRIMITIVE_TYPES.contains(listItemType.toClassName().canonicalName)

                                append(".map { ")
                                if (listItemIsPrimitive) {
                                    append(outValueName)
                                    append("(it)")
                                }
                                else {
                                    append(KotuleExtensionBinderConstants.getOutputBindingName(listItemType.toClassName().simpleName))
                                    append('(')
                                    appendParameters((listItemType.declaration as KSClassDeclaration).primaryConstructor!!.parameters) { "it.$it" }
                                    append(')')
                                }
                                append(" }")
                            }

                            append(")\n}")
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


    private fun Sequence<KSFunctionDeclaration>.filterRelevantFunctions(kotuleClass: KSClassDeclaration): Sequence<KSFunctionDeclaration> =
        filter { function ->
            val functionName: String = function.simpleName.asString()
            if (Any::class.java.declaredMethods.any { it.name == functionName }) {
                return@filter false
            }

            if (kotuleClass.modifiers.contains(Modifier.DATA)) {
                if (functionName == "copy") {
                    return@filter false
                }

                if (
                    (1 .. kotuleClass.primaryConstructor!!.parameters.size)
                        .any { functionName == "component$it" }
                ) {
                    return@filter false
                }
            }

            return@filter true
        }
}
