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
import dev.toastbits.kotules.binder.runtime.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.runtime.util.resolveTypeAlias
import dev.toastbits.kotules.binder.runtime.util.shouldBeSerialsied
import dev.toastbits.kotules.core.type.ValueType
import dev.toastbits.kotules.core.util.LIST_TYPES
import dev.toastbits.kotules.core.util.PRIMITIVE_TYPES
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.runtime.KotuleInputBinding

private fun FileGenerator.Scope.getOutputInteropTypeFor(type: KSType, canBePrimitive: Boolean = false): TypeName {
    val canonicalName: String = type.resolveTypeAlias()
    if (canBePrimitive && PRIMITIVE_TYPES.contains(canonicalName) && !LIST_TYPES.contains(canonicalName)) {
        return type.toTypeName()
    }

    type.getBuiltInInputWrapperClass(this)?.also { return it }

    if (canonicalName.startsWith("kotlin.Function")) {
        return getFunctionInteropType(type)
    }

    if (type.declaration.shouldBeSerialsied()) {
        return String::class.asTypeName()
    }

    val declaration: KSDeclaration = type.declaration
    check(declaration is KSClassDeclaration) { "$declaration (${declaration::class})" }

    val typeClass: ClassName = resolveInPackage(KotuleCoreBinderConstants.getInputBindingName(type.toClassName().simpleName))
    log("Generating input interop type for $type (${type.toClassName()})")

    return generateNew(typeClass) {
        interfaceGenerator.generate(typeClass.simpleName, declaration, true)?.also {
            file.addType(it)
        }
    }
}

private fun FileGenerator.Scope.getFunctionInteropType(type: KSType): TypeName =
    type.toClassName().parameterizedBy(
        type.arguments.map {
            getOutputInteropTypeFor(it.type!!.resolve(), canBePrimitive = true)
        }
    )

internal class KotuleBindingInterfaceGenerator(
    private val scope: FileGenerator.Scope
) {
    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration,
        mutableFunctions: Boolean
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
                    expectationModifier
                )
                addFunctions(
                    kotuleInterface.getDeclaredFunctions(),
                    expectationModifier,
                    mutable = mutableFunctions
                )
            }
            else {
                addProperties(
                    kotuleInterface.primaryConstructor?.parameters.orEmpty().associate { it.name!!.asString() to it.type.resolve() },
                    expectationModifier
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
                PropertySpec.builder("value", kotuleInterface.toClassName())
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
                                        append(param.name!!.asString())
                                        if (!PRIMITIVE_TYPES.contains(type.toClassName().canonicalName)) {
                                            if (type.isMarkedNullable) {
                                                append('?')
                                            }
                                            append(".value")
                                        }
                                        else if (LIST_TYPES.contains(type.toClassName().canonicalName)) {
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
        expectationModifier: KModifier
    ) {
        for ((propertyName, propertyType) in properties) {
            val outPropertyType: TypeName = scope.getOutputInteropTypeFor(propertyType, canBePrimitive = true)

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
        mutable: Boolean
    ) {
        for (function in functions) {
            if (function.isConstructor()) {
                continue
            }

            val returnType: KSType? = function.returnType?.resolve()
            val actualReturnType: TypeName =
                if (function.modifiers.contains(Modifier.SUSPEND)) {
                    val promiseTypeParam: TypeName =
                        returnType?.let { scope.getOutputInteropTypeFor(it) }
                            ?: ValueType::class.asClassName()

                    KotulePromise::class.asClassName().plusParameter(promiseTypeParam)
                }
                else if (returnType != null) scope.getOutputInteropTypeFor(returnType, canBePrimitive = true)
                else Unit::class.asClassName()

            val functionName: String = function.simpleName.asString()
            val functionParameters: List<ParameterSpec> =
                function.parameters.map {
                    ParameterSpec.builder(
                        it.name!!.asString(),
                        scope.getOutputInteropTypeFor(
                            it.type.resolve(),
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
