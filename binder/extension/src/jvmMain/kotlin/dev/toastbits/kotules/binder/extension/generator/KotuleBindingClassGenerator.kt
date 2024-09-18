package dev.toastbits.kotules.binder.extension.generator

import com.google.devtools.ksp.isAbstract
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
import dev.toastbits.kotules.binder.core.util.getAbstractFunctions
import dev.toastbits.kotules.binder.core.util.isListType
import dev.toastbits.kotules.binder.core.util.isPrimitiveType
import dev.toastbits.kotules.binder.core.util.resolveTypeAlias
import dev.toastbits.kotules.binder.core.util.resolveTypeAliasQualifiedName
import dev.toastbits.kotules.extension.OutKotulePromise
import dev.toastbits.kotules.extension.PlatformJsExport
import dev.toastbits.kotules.extension.PlatformJsName
import dev.toastbits.kotules.extension.kotulePromise
import dev.toastbits.kotules.extension.type.OutValue
import dev.toastbits.kotules.extension.type.OutValueContainer
import kotlin.reflect.KCallable

private val FileGenerator.Scope.OutValue: String
    get() {
        val outValue: (Any) -> Any = ::OutValue
        val outValueName: String = (outValue as KCallable<*>).name
        import("dev.toastbits.kotules.extension.type", outValueName)
        return outValueName
    }

fun FileGenerator.Scope.getInteropTypeAndConstructorFor(type: KSType, canBePrimitive: Boolean = false, isInput: Boolean = false): Pair<TypeName, String?> {
    val isPrimitive: Boolean = type.isPrimitiveType()
    val isListType: Boolean = isPrimitive && type.isListType()

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

        if (isInput) {
            val inputBindingName: String = KotuleExtensionBinderConstants.getInputBindingName(type.toClassName().canonicalName)
            val inputMapperName: String = KotuleExtensionBinderConstants.getInputMapperName(type.toClassName().canonicalName)
            val inputType: ClassName =
                generateNew(resolveInPackage(inputBindingName)) {
                    file.addType(
//                        KotuleB(this).generate(inputBindingName, type.declaration as KSClassDeclaration, force = true)!!
                        TypeSpec.interfaceBuilder(inputBindingName)
                            .apply {
                                addAnnotation(ClassName("kotlin.js", "JsExport"))
//                                val dec = type.declaration as KSClassDeclaration
                                addFunction(
                                    FunSpec.builder("getText")
                                        .returns(String::class)
                                        .addModifiers(KModifier.ABSTRACT)
                                        .addAnnotation(
                                            AnnotationSpec.builder(ClassName("kotlin.js", "JsName"))
                                                .addMember("name = \"getText\"")
                                                .build()
                                        )
                                        .build()
                                )
                            }
                            .build()
                    )
                }

            generateNew(resolveInPackage(inputMapperName)) {
                file.addType(
                    TypeSpec.classBuilder(inputMapperName)
                        .apply {
                            addSuperinterface(type.toTypeName())
                            addAnnotation(ClassName("kotlin.js", "JsExport"))

                            primaryConstructor(FunSpec.constructorBuilder().addParameter("_instance", inputType).build())
                            addProperty(PropertySpec.builder("_instance", inputType).initializer("_instance").build())

                            addFunction(
                                FunSpec.builder("getText")
                                    .returns(String::class)
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addCode("return _instance.getText()")
                                    .build()
                            )
                        }
                        .build()
                )
            }

            return inputType to inputBindingName
        }
        else {
            val outputBindingName: String = KotuleExtensionBinderConstants.getOutputBindingName(type.toClassName().simpleName)
            val outputType: ClassName =
                generateNew(resolveInPackage(outputBindingName)) {
                    file.addType(KotuleBindingClassGenerator(this).generate(outputBindingName, type.declaration as KSClassDeclaration)!!)
                }

            return outputType to outputBindingName
        }
    }
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

            if (kotuleClass.isAbstract()) {
                addModifiers(KModifier.ABSTRACT)
            }
            else {
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
            }

            addAnnotation(PlatformJsExport::class)

            addAnnotation(
                AnnotationSpec.builder(PlatformJsName::class)
                    .addMember("\"${kotuleClass.simpleName.asString()}\"")
                    .build()
            )

            addProperties(
                kotuleClass.getAllProperties().associate { it.simpleName.asString() to it.type.resolve() },
                kotuleClass.isAbstract()
            )

            addFunctions(
                kotuleClass.getAbstractFunctions(),
                kotuleClass.isAbstract()
            )
        }.build()

    private fun TypeSpec.Builder.addProperties(properties: Map<String, KSType>, isAbstract: Boolean) {
        for ((propertyName, propertyType) in properties) {
            val (outPropertyType: TypeName, outPropertyTypeConstructor: String?) = scope.getInteropTypeAndConstructorFor(propertyType)
            val isPrimitive: Boolean = propertyType.isPrimitiveType()

            addProperty(
                PropertySpec.builder(propertyName, outPropertyType.copy(nullable = propertyType.isMarkedNullable))
                    .apply {
                        if (isAbstract) {
                            addModifiers(KModifier.ABSTRACT)
                            return@apply
                        }

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

            if (propertyType.isListType()) {
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
            listItemType.isPrimitiveType()

        append(".map { ")
        if (listItemIsPrimitive) {
            append("${scope.OutValue}(it)")
        } else {
            append(KotuleExtensionBinderConstants.getOutputBindingName(listItemType.toClassName().simpleName))
            append('(')
            appendParameters((listItemType.declaration as KSClassDeclaration).primaryConstructor!!.parameters) { "it.$it" }
            append(')')
        }
        append(" }")
    }

    private fun TypeSpec.Builder.addFunctions(
        functions: Sequence<KSFunctionDeclaration>,
        isAbstract: Boolean
    ) {
        for (function in functions) {
            if (function.isConstructor()) {
                continue
            }

            if (function.modifiers.contains(Modifier.SUSPEND)) {
                addFunction(generateSuspendFunction(function, isAbstract))
            }
            else {
                addFunction(generateNonSuspendFunction(function, isAbstract))
            }
        }
    }

    private fun FunSpec.Builder.addInteropParameters(parameters: List<KSValueParameter>) {
        for (parameter in parameters) {
            addParameter(
                parameter.name!!.asString(),
                scope.getInteropTypeAndConstructorFor(
                    parameter.type.resolve(),
                    canBePrimitive = true,
                    isInput = true
                ).first
            )
        }
    }

    private fun generateNonSuspendFunction(function: KSFunctionDeclaration, isAbstract: Boolean): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
                if (isAbstract) {
                    addModifiers(KModifier.ABSTRACT)
                }

//                addModifiers(
//                    function.modifiers
//                        .filter { it != Modifier.SUSPEND && it != Modifier.OVERRIDE }
//                        .mapNotNull { it.toKModifier() }
//                )

                function.returnType?.also { returnType ->
                    returns(scope.getInteropTypeAndConstructorFor(returnType.resolve(), true).first)
                }

                addInteropParameters(function.parameters)

                if (!function.isAbstract && !isAbstract) {
                    addCode(
                        buildString {
                            append("return $instanceName.${function.simpleName.asString()}(")
                            addFunctionCallArgs(function.parameters)
                            append(')')
                        }
                    )
                }
            }
            .build()

    private fun generateSuspendFunction(function: KSFunctionDeclaration, isAbstract: Boolean): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
                if (isAbstract) {
                    addModifiers(KModifier.ABSTRACT)
                }

                addModifiers(
                    function.modifiers
                        .filter { it != Modifier.SUSPEND && it != Modifier.OVERRIDE }
                        .mapNotNull { it.toKModifier() }
                )

                returns(OutKotulePromise::class)

                addInteropParameters(function.parameters)

                if (isAbstract) {
                    return@apply
                }

                addCode(
                    buildString {
                        val kotulePromise: (suspend () -> Any) -> Any = ::kotulePromise
                        val kotulePromiseName: String = (kotulePromise as KCallable<*>).name

                        scope.import("dev.toastbits.kotules.extension", kotulePromiseName)
                        append("return $kotulePromiseName { ")

                        val returnType: KSType = function.returnType!!.resolve()
                        val returnTypeDeclaration: KSClassDeclaration = returnType.declaration as KSClassDeclaration
                        val isPrimitive: Boolean = returnType.isPrimitiveType()

                        if (isPrimitive) {
                            val outValue: (Any) -> Any = ::OutValue
                            val outValueName: String = (outValue as KCallable<*>).name

                            scope.import("dev.toastbits.kotules.extension.type", outValueName)
                            append("$outValueName(")

                            append("$instanceName.${function.simpleName.asString()}(")
                            addFunctionCallArgs(function.parameters)
                            append(')')

                            if (returnType.isListType()) {
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

    private fun StringBuilder.addFunctionCallArgs(parameters: List<KSValueParameter>) {
        for ((index, parameter) in parameters.withIndex()) {
            append(parameter.name!!.asString())

            val type: KSType = parameter.type.resolve().resolveTypeAlias()
            if (type.isPrimitiveType()) {
                val mapperName: String = KotuleExtensionBinderConstants.getInputMapperName(type.declaration.qualifiedName!!.asString())
                append(".let { $mapperName(it) }")
            }

            if (index + 1 != parameters.size) {
                append(", ")
            }
        }
    }
}
