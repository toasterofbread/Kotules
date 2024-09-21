package dev.toastbits.kotules.binder.runtime.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.core.generator.FileGenerator
import dev.toastbits.kotules.binder.runtime.generator.KotuleBindingInterfaceGenerator
import dev.toastbits.kotules.binder.runtime.generator.KotuleLoaderGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.core.Kotule
import dev.toastbits.kotules.runtime.KotuleLoader
import dev.toastbits.kotules.runtime.annotation.KotuleDeclaration
import kotlin.reflect.KClass

internal val FileGenerator.Scope.interfaceGenerator: KotuleBindingInterfaceGenerator
    get() = KotuleBindingInterfaceGenerator(this)

internal val FileGenerator.Scope.loaderGenerator: KotuleLoaderGenerator
    get() = KotuleLoaderGenerator(this)

internal class KotuleRuntimeAnnotationProcessor(
    environment: SymbolProcessorEnvironment
): SymbolProcessor {
    private val fileGenerator: FileGenerator = FileGenerator(environment.codeGenerator, environment.logger)
    private val logger: KSPLogger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val kotuleInterfaces: Sequence<KSClassDeclaration> =
            resolver.getSymbolsWithAnnotation(KotuleDeclaration::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()

        for (kotuleInterface in kotuleInterfaces) {
            validateKotuleClass(kotuleInterface)

            try {
                generateClassBindings(kotuleInterface)
                generateLoaderInstanceExtension(kotuleInterface)
            }
            catch (e: Throwable) {
                logger.error(e.stackTraceToString(), kotuleInterface)
            }
        }

        fileGenerator.writeToDisk()

        return emptyList()
    }

    private fun generateClassBindings(kotuleInterface: KSClassDeclaration) {
        val bindingName: String = KotuleCoreBinderConstants.getInputBindingName(kotuleInterface)
        val loaderName: String = KotuleRuntimeBinderConstants.getLoaderName(kotuleInterface)

        for (target in KmpTarget.entries) {
            fileGenerator.generate(
                ClassName(
                    kotuleInterface.packageName.asString(),
                    bindingName
                ),
                target
            ) {
                interfaceGenerator.generate(bindingName, kotuleInterface, false)?.also {
                    file.addType(it)
                }
            }
            fileGenerator.generate(
                ClassName(
                    kotuleInterface.packageName.asString(),
                    loaderName
                ),
                target
            ) {
                file.addType(loaderGenerator.generate(loaderName, kotuleInterface))
            }
        }
    }

    private fun generateLoaderInstanceExtension(kotuleInterface: KSClassDeclaration) {
        fileGenerator.generate(
            packageName = kotuleInterface.packageName.asString(),
            name = KotuleRuntimeBinderConstants.LOADER_INSTANCE_EXTENSION_FUNCTION_NAME,
            target = KmpTarget.COMMON
        ) {
            val packageName: String = kotuleInterface.packageName.asString()
            val loaderType: ClassName = ClassName(packageName, KotuleRuntimeBinderConstants.getLoaderName(kotuleInterface))

            file.addFunction(
                FunSpec.builder(KotuleRuntimeBinderConstants.LOADER_INSTANCE_EXTENSION_FUNCTION_NAME)
                    .returns(KotuleLoader::class.asClassName().plusParameter(kotuleInterface.toClassName()))
                    .receiver(KClass::class.asClassName().plusParameter(kotuleInterface.toClassName()))
                    .addCode("return ${loaderType.simpleName}")
                    .build()
            )
        }
    }

    private fun validateKotuleClass(kotuleInterface: KSClassDeclaration) {
        if (kotuleInterface.classKind != ClassKind.INTERFACE) {
            fail("Must be an interface", kotuleInterface)
        }

        if (kotuleInterface.getAllSuperTypes().none { it.toClassName() == Kotule::class.asClassName() }) {
            fail("Kotule interface must inherit ${Kotule::class.qualifiedName}", kotuleInterface)
        }

        try {
            check(kotuleInterface.containingFile!!.getTarget() == KmpTarget.COMMON)
        }
        catch (e: Throwable) {
            logger.error("Must be in commonMain source set", kotuleInterface)
            throw e
        }
    }

    private fun fail(message: String, node: KSNode): Nothing {
        logger.error(message, node)
        throw RuntimeException()
    }
}

private fun KSFile.getTarget(): KmpTarget {
    val pathParts: MutableList<String> = filePath.split("/").dropLast(1).toMutableList()
    val packageParts: List<String> = packageName.asString().split(".")
    for (part in packageParts.asReversed()) {
        check(pathParts.removeLast() == part) { part }
    }

    check(pathParts.removeLast() == "kotlin")

    return when (val sourceSet: String = pathParts.last()) {
        "commonMain" -> KmpTarget.COMMON
        "jvmMain" -> KmpTarget.JVM
        "wasmJsMain" -> KmpTarget.WASMJS
        "jsMain" -> KmpTarget.JS
        else -> throw NotImplementedError("Unknown source set name '$sourceSet'")
    }
}
