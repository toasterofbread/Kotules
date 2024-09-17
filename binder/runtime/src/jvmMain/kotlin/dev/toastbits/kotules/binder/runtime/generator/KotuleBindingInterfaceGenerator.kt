package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSValueParameter
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
import dev.toastbits.kotules.binder.runtime.mapper.getBuiltInInputWrapperClass
import dev.toastbits.kotules.binder.runtime.processor.interfaceGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.core.util.resolveTypeAlias
import dev.toastbits.kotules.binder.core.util.shouldBeSerialised
import dev.toastbits.kotules.core.type.ValueType
import dev.toastbits.kotules.core.util.LIST_TYPES
import dev.toastbits.kotules.core.util.PRIMITIVE_TYPES
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
    val qualifiedName: String = resolvedType.declaration.qualifiedName!!.asString()

    if (canBePrimitive && PRIMITIVE_TYPES.contains(qualifiedName) && !LIST_TYPES.contains(qualifiedName)) {
        return type.toTypeName()
    }

    type.getBuiltInInputWrapperClass(this)?.also { return it }

    if (qualifiedName.startsWith("kotlin.Function")) {
        return getFunctionInteropType(type, arguments)
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

            if (kotuleInterface.isAbstract()) {
                addProperties(
                    kotuleInterface.getDeclaredProperties().associate { it.simpleName.asString() to it.type.resolve() },
                    expectationModifier,
                    arguments
                )
                addFunctions(
                    kotuleInterface.getDeclaredFunctions(),
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

            if (scope.target != KmpTarget.COMMON) {
                return@apply
            }

            scope.file.addProperty(
                PropertySpec.builder(
                    "value",
                    try {
                        kotuleInterface.toClassName()
                            .run {
                                if (kotuleInterface.typeParameters.isNotEmpty())
                                    parameterizedBy(kotuleInterface.typeParameters.map { arguments.find(it).toTypeName() })
                                else this
                            }
                    }
                    catch (e: Throwable) {
                        throw RuntimeException("$kotuleInterface ${kotuleInterface.typeParameters} $arguments", e)
                    }
                )
                    .addModifiers(KModifier.INTERNAL)
                    .receiver(scope.resolveInPackage(name))
                    .getter(
                        FunSpec.getterBuilder()
                            .addCode(buildString {
                                append("return ")

                                if (kotuleInterface.classKind == ClassKind.INTERFACE) {
                                    append(KotuleCoreBinderConstants.getInputMapperName(kotuleInterface))
                                    append("(this)")
                                }
                                else {
                                    append(kotuleInterface.toClassName().run { canonicalName.removePrefix(packageName + ".") })
                                    append('(')
                                    val params: List<KSValueParameter> = kotuleInterface.primaryConstructor?.parameters.orEmpty()
                                    for ((index, param) in params.withIndex()) {
                                        val type: KSType = param.type.resolve()
                                        val actualType: KSType =
                                            (type.declaration as? KSTypeParameter)?.let {
                                                arguments.findOrNull(it)?.type?.resolve()
                                            } ?: type

                                        val qualifiedName: String = actualType.toClassName().canonicalName

                                        append(param.name!!.asString())
                                        if (!PRIMITIVE_TYPES.contains(qualifiedName)) {
                                            if (type.isMarkedNullable) {
                                                append('?')
                                            }
                                            append(".value")
                                        }
                                        else if (LIST_TYPES.contains(qualifiedName)) {
                                            scope.import("dev.toastbits.kotules.core.type.input", "getListValue")
                                            if (type.isMarkedNullable) {
                                                append('?')
                                            }
                                            append(".getListValue().map { it.value }")
                                        }

                                        if (index + 1 != params.size) {
                                            append(", ")
                                        }
                                    }
                                    append(')')
                                }
                            })
                            .build()
                    )
                    .build()
            )
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
