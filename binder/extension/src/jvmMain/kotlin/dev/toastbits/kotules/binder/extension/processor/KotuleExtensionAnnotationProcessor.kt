package dev.toastbits.kotules.binder.extension.processor

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import dev.toastbits.kotules.binder.extension.generator.KotuleBindingClassGenerator
import dev.toastbits.kotules.binder.extension.util.KotuleExtensionBinderConstants
import dev.toastbits.kotules.binder.runtime.generator.FileGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.extension.annotation.KotuleDefinition

internal class KotuleExtensionAnnotationProcessor(
    environment: SymbolProcessorEnvironment
): SymbolProcessor {
    private val fileGenerator: FileGenerator = FileGenerator(environment.codeGenerator, environment.logger)
    private val logger: KSPLogger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val kotuleClasses: Sequence<KSClassDeclaration> =
            resolver.getSymbolsWithAnnotation(KotuleDefinition::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()

        for (kotuleClass in kotuleClasses) {
            validateKotuleClass(kotuleClass)
            generateClassBindings(kotuleClass)
        }

        fileGenerator.writeToDisk()

        return emptyList()
    }

    private fun generateClassBindings(kotuleClass: KSClassDeclaration) {
        val packageName: String = kotuleClass.packageName.asString()
        val bindingClassName: String = KotuleExtensionBinderConstants.getOutputBindingName(kotuleClass)

        for (target in KmpTarget.entries) {
            fileGenerator.generate(ClassName(packageName, bindingClassName), target) {
                KotuleBindingClassGenerator(this).generate(bindingClassName, kotuleClass)?.also {
                    file.addType(it)
                }
            }
        }
    }

    private fun validateKotuleClass(kotuleClass: KSClassDeclaration) {
        if (kotuleClass.modifiers.contains(Modifier.ABSTRACT)) {
            fail("Kotule class may not be abstract", kotuleClass)
        }

        if (kotuleClass.getConstructors().firstOrNull() == null) {
            fail("Kotule class must have at least one constructor", kotuleClass)
        }

        try {
            check(kotuleClass.containingFile!!.getTarget() == KmpTarget.COMMON)
        }
        catch (e: Throwable) {
            logger.error("Must be in commonMain source set", kotuleClass)
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
