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
import com.google.devtools.ksp.symbol.KSTypeReference
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
import dev.toastbits.kotules.binder.core.util.getNeededFunctions
import dev.toastbits.kotules.binder.core.util.getNeededProperties
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

fun FileGenerator.Scope.getInputBinding(kotuleInterface: KSClassDeclaration, arguments: TypeArgumentInfo): ClassName {
    lateinit var ret: ClassName
    for (target in KmpTarget.entries) {
        val bindingName: ClassName = resolveInPackage(KotuleCoreBinderConstants.getInputBindingName(kotuleInterface))
        ret = generateNew(bindingName, target = target) {
            interfaceGenerator.generate(bindingName.simpleName, kotuleInterface, mutableFunctions = true, arguments = arguments)?.also {
                this@generateNew.file.addType(it)
            }
        }
    }
    return ret
}

data class TypeArgumentInfo(
    val args: List<KSTypeReference?> = emptyList(),
    val parameters: List<KSTypeParameter> = emptyList()
) {
    fun findOrNull(param: KSTypeParameter): KSTypeReference? =
        args.getOrNull(parameters.indexOf(param))

    fun find(param: KSTypeParameter): KSTypeReference =
        findOrNull(param) ?: throw NullPointerException(param.toString())

    fun withBounds(): TypeArgumentInfo =
        copy(args = parameters.map { it.bounds.single() })

    companion object {
        fun from(
            args: List<KSTypeArgument>,
            parameters: List<KSTypeParameter>
        ): TypeArgumentInfo =
            TypeArgumentInfo(args.map { it.type }, parameters)
    }
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
            is KSTypeParameter -> arguments.find(declaration).resolve().declaration as KSClassDeclaration
            else -> throw NotImplementedError("$type $resolvedType $arguments (${declaration::class})")
        }

    val typeClass: ClassName = resolveInPackage(KotuleCoreBinderConstants.getInputBindingName(typeDeclaration.qualifiedName!!.asString()))
    log("Generating input interop type for $declaration (from $type) ($typeDeclaration)")

    return generateNew(typeClass, target = target) {
        interfaceGenerator.generate(
            typeClass.simpleName,
            typeDeclaration,
            true,
            TypeArgumentInfo.from(resolvedType.arguments, declaration.typeParameters)
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

fun KSTypeReference.resolve(arguments: TypeArgumentInfo): KSType {
    var resolved: KSType = resolve()
    while (resolved.declaration is KSTypeParameter) {
        resolved = arguments.find(resolved.declaration as KSTypeParameter).resolve()
    }
    return resolved
}

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
                val createFunctionName: String = "createObject"
                val instanceParamName: String = "instance"

                if (scope.target != KmpTarget.COMMON) {
                    file.addFunction(
                        FunSpec.builder(createFunctionName)
                            .apply {
                                returns(ClassName(scope.file.packageName, name))
                                addModifiers(KModifier.PRIVATE)

                                val argPrefix: String = "arg"
                                var arg: Int = 0

                                for (function in kotuleInterface.getNeededFunctions()) {
                                    val returnType: KSType = function.returnType!!.resolve()
                                    val type: TypeName =
                                        LambdaTypeName.get(
                                            returnType =
                                                getOutputInteropTypeFor(returnType, arguments.withBounds(), true).copy(nullable = returnType.isMarkedNullable),
                                            parameters =
                                                function.parameters.map {
                                                    ParameterSpec.unnamed(getOutputInteropTypeFor(it.type.resolve(), arguments.withBounds(), true))
                                                }
                                        )

                                    addParameter("$argPrefix${arg++}", type)
                                }

                                for (property in kotuleInterface.getNeededProperties()) {
                                    val type: KSType = property.type.resolve()
                                    val interopType: TypeName = getOutputInteropTypeFor(type, arguments.withBounds(), true).copy(nullable = type.isMarkedNullable)
                                    addParameter("$argPrefix${arg++}", interopType)
                                }

                                addCode(buildString {
                                    appendLine("return js(\"\"\"({")

                                    arg = 0
                                    for (function in kotuleInterface.getNeededFunctions()) {
                                        val functionName: String = function.simpleName.asString()
                                        appendLine("$functionName: $argPrefix${arg++}")
                                    }

                                    for (property in kotuleInterface.getNeededProperties()) {
                                        val propertyName: String = property.simpleName.asString()
                                        appendLine("$propertyName: $argPrefix${arg++}")
                                    }

                                    appendLine("})\"\"\")")
                                })
                            }
                            .build()
                    )
                }

                file.addFunction(
                    FunSpec.builder(name)
                        .apply {
                            returns(ClassName(scope.file.packageName, name))
                            addModifiers(KModifier.INTERNAL)
                            addParameter(
                                instanceParamName,
                                kotuleInterface.toClassName()
                                    .run {
                                        if (kotuleInterface.typeParameters.isNotEmpty())
                                            parameterizedBy(kotuleInterface.typeParameters.map {
                                                it.bounds.single().toTypeName()
                                            })
                                        else this
                                    }
                            )

                            if (scope.target == KmpTarget.COMMON) {
                                addModifiers(KModifier.EXPECT)
                            }
                            else {
                                addModifiers(KModifier.ACTUAL)
                                addCode(buildString {
                                    appendLine("return $createFunctionName(")

                                    for (function in kotuleInterface.getNeededFunctions()) {
                                        val functionName: String = function.simpleName.asString()
                                        val valueSuffix: String =
                                            getFunctionParameterTransformSuffix(
                                                function.parameters.map {
                                                    it.type.resolve(arguments.withBounds())
                                                        .resolveTypeAlias()
                                                },
                                                function.returnType?.resolve(arguments)?.resolveTypeAlias(),
                                                addArgumentTypes = false
                                            )
                                        appendLine("$instanceParamName::$functionName$valueSuffix,")
                                    }

                                    for (property in kotuleInterface.getNeededProperties()) {
                                        val propertyName: String = property.simpleName.asString()
                                        val valueSuffix: String = getFunctionParameterTransformSuffix(property.type.resolve(arguments.withBounds()), true)
                                        appendLine("$instanceParamName.$propertyName$valueSuffix,")
                                    }

                                    appendLine(")")
                                })
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
                    kotuleInterface.getNeededProperties().associate { it.simpleName.asString() to (it.type.resolve() to (it.isMutable || mutableFunctions)) },
                    expectationModifier,
                    arguments
                )
                addFunctions(
                    kotuleInterface.getNeededFunctions(),
                    expectationModifier,
                    mutable = mutableFunctions,
                    arguments = arguments
                )
            }
            else {
                addProperties(
                    kotuleInterface.primaryConstructor?.parameters.orEmpty().associate { it.name!!.asString() to (it.type.resolve() to mutableFunctions) },
                    expectationModifier,
                    arguments
                )
            }
        }.build()

    private fun TypeSpec.Builder.addProperties(
        properties: Map<String, Pair<KSType, Boolean>>,
        expectationModifier: KModifier,
        arguments: TypeArgumentInfo
    ) {
        for ((propertyName, property) in properties) {
            val (propertyType: KSType, mutable: Boolean) = property
            val outPropertyType: TypeName = scope.getOutputInteropTypeFor(propertyType, arguments, canBePrimitive = true)

            addProperty(
                PropertySpec.builder(
                    propertyName,
                    outPropertyType.copy(nullable = propertyType.isMarkedNullable)
                )
                    .mutable(mutable)
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
                    val type: KSType = it.type.resolve()
                    ParameterSpec.builder(
                        it.name!!.asString(),
                        scope.getOutputInteropTypeFor(
                            type,
                            arguments,
                            canBePrimitive = true
                        ).copy(nullable = type.isMarkedNullable)
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
