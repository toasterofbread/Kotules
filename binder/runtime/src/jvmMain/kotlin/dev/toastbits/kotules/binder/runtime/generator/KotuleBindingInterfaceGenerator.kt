package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.runtime.mapper.getBuiltInInputWrapperClass
import dev.toastbits.kotules.binder.runtime.processor.interfaceGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.binder.runtime.util.PRIMITIVE_TYPES
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.type.ValueType
import dev.toastbits.kotules.runtime.KotuleInputBinding

internal class KotuleBindingInterfaceGenerator(
    private val scope: FileGenerator.Scope
) {
    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration
    ): TypeSpec =
        TypeSpec.interfaceBuilder(name).apply {
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

            addProperties(kotuleInterface.getDeclaredProperties(), expectationModifier)
            addFunctions(kotuleInterface.getDeclaredFunctions(), expectationModifier)

            if (kotuleInterface.classKind == ClassKind.CLASS && scope.target == KmpTarget.COMMON) {
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
        properties: Sequence<KSPropertyDeclaration>,
        expectationModifier: KModifier
    ) {
        for (property in properties) {
            val outPropertyType: ClassName
            val propertyType: KSType = property.type.resolve()

            if (PRIMITIVE_TYPES.contains(propertyType.toClassName().canonicalName)) {
                outPropertyType = propertyType.toClassName()
            }
            else {
                val typeDeclaration: KSDeclaration = propertyType.declaration
                check(typeDeclaration is KSClassDeclaration) { typeDeclaration }

                outPropertyType = scope.resolveInPackage(KotuleRuntimeBinderConstants.getInputBindingName(propertyType.toClassName().simpleName))
                scope.generateNew(outPropertyType) {
                    file.addType(interfaceGenerator.generate(outPropertyType.simpleName, typeDeclaration))
                }
            }

            addProperty(
                PropertySpec.builder(property.simpleName.asString(), outPropertyType.copy(nullable = propertyType.isMarkedNullable))
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

                        val returnType: KSTypeReference? = function.returnType
                        if (function.modifiers.contains(Modifier.SUSPEND)) {
                            val promiseTypeParam: ClassName =
                                returnType?.resolve()?.let { type ->
                                    type.getBuiltInInputWrapperClass()?.also { return@let it.asClassName() }

                                    val declaration: KSDeclaration = type.declaration
                                    check(declaration is KSClassDeclaration) { declaration }

                                    val typeClass: ClassName = scope.resolveInPackage(KotuleRuntimeBinderConstants.getInputBindingName(type.toClassName().simpleName))
                                    scope.generateNew(typeClass) {
                                        file.addType(interfaceGenerator.generate(typeClass.simpleName, declaration))
                                    }
                                } ?: ValueType::class.asClassName()

                            returns(KotulePromise::class.asClassName().plusParameter(promiseTypeParam))
                        }
                        else if (returnType != null) {
                            returns(returnType.toTypeName())
                        }

                        for (parameter in function.parameters) {
                            addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                        }
                    }
                    .build()
            )
        }
    }
}
