@file:OptIn(KspExperimental::class)

package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.runtime.mapper.getBuiltInInputWrapperClass
import dev.toastbits.kotules.binder.runtime.processor.interfaceGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.binder.runtime.util.resolveTypeAlias
import dev.toastbits.kotules.binder.runtime.util.shouldBeSerialsied
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.core.type.ValueType
import dev.toastbits.kotules.extension.util.LIST_TYPES
import dev.toastbits.kotules.extension.util.PRIMITIVE_TYPES
import dev.toastbits.kotules.runtime.KotuleInputBinding

private fun FileGenerator.Scope.getInteropTypeFor(type: KSType, canBePrimitive: Boolean = false): TypeName {
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
    check(declaration is KSClassDeclaration) { declaration }

    val typeClass: ClassName = resolveInPackage(KotuleRuntimeBinderConstants.getInputBindingName(type.toClassName().simpleName))
    log("Generating input interop type for $type (${type.toClassName()})")

    return generateNew(typeClass) {
        interfaceGenerator.generate(typeClass.simpleName, declaration)?.also {
            file.addType(it)
        }
    }
}

private fun FileGenerator.Scope.getFunctionInteropType(type: KSType): TypeName =
    type.toClassName().parameterizedBy(
        type.arguments.map {
            getInteropTypeFor(it.type!!.resolve(), canBePrimitive = true)
        }
    )

internal class KotuleBindingInterfaceGenerator(
    private val scope: FileGenerator.Scope
) {
    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration
    ): TypeSpec? =
        if (scope.target != KmpTarget.COMMON && !scope.target.isWeb()) null
        else TypeSpec.interfaceBuilder(name).apply {
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

            if (kotuleInterface.classKind == ClassKind.CLASS) {
                addProperties(
                    kotuleInterface.primaryConstructor?.parameters.orEmpty().associate { it.name!!.asString() to it.type.resolve() },
                    expectationModifier
                )
            }
            else {
                addProperties(
                    kotuleInterface.getDeclaredProperties().associate { it.simpleName.asString() to it.type.resolve() },
                    expectationModifier
                )
                addFunctions(kotuleInterface.getDeclaredFunctions(), expectationModifier)
            }

            if (scope.target == KmpTarget.COMMON) {
                scope.file.addProperty(
                    PropertySpec.builder("value", kotuleInterface.toClassName())
                        .addModifiers(KModifier.INTERNAL)
                        .receiver(scope.resolveInPackage(name))
                        .getter(
                            FunSpec.getterBuilder()
                                .addCode(buildString {
                                    append("return ")
                                    append(kotuleInterface.toClassName().simpleName)
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
                                })
                                .build()
                        )
                        .build()
                )
            }

            if (kotuleInterface.classKind == ClassKind.INTERFACE) {
                val mapperName: ClassName = scope.resolveInPackage(KotuleRuntimeBinderConstants.getMapperName(kotuleInterface))
                scope.generateNew(mapperName) {
                    KotuleMapperClassGenerator(this).generate(mapperName.simpleName, kotuleInterface)?.also {
                        this@generateNew.file.addType(it)
                    }
                }
            }
        }.build()

    private fun TypeSpec.Builder.addProperties(
        properties: Map<String, KSType>,
        expectationModifier: KModifier
    ) {
        for ((propertyName, propertyType) in properties) {
            val outPropertyType: TypeName = scope.getInteropTypeFor(propertyType, canBePrimitive = true)

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
        expectationModifier: KModifier
    ) {
        for (function in functions) {
            if (function.isConstructor()) {
                continue
            }

            addFunction(
                FunSpec.builder(function.simpleName.asString())
                    .apply {
                        addModifiers(expectationModifier, KModifier.ABSTRACT)
                        addModifiers(
                            function
                                .modifiers
                                .filter { it != Modifier.SUSPEND && it != Modifier.OVERRIDE }
                                .mapNotNull { it.toKModifier() }
                        )

                        val returnType: KSType? = function.returnType?.resolve()
                        if (function.modifiers.contains(Modifier.SUSPEND)) {
                            val promiseTypeParam: TypeName =
                                returnType?.let { scope.getInteropTypeFor(it) }
                                ?: ValueType::class.asClassName()

                            returns(KotulePromise::class.asClassName().plusParameter(promiseTypeParam))
                        }
                        else if (returnType != null) {
                            returns(scope.getInteropTypeFor(returnType, canBePrimitive = true))
                        }

                        for (parameter in function.parameters) {
                            addParameter(
                                parameter.name!!.asString(),
                                scope.getInteropTypeFor(
                                    parameter.type.resolve(),
                                    canBePrimitive = true
                                )
                            )
                        }
                    }
                    .build()
            )
        }
    }
}
