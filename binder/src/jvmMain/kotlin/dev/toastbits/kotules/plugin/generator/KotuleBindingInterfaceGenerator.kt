package dev.toastbits.kotules.plugin.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.type.ValueType
import dev.toastbits.kotules.plugin.mapper.getInputWrapperClass
import dev.toastbits.kotules.plugin.util.KmpTarget

internal class KotuleBindingInterfaceGenerator: KotuleBindingGenerator {
    override fun generate(
        className: String,
        kotuleClass: KSClassDeclaration,
        target: KmpTarget
    ): TypeSpec =
        TypeSpec.interfaceBuilder(className).apply {
            val expectationModifier: KModifier =
                if (target == KmpTarget.COMMON) KModifier.EXPECT
                else KModifier.ACTUAL

            addModifiers(expectationModifier)
            if (target.isWeb()) {
                addModifiers(KModifier.EXTERNAL)
            }

            if (target != KmpTarget.COMMON) {
                addSuperinterface(Kotule::class)
            }

            addProperties(kotuleClass.getDeclaredProperties(), expectationModifier)
            addFunctions(kotuleClass.getDeclaredFunctions(), expectationModifier)
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
                                .filter { it != Modifier.SUSPEND }
                                .mapNotNull { it.toKModifier() }
                        )

                        val returnType: KSTypeReference? = function.returnType
                        if (function.modifiers.contains(Modifier.SUSPEND)) {
                            val promiseTypeParam: ClassName =
                                if (returnType != null) returnType.resolve().getInputWrapperClass().asClassName()
                                else ValueType::class.asClassName()

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
