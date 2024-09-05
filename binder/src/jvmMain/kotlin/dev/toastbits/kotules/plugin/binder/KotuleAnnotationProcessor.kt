package dev.toastbits.kotules.plugin.binder

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import dev.toastbits.kotules.extension.annotation.KotuleAnnotation
import dev.toastbits.kotules.plugin.generator.KotuleBindingClassGenerator
import dev.toastbits.kotules.plugin.generator.KotuleBindingGenerator
import dev.toastbits.kotules.plugin.generator.KotuleBindingInterfaceGenerator
import dev.toastbits.kotules.plugin.util.KmpTarget
import dev.toastbits.kotules.plugin.util.KotuleGenerationConstants
import dev.toastbits.kotules.plugin.util.getSourceSetName
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal class KotuleAnnotationProcessor(
    environment: SymbolProcessorEnvironment
): SymbolProcessor {
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val kotuleClasses: Sequence<KSClassDeclaration> =
            resolver.getSymbolsWithAnnotation(KotuleAnnotation::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()

        for (kotuleClass in kotuleClasses) {
            validateKotuleClass(kotuleClass)
            generateClassBindings(kotuleClass)
        }

        return emptyList()
    }

    private fun generateClassBindings(kotuleClass: KSClassDeclaration) {
        val packageName: String = kotuleClass.packageName.asString()
        val bindingClassName: String = kotuleClass.simpleName.asString() + KotuleGenerationConstants.OUTPUT_BINDING_NAME_SUFFIX
        val bindingInterfaceName: String = kotuleClass.simpleName.asString() + KotuleGenerationConstants.INPUT_BINDING_NAME_SUFFIX

        for (target in KmpTarget.entries) {
            lateinit var fileSpecBuilder: FileSpec.Builder

            val interfaceGenerator: KotuleBindingGenerator = KotuleBindingInterfaceGenerator()
            val classGenerator: KotuleBindingGenerator = KotuleBindingClassGenerator(
                addImport = {
                    val split: List<String> = it.split('.')
                    fileSpecBuilder.addImport(split.dropLast(1).joinToString("."), split.last())
                }
            )

            for ((name, generator) in listOf(bindingClassName to classGenerator, bindingInterfaceName to interfaceGenerator)) {
                fileSpecBuilder = FileSpec.builder(packageName, name)

                val bindingInterface: TypeSpec = generator.generate(name, kotuleClass, target) ?: continue
                fileSpecBuilder.addType(bindingInterface)

                val fileSpec: FileSpec = fileSpecBuilder.build()

                val outputPath: String = target.getSourceSetName() + "." + packageName
                val file: OutputStream =
                    codeGenerator.createNewFile(
                        Dependencies(false),
                        outputPath,
                        name
                    )

                OutputStreamWriter(file, StandardCharsets.UTF_8).use(fileSpec::writeTo)
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
