package dev.toastbits.kotules.binder.runtime.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.runtime.generator.KotuleBindingInterfaceGenerator
import dev.toastbits.kotules.binder.runtime.generator.KotuleLoaderGenerator
import dev.toastbits.kotules.binder.runtime.generator.KotuleMapperClassGenerator
import dev.toastbits.kotules.binder.runtime.generator.KotuleTypeGenerator
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.getSourceSetName
import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.runtime.annotation.KotuleAnnotation
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal class KotuleRuntimeAnnotationProcessor(
    environment: SymbolProcessorEnvironment
): SymbolProcessor {
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val kotuleInterfaces: Sequence<KSClassDeclaration> =
            resolver.getSymbolsWithAnnotation(KotuleAnnotation::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()

        for (kotuleInterface in kotuleInterfaces) {
            validateKotuleClass(kotuleInterface)
            generateClassBindings(kotuleInterface)
        }

        return emptyList()
    }

    private fun generateClassBindings(kotuleInterface: KSClassDeclaration) {
        val packageName: String = kotuleInterface.packageName.asString()

        val imports: MutableList<Pair<String, String>> = mutableListOf()

        val addImport: (String, String) -> Unit = { pkg, name -> imports.add(pkg to name) }

        val interfaceGenerator: KotuleTypeGenerator = KotuleBindingInterfaceGenerator()
        val mapperClassGenerator: KotuleTypeGenerator = KotuleMapperClassGenerator(addImport)
        val loaderGenerator: KotuleTypeGenerator = KotuleLoaderGenerator(addImport)

        for (
            generator in listOf(
                interfaceGenerator,
                mapperClassGenerator,
                loaderGenerator
            )
        ) {
            fun KmpTarget.generateFile(): FileSpec? {
                val bindingInterface: TypeSpec =
                    generator.generate(
                        kotuleInterface,
                        this,
                        packageName
                    ) ?: return null

                val fileSpecBuilder: FileSpec.Builder = FileSpec.builder(packageName, bindingInterface.name!!)
                fileSpecBuilder.addType(bindingInterface)

                for ((pkg, import) in imports) {
                    fileSpecBuilder.addImport(pkg, import)
                }
                imports.clear()

                return fileSpecBuilder.build()
            }

            val targets: MutableList<KmpTarget> = KmpTarget.entries.toMutableList()

            for ((groupName, groupTargets) in KmpTarget.GROUPS) {
                if (groupTargets.isEmpty()) {
                    continue
                }

                var matching: Boolean = true
                var previousFile: FileSpec? = null
                for (target in groupTargets) {
                    val file: FileSpec? = target.generateFile()
                    if (file == null || (previousFile != null && file != previousFile)) {
                        matching = false
                        break
                    }
                    previousFile = file
                }

                if (matching) {
                    val outputPath: String = "${groupName}Main.$packageName"
                    val file: OutputStream =
                        codeGenerator.createNewFile(
                            Dependencies(false),
                            outputPath,
                            previousFile!!.name,
                            extensionName = "$groupName.kt"
                        )

                    OutputStreamWriter(file, StandardCharsets.UTF_8).use(previousFile::writeTo)
                    targets.removeAll(groupTargets)
                }
            }

            for (target in targets) {
                val fileSpec: FileSpec = target.generateFile() ?: continue

                val outputPath: String = target.getSourceSetName() + "Main." + packageName
                val file: OutputStream =
                    codeGenerator.createNewFile(
                        Dependencies(false),
                        outputPath,
                        fileSpec.name,
                        extensionName = "${target.getSourceSetName()}.kt"
                    )

                OutputStreamWriter(file, StandardCharsets.UTF_8).use(fileSpec::writeTo)
            }
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
