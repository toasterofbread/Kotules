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
    private val addImport: (String, String) -> Unit
): KotuleTypeGenerator {
    override fun generate(
        kotuleInterface: KSClassDeclaration,
        target: KmpTarget,
        packageName: String
    ): TypeSpec =
        TypeSpec.objectBuilder(KotuleRuntimeBinderConstants.getLoaderName(kotuleInterface))
            .apply {
                if (target == KmpTarget.COMMON) {
                    addModifiers(KModifier.EXPECT)
                }
                else {
                    addModifiers(KModifier.ACTUAL)
                }

                addSuperinterface(KotuleLoader::class.asTypeName().plusParameter(kotuleInterface.toClassName()))
                generateBody(target, kotuleInterface)
            }
            .build()

    private fun TypeSpec.Builder.generateBody(
        target: KmpTarget,
        kotuleInterface: KSClassDeclaration
    ) {
        when (target) {
            KmpTarget.COMMON -> {}
            KmpTarget.JVM -> {
                addFunction(
                    FunSpec.builder("loadFromJar")
                        .returns(kotuleInterface.toClassName())
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameters(
                            listOf(
                                ParameterSpec.builder("jarPath", String::class).build(),
                                ParameterSpec.builder("implementationClass", String::class).build()
                            )
                        )
                        .addCode(buildString {
                            addImport("dev.toastbits.kotules.runtime", "loadKotuleFromJar")
                            append("return loadKotuleFromJar(jarPath, implementationClass) as ${kotuleInterface.simpleName.asString()}")
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
                            addImport("dev.toastbits.kotules.runtime", "loadKotuleInputBindingFromKotlinJsCode")

                            append("return $mapperClass(loadKotuleInputBindingFromKotlinJsCode(jsCode, implementationClass))")
                        })
                        .build()
                )
            }
        }
    }
}
