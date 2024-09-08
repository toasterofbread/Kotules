package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.runtime.KotuleLoader

internal class KotuleLoaderGenerator(
    private val scope: FileGenerator.Scope
) {
    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration
    ): TypeSpec =
        TypeSpec.objectBuilder(name)
            .apply {
                addModifiers(KModifier.INTERNAL)

                if (scope.target == KmpTarget.COMMON) {
                    addModifiers(KModifier.EXPECT)
                }
                else {
                    addModifiers(KModifier.ACTUAL)
                }

                addSuperinterface(KotuleLoader::class.asTypeName().plusParameter(kotuleInterface.toClassName()))
                generateBody(kotuleInterface)
            }
            .build()

    private fun TypeSpec.Builder.generateBody(
        kotuleInterface: KSClassDeclaration
    ) {
        when (scope.target) {
            KmpTarget.COMMON -> {}
            KmpTarget.JVM -> {
                addFunction(
                    FunSpec.builder("loadFromJarFile")
                        .returns(kotuleInterface.toClassName())
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameters(
                            listOf(
                                ParameterSpec.builder("jarPath", String::class).build(),
                                ParameterSpec.builder("implementationClass", String::class).build()
                            )
                        )
                        .addCode(buildString {
                            scope.import("dev.toastbits.kotules.runtime", "loadKotuleFromJarFile")
                            append("return loadKotuleFromJarFile(jarPath, implementationClass) as ${kotuleInterface.simpleName.asString()}")
                        })
                        .build()
                )
                addFunction(
                    FunSpec.builder("loadFromJarBytes")
                        .returns(kotuleInterface.toClassName())
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameters(
                            listOf(
                                ParameterSpec.builder("jarBytes", ByteArray::class).build(),
                                ParameterSpec.builder("implementationClass", String::class).build()
                            )
                        )
                        .addCode(buildString {
                            scope.import("dev.toastbits.kotules.runtime", "loadKotuleFromJarBytes")
                            append("return loadKotuleFromJarBytes(jarBytes, implementationClass) as ${kotuleInterface.simpleName.asString()}")
                        })
                        .build()
                )
            }
            KmpTarget.WASMJS,
            KmpTarget.JS -> {
                addFunction(
                    FunSpec.builder("loadFromKotlinJsCode")
                        .returns(kotuleInterface.toClassName())
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameters(
                            listOf(
                                ParameterSpec.builder("jsCode", String::class).build(),
                                ParameterSpec.builder("implementationClass", String::class).build()
                            )
                        )
                        .addCode(buildString {
                            val mapperClass: String = KotuleRuntimeBinderConstants.getMapperName(kotuleInterface)
                            scope.import("dev.toastbits.kotules.runtime", "loadKotuleInputBindingFromKotlinJsCode")

                            append("return $mapperClass(loadKotuleInputBindingFromKotlinJsCode(jsCode, implementationClass))")
                        })
                        .build()
                )
            }
        }
    }
}
