package dev.toastbits.kotules.binder.extension.processor

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.extension.generator.KotuleBindingClassGenerator
import dev.toastbits.kotules.binder.extension.generator.KotuleTypeGenerator
import dev.toastbits.kotules.binder.extension.util.KmpTarget
import dev.toastbits.kotules.binder.extension.util.KotuleExtensionBinderConstants
import dev.toastbits.kotules.binder.extension.util.getSourceSetName
import dev.toastbits.kotules.extension.annotation.KotuleImplementationAnnotation
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal class KotuleExtensionAnnotationProcessor(
    environment: SymbolProcessorEnvironment
): SymbolProcessor {
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val kotuleClasses: Sequence<KSClassDeclaration> =
            resolver.getSymbolsWithAnnotation(KotuleImplementationAnnotation::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()

        for (kotuleClass in kotuleClasses) {
            val annotation: KSAnnotation = kotuleClass.annotations.first { it.annotationType.resolve().toClassName() == KotuleImplementationAnnotation::class.asClassName() }
            val bindInterface: KSType = annotation.arguments.first { it.name?.asString() == KotuleImplementationAnnotation::bindInterface.name }.value as KSType

            validateKotuleClass(kotuleClass)
            generateClassBindings(kotuleClass, bindInterface)
        }

        return emptyList()
    }

    private fun generateClassBindings(kotuleClass: KSClassDeclaration, bindInterface: KSType) {
        val packageName: String = kotuleClass.packageName.asString()
        val bindingClassName: String = KotuleExtensionBinderConstants.getOutputBindingName(kotuleClass)

        lateinit var fileSpecBuilder: FileSpec.Builder
        val addImport: (String) -> Unit = {
            val split: List<String> = it.split('.')
            fileSpecBuilder.addImport(split.dropLast(1).joinToString("."), split.last())
        }

        val classGenerator: KotuleTypeGenerator = KotuleBindingClassGenerator(
            addImport = addImport
        )

        fun KmpTarget.generateFile(): FileSpec? {
            fileSpecBuilder = FileSpec.builder(packageName, bindingClassName)

            val bindingInterface: TypeSpec =
                classGenerator.generate(
                    bindingClassName,
                    kotuleClass,
                    bindInterface,
                    this
                ) ?: return null
            fileSpecBuilder.addType(bindingInterface)

            return fileSpecBuilder.build()
        }

        for (target in KmpTarget.entries) {
            val fileSpec: FileSpec = target.generateFile() ?: continue

            val outputPath: String = target.getSourceSetName() + "Main." + packageName
            val file: OutputStream =
                codeGenerator.createNewFile(
                    Dependencies(false),
                    outputPath,
                    bindingClassName,
                    extensionName = "${target.getSourceSetName()}.kt"
                )

            OutputStreamWriter(file, StandardCharsets.UTF_8).use(fileSpec::writeTo)
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
