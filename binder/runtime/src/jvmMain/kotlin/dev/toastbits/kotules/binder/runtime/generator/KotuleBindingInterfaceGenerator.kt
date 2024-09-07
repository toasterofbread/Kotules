package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
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
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.PlatformJsName
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

            val mapperName: ClassName = scope.resolveInPackage(KotuleRuntimeBinderConstants.getMapperName(kotuleInterface))
            scope.generateNew(mapperName) {
                KotuleMapperClassGenerator(this).generate(mapperName.simpleName, kotuleInterface)?.also {
                    this@generateNew.file.addType(it)
                }
            }
        }.build()

    private fun TypeSpec.Builder.addProperties(
        properties: Sequence<KSPropertyDeclaration>,
        expectationModifier: KModifier
    ) {
        for (property in properties) {
            addProperty(
                PropertySpec.builder(property.simpleName.asString(), property.type.toTypeName())
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
