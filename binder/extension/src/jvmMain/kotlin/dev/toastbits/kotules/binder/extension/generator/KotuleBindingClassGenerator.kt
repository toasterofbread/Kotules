package dev.toastbits.kotules.binder.extension.generator

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
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
import dev.toastbits.kotules.binder.runtime.util.filterRelevantFunctions
import dev.toastbits.kotules.extension.OutKotulePromise
import dev.toastbits.kotules.extension.PlatformJsExport
import dev.toastbits.kotules.extension.PlatformJsName
import dev.toastbits.kotules.extension.kotulePromise
import dev.toastbits.kotules.extension.type.OutValue
import dev.toastbits.kotules.extension.type.OutValueContainer
import dev.toastbits.kotules.extension.util.LIST_TYPES
import dev.toastbits.kotules.extension.util.PRIMITIVE_TYPES
import kotlin.reflect.KCallable

private val FileGenerator.Scope.OutValue: String
    get() {
        val outValue: (Any) -> Any = ::OutValue
        val outValueName: String = (outValue as KCallable<*>).name
        import("dev.toastbits.kotules.extension.type", outValueName)
        return outValueName
    }

fun FileGenerator.Scope.getInteropTypeAndConstructorFor(type: KSType, canBePrimitive: Boolean = false): Pair<TypeName, String?> {
    val isPrimitive: Boolean = PRIMITIVE_TYPES.contains(type.toClassName().canonicalName)
    val isListType: Boolean = isPrimitive && LIST_TYPES.contains(type.toClassName().canonicalName)

    log("GEN ${type.toClassName().canonicalName} $isPrimitive $isListType")

    if (canBePrimitive && !isListType && isPrimitive) {
        return type.toTypeName() to null
    }
    else {
        if (isPrimitive) {
            val outputType: ParameterizedTypeName =
                OutValueContainer::class.asClassName()
                    .plusParameter(WildcardTypeName.producerOf(Any::class.asTypeName().copy(nullable = true)))

            return outputType to OutValue
        }

        val outputBindingName: String = KotuleExtensionBinderConstants.getOutputBindingName(type.toClassName().simpleName)
        val outputType: ClassName =
            generateNew(resolveInPackage(outputBindingName)) {
                file.addType(KotuleBindingClassGenerator(this).generate(outputBindingName, type.declaration as KSClassDeclaration)!!)
            }

        return outputType to outputBindingName
    }

//    val canonicalName: String = type.toClassName().canonicalName
//    if (canBePrimitive && PRIMITIVE_TYPES.contains(canonicalName) && !LIST_TYPES.contains(canonicalName)) {
//        return type.toTypeName()
//    }
//
//    type.getBuiltInInputWrapperClass(this)?.also { return it }
//
//    val declaration: KSDeclaration = type.declaration
//    check(declaration is KSClassDeclaration) { declaration }
//
//    val typeClass: ClassName = resolveInPackage(KotuleRuntimeBinderConstants.getInputBindingName(type.toClassName().simpleName))
//    return generateNew(typeClass) {
//        interfaceGenerator.generate(typeClass.simpleName, declaration)?.also {
//            file.addType(it)
//        }
//    }
}


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
        }.build()

    private fun TypeSpec.Builder.addProperties(properties: Map<String, KSType>) {
        for ((propertyName, propertyType) in properties) {
            val (outPropertyType: TypeName, outPropertyTypeConstructor: String?) = scope.getInteropTypeAndConstructorFor(propertyType)
            val isPrimitive: Boolean = PRIMITIVE_TYPES.contains(propertyType.toClassName().canonicalName)

            addProperty(
                PropertySpec.builder(propertyName, outPropertyType.copy(nullable = propertyType.isMarkedNullable))
                    .apply {
                        getter(
                            FunSpec.getterBuilder()
                                .addCode(
                                    buildString {
                                        append("return ")
                                        append("$instanceName.$propertyName")

                                        if (outPropertyTypeConstructor != null) {
                                            temp(
                                                propertyType,
                                                outPropertyTypeConstructor,
                                                isPrimitive
                                            )
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

    private fun StringBuilder.temp(
        propertyType: KSType,
        outPropertyTypeConstructor: String?,
        isPrimitive: Boolean
    ) {
        if (propertyType.isMarkedNullable) {
            append('?')
        }
        append(".let {\n$outPropertyTypeConstructor(")

        if (isPrimitive) {
            append("it")

            if (LIST_TYPES.contains(propertyType.toClassName().canonicalName)) {
                appendListMapSuffix(propertyType)
            }
        }
        else {
            appendParameters((propertyType.declaration as KSClassDeclaration).primaryConstructor!!.parameters) { "it.$it" }
        }
        append(")\n}")
    }

    private fun StringBuilder.appendListMapSuffix(propertyType: KSType) {
        val listItemType: KSType = propertyType.arguments.single().type!!.resolve()
        val listItemIsPrimitive: Boolean =
            PRIMITIVE_TYPES.contains(listItemType.toClassName().canonicalName)

        append(".map { ")
        if (listItemIsPrimitive) {
            append(scope.OutValue)
            append("(it)")
        } else {
            append(KotuleExtensionBinderConstants.getOutputBindingName(listItemType.toClassName().simpleName))
            append('(')
            appendParameters((listItemType.declaration as KSClassDeclaration).primaryConstructor!!.parameters) { "it.$it" }
            append(')')
        }
        append(" }")
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

    private fun FunSpec.Builder.addInteropParameters(parameters: List<KSValueParameter>) {
        for (parameter in parameters) {
            addParameter(parameter.name!!.asString(), scope.getInteropTypeAndConstructorFor(parameter.type.resolve(), canBePrimitive = true).first)
        }
    }

    private fun generateNonSuspendFunction(function: KSFunctionDeclaration): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
//                addModifiers(
//                    function.modifiers
//                        .filter { it != Modifier.SUSPEND && it != Modifier.OVERRIDE }
//                        .mapNotNull { it.toKModifier() }
//                )

                function.returnType?.also { returnType ->
                    returns(scope.getInteropTypeAndConstructorFor(returnType.resolve(), true).first)
                }

                addInteropParameters(function.parameters)

                if (!function.isAbstract) {
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

                addInteropParameters(function.parameters)

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
                                appendListMapSuffix(returnType)
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
                            appendParameters((returnType.declaration as KSClassDeclaration).primaryConstructor?.parameters.orEmpty())
                            append(")}\n}")
                        }
                    }
                )
            }
            .build()
}
