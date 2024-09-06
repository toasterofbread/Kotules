package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.await
import dev.toastbits.kotules.extension.type.ValueType

internal class KotuleMapperClassGenerator(
    private val addImport: (String, String) -> Unit
): KotuleTypeGenerator {
    private val instanceName: String = KotuleRuntimeBinderConstants.MAPPER_INSTANCE_NAME

    override fun generate(
        kotuleInterface: KSClassDeclaration,
        target: KmpTarget,
        packageName: String
    ): TypeSpec? =
        if (target == KmpTarget.COMMON) null
        else TypeSpec.classBuilder(KotuleRuntimeBinderConstants.getMapperName(kotuleInterface)).apply {
            val inputClassName: ClassName = ClassName(packageName, KotuleRuntimeBinderConstants.getInputBindingName(kotuleInterface))

            addModifiers(KModifier.INTERNAL)

            primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(instanceName, inputClassName)
                    .build()
            )

            addProperty(
                PropertySpec.builder(instanceName, inputClassName)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(instanceName)
                    .build()
            )

            addSuperinterface(kotuleInterface.toClassName())

            addProperties(kotuleInterface.getDeclaredProperties())
            addFunctions(kotuleInterface.getDeclaredFunctions())
        }.build()

    private fun TypeSpec.Builder.addProperties(properties: Sequence<KSPropertyDeclaration>) {
        for (property in properties) {
            addProperty(
                PropertySpec.builder(property.simpleName.asString(), property.type.toTypeName())
                    .apply {
                        property.getter?.also { getter ->
                            getter(
                                FunSpec.getterBuilder()
                                    .addCode("return $instanceName.${property.simpleName.asString()}")
                                    .build()
                            )
                        }
                    }
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(property.modifiers.mapNotNull { it.toKModifier() })
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
                addModifiers(KModifier.OVERRIDE)
                addModifiers(
                    function.modifiers
                        .filter { it != Modifier.SUSPEND }
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
                        appendParameters(function.parameters)
                        append(')')
                    }
                )
            }
            .build()

    private fun generateSuspendFunction(function: KSFunctionDeclaration): FunSpec =
        FunSpec.builder(function.simpleName.asString())
            .apply {
                val returnType: KSTypeReference? = function.returnType

                addModifiers(KModifier.OVERRIDE)
                addModifiers(function.modifiers.mapNotNull { it.toKModifier() })

                if (returnType != null) {
                    returns(returnType.toTypeName())
                }

                for (parameter in function.parameters) {
                    addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
                }

                addCode(
                    buildString {
                        val awaitName: String = KotulePromise<ValueType>::await.name
                        val awaitPackage: String = KotulePromise::class.java.packageName

                        addImport(awaitPackage, awaitName)

                        if (returnType != null) {
                            append("return ")
                        }
                        append("$instanceName.${function.simpleName.asString()}(")
                        appendParameters(function.parameters)
                        append(").$awaitName().value")

//                        addImport("dev.toastbits.kotules.extension.$kotulePromiseName")
//                        append("return $kotulePromiseName { ")
//
//                        val wrapperClass: KClass<*> = function.returnType!!.resolve().getOutputWrapperClass()
//                        addImport(wrapperClass.qualifiedName!!)
//
//                        append("${wrapperClass.simpleName!!}(")
//                        append("$instanceName.${function.simpleName.asString()}(")
//                        for ((index, parameter) in function.parameters.withIndex()) {
//                            append(parameter.name!!.asString())
//                            if (index + 1 != function.parameters.size) {
//                                append(", ")
//                            }
//                        }
//                        append("))\n}")
                    }
                )
            }
            .build()
}

private fun StringBuilder.appendParameters(parameters: List<KSValueParameter>) {
    for ((index, parameter) in parameters.withIndex()) {
        append(parameter.name!!.asString())
        if (index + 1 != parameters.size) {
            append(", ")
        }
    }
}
