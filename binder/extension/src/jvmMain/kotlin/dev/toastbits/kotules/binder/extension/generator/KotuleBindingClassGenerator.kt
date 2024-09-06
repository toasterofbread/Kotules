package dev.toastbits.kotules.binder.extension.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.extension.OutKotulePromise
import dev.toastbits.kotules.extension.PlatformJsExport
import dev.toastbits.kotules.extension.kotulePromise
import dev.toastbits.kotules.binder.extension.mapper.getOutputWrapperClass
import dev.toastbits.kotules.binder.extension.util.KmpTarget
import dev.toastbits.kotules.binder.extension.util.KotuleExtensionBinderConstants
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

internal class KotuleBindingClassGenerator(
    private val addImport: (String) -> Unit
): KotuleTypeGenerator {
    private val instanceName: String = KotuleExtensionBinderConstants.OUTPUT_BINDING_INSTANCE_NAME

    override fun generate(
        className: String,
        kotuleClass: KSClassDeclaration,
        bindInterface: KSType,
        target: KmpTarget
    ): TypeSpec? =
        if (target != KmpTarget.COMMON) null
        else TypeSpec.classBuilder(className).apply {
            val instanceType: TypeName = kotuleClass.asType(emptyList()).toTypeName()
            addProperty(
                PropertySpec.builder(instanceName, instanceType)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("${kotuleClass.simpleName.asString()}()")
                    .build()
            )

            addAnnotation(PlatformJsExport::class)

            if (target.isWeb()) {
                addModifiers(KModifier.EXTERNAL)
            }

            addProperties(kotuleClass.getDeclaredProperties())
            addFunctions(kotuleClass.getDeclaredFunctions())
        }.build()

    private fun TypeSpec.Builder.addProperties(properties: Sequence<KSPropertyDeclaration>) {
        for (property in properties) {
            addProperty(
                PropertySpec.builder(property.simpleName.asString(), property.type.toTypeName())
                    .apply {
                        property.getter?.also { getter ->
                            getter(
                                FunSpec.getterBuilder().addCode(
                                    "return $instanceName.${property.simpleName.asString()}"
                                ).build()
                            )
                        }
                    }
                    .build()
            )
        }
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

    private fun generateNonSuspendFunction(function: KSFunctionDeclaration): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
                addModifiers(
                    function.modifiers
                        .filter { it != Modifier.SUSPEND && it != Modifier.OVERRIDE }
                        .mapNotNull { it.toKModifier() }
                )

                function.returnType?.also { returnType ->
                    returns(returnType.toTypeName())
                }

                for (parameter in function.parameters) {
                    addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                }

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

                for (parameter in function.parameters) {
                    addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                }

                addCode(
                    buildString {
                        val kotulePromise: (suspend () -> Any) -> Any = ::kotulePromise
                        val kotulePromiseName: String = (kotulePromise as KCallable<*>).name

                        addImport("dev.toastbits.kotules.extension.$kotulePromiseName")
                        append("return $kotulePromiseName { ")

                        val wrapperClass: KClass<*> = function.returnType!!.resolve().getOutputWrapperClass()
                        addImport(wrapperClass.qualifiedName!!)

                        append("${wrapperClass.simpleName!!}(")
                        append("$instanceName.${function.simpleName.asString()}(")
                        for ((index, parameter) in function.parameters.withIndex()) {
                            append(parameter.name!!.asString())
                            if (index + 1 != function.parameters.size) {
                                append(", ")
                            }
                        }
                        append("))\n}")
                    }
                )
            }
            .build()
}
