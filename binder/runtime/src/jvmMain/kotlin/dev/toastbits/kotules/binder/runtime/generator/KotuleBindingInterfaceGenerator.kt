package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.core.util.getAbstractFunctions
import dev.toastbits.kotules.binder.core.util.getAbstractProperties
import dev.toastbits.kotules.binder.core.util.isListType
import dev.toastbits.kotules.binder.core.util.isPrimitiveType
import dev.toastbits.kotules.binder.core.util.resolveTypeAlias
import dev.toastbits.kotules.binder.core.util.shouldBeSerialised
import dev.toastbits.kotules.binder.runtime.mapper.getBuiltInInputWrapperClass
import dev.toastbits.kotules.binder.runtime.processor.interfaceGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.core.type.ValueType
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.runtime.KotuleInputBinding

data class TypeArgumentInfo(
    val args: List<KSTypeArgument> = emptyList(),
    val parameters: List<KSTypeParameter> = emptyList()
) {
    fun findOrNull(param: KSTypeParameter): KSTypeArgument? =
        args.getOrNull(parameters.indexOf(param))

    fun find(param: KSTypeParameter): KSTypeArgument =
        findOrNull(param) ?: throw NullPointerException(param.toString())
}

private fun FileGenerator.Scope.getOutputInteropTypeFor(type: KSType, arguments: TypeArgumentInfo, canBePrimitive: Boolean = false): TypeName {
    val resolvedType: KSType = type.resolveTypeAlias()

    if (canBePrimitive && resolvedType.isPrimitiveType() && !resolvedType.isListType()) {
        return type.toTypeName()
    }

    resolvedType.getBuiltInInputWrapperClass(this)?.also { return it }

    val qualifiedName: String = resolvedType.declaration.qualifiedName!!.asString()
    if (qualifiedName.startsWith("kotlin.Function")) {
        return getFunctionInteropType(resolvedType, arguments)
    }

    val declaration: KSDeclaration = resolvedType.declaration
    if (declaration.shouldBeSerialised()) {
        return String::class.asTypeName()
    }

    val typeDeclaration: KSClassDeclaration =
        when (declaration) {
            is KSClassDeclaration -> declaration
            is KSTypeParameter -> arguments.find(declaration).type!!.resolve().declaration as KSClassDeclaration
            else -> throw NotImplementedError("$type $resolvedType $arguments (${declaration::class})")
        }

    val typeClass: ClassName = resolveInPackage(KotuleCoreBinderConstants.getInputBindingName(typeDeclaration.qualifiedName!!.asString()))
    log("Generating input interop type for $declaration (from $type) ($typeDeclaration)")

    return generateNew(typeClass, target = target) {
        interfaceGenerator.generate(
            typeClass.simpleName,
            typeDeclaration,
            true,
            TypeArgumentInfo(resolvedType.arguments, declaration.typeParameters)
        )?.also {
            file.addType(it)
        }
    }
}

private fun FileGenerator.Scope.getFunctionInteropType(type: KSType, arguments: TypeArgumentInfo): TypeName =
    type.toClassName().parameterizedBy(
        type.arguments.map {
            getOutputInteropTypeFor(it.type!!.resolve(), arguments, canBePrimitive = true)
        }
    )

internal class KotuleBindingInterfaceGenerator(
    private val scope: FileGenerator.Scope
) {
    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration,
        mutableFunctions: Boolean,
        arguments: TypeArgumentInfo = TypeArgumentInfo()
    ): TypeSpec? =
        if (scope.target != KmpTarget.COMMON && !scope.target.isWeb()) null
        else TypeSpec.interfaceBuilder(name).apply {
            scope.generateNew(ClassName(scope.file.packageName, name + "_Constructor")) {
                file.addFunction(
                    FunSpec.builder(name)
                        .apply {
                            returns(ClassName(scope.file.packageName, name))
                            addModifiers(KModifier.INTERNAL)
                            if (scope.target == KmpTarget.COMMON) {
                                addModifiers(KModifier.EXPECT)
                            }
                            else {
                                addModifiers(KModifier.ACTUAL)
                                addCode("return js(\"({})\")")
                            }
                        }
                        .build()
                )
            }

            val expectationModifier: KModifier =
                if (scope.target == KmpTarget.COMMON) KModifier.EXPECT
                else KModifier.ACTUAL

            addModifiers(expectationModifier, KModifier.INTERNAL)

            if (scope.target.isWeb()) {
                addModifiers(KModifier.EXTERNAL)
            }

            addSuperinterface(KotuleInputBinding::class)

            if (scope.target == KmpTarget.COMMON) {
                addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember("\"NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE\"")
                        .build()
                )
            }

            if (kotuleInterface.classKind == ClassKind.ENUM_CLASS) {
                addProperty(
                    PropertySpec.builder(KotuleRuntimeBinderConstants.ENUM_BINDER_ORDINAL_PROPERTY_NAME, Int::class)
                        .addModifiers(expectationModifier)
                        .build()
                )
            }
            else if (kotuleInterface.isAbstract()) {
                addProperties(
                    kotuleInterface.getAbstractProperties().associate { it.simpleName.asString() to it.type.resolve() },
                    expectationModifier,
                    arguments
                )
                addFunctions(
                    kotuleInterface.getAbstractFunctions(),
                    expectationModifier,
                    mutable = mutableFunctions,
                    arguments = arguments
                )
            }
            else {
                addProperties(
                    kotuleInterface.primaryConstructor?.parameters.orEmpty().associate { it.name!!.asString() to it.type.resolve() },
                    expectationModifier,
                    arguments
                )
            }

            if (kotuleInterface.classKind == ClassKind.INTERFACE) {
                val mapperName: ClassName = scope.resolveInPackage(KotuleCoreBinderConstants.getInputMapperName(kotuleInterface))
                scope.generateNew(mapperName) {
                    KotuleMapperClassGenerator(this).generate(mapperName.simpleName, kotuleInterface)?.also {
                        this@generateNew.file.addType(it)
                    }
                }
            }
        }.build()

    private fun TypeSpec.Builder.addProperties(
        properties: Map<String, KSType>,
        expectationModifier: KModifier,
        arguments: TypeArgumentInfo
    ) {
        for ((propertyName, propertyType) in properties) {
            val outPropertyType: TypeName = scope.getOutputInteropTypeFor(propertyType, arguments, canBePrimitive = true)

            addProperty(
                PropertySpec.builder(
                    propertyName,
                    outPropertyType.copy(nullable = propertyType.isMarkedNullable)
                )
                    .addModifiers(expectationModifier)
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addFunctions(
        functions: Sequence<KSFunctionDeclaration>,
        expectationModifier: KModifier,
        mutable: Boolean,
        arguments: TypeArgumentInfo
    ) {
        for (function in functions) {
            if (function.isConstructor()) {
                continue
            }

            val returnType: KSType? = function.returnType?.resolve()
            val actualReturnType: TypeName =
                if (function.modifiers.contains(Modifier.SUSPEND)) {
                    val promiseTypeParam: TypeName =
                        returnType?.let { scope.getOutputInteropTypeFor(it, arguments) }
                            ?: ValueType::class.asClassName()

                    KotulePromise::class.asClassName().plusParameter(promiseTypeParam)
                }
                else if (returnType != null) scope.getOutputInteropTypeFor(returnType, arguments, canBePrimitive = true)
                else Unit::class.asClassName()

            val functionName: String = function.simpleName.asString()
            val functionParameters: List<ParameterSpec> =
                function.parameters.map {
                    ParameterSpec.builder(
                        it.name!!.asString(),
                        scope.getOutputInteropTypeFor(
                            it.type.resolve(),
                            arguments,
                            canBePrimitive = true
                        )
                    ).build()
                }

            if (mutable) {
                addProperty(
                    PropertySpec.builder(
                        functionName,
                        LambdaTypeName.get(
                            returnType = actualReturnType,
                            parameters = functionParameters
                        )
                    )
                        .mutable(true)
                        .addModifiers(expectationModifier, KModifier.ABSTRACT)
                        .build()
                )
            }
            else {
                addFunction(
                    FunSpec.builder(functionName)
                        .returns(actualReturnType)
                        .addParameters(functionParameters)
                        .addModifiers(expectationModifier, KModifier.ABSTRACT)
                        .build()
                )
            }
        }
    }
}
