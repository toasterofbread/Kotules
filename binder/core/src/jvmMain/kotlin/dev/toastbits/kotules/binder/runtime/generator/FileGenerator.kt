package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.getSourceSetName
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

class FileGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    private data class FileLocation(val packageName: String, val name: String)
    private val filesToWrite: MutableMap<FileLocation, MutableMap<KmpTarget, FileSpec?>> = mutableMapOf()

    private var log: String = "\n"
    fun log(msg: Any?) {
        log += "$msg\n"
    }

    fun generate(
        packageName: String,
        name: String,
        target: KmpTarget,
        generationScope: Scope.() -> Unit
    ): ClassName {
        val fileLocation: FileLocation = FileLocation(packageName, name)
        log("generate($fileLocation)")

        if (filesToWrite[fileLocation]?.containsKey(target) == true) {
            return ClassName(packageName, name)
        }

        filesToWrite.getOrPut(fileLocation) { mutableMapOf() }[target] = null

        val fileSpecBuilder: FileSpec.Builder = FileSpec.builder(packageName, name)

        val scope: Scope = Scope(target, fileSpecBuilder, packageName)

        try {
            generationScope(scope)
        }
        catch (e: Throwable) {
            logger.error("$log\n\n$fileLocation ${e.stackTraceToString()}")
        }

        filesToWrite[fileLocation]!![target] = scope.file.build()

        return ClassName(packageName, name)
    }

    fun writeToDisk() {
        removeEmptyFiles()

        for ((location, targets) in filesToWrite) {
            val groupsWithCommon: Map<String, List<KmpTarget>> = KmpTarget.GROUPS.entries.associate { it.key to it.value + KmpTarget.COMMON }
            var groupFound: Boolean = false

            val allowedGroups: Map<String, List<KmpTarget>> =
                if (targets.keys.singleOrNull() == KmpTarget.COMMON) mapOf("common" to listOf(KmpTarget.COMMON))
                else mapOf("common" to KmpTarget.entries) + groupsWithCommon

            for ((groupName, groupTargets) in allowedGroups) {
                if (targets.keys.size != groupTargets.size || !targets.keys.containsAll(groupTargets)) {
                    continue
                }

                groupFound = true

                for ((target, file) in targets) {
                    writeFile(file!!, location, target, groupName)
                }
                break
            }

            if (groupFound) {
                continue
            }
            
            for ((groupName, groupTargets) in KmpTarget.GROUPS) {
                var file: FileSpec? = null
                for (target in groupTargets) {
                    val targetFile: FileSpec? = targets[target]
                    if (targetFile == null || (file != null && file != targetFile)) {
                        file = null
                        break
                    }
                    file = targetFile
                }

                if (file != null) {
                    writeFile(file, location, KmpTarget.COMMON, groupName)
                    groupFound = true
                    break
                }
            }

            if (groupFound) {
                continue
            }

            for ((target, file) in targets) {
                writeFile(file!!, location, target)
            }
        }

        filesToWrite.clear()
    }

    fun generate(
        className: ClassName,
        target: KmpTarget,
        generationScope: Scope.() -> Unit
    ): ClassName =
        generate(className.packageName, className.simpleName, target, generationScope)

    private fun removeEmptyFiles() {
        for ((location, targets) in filesToWrite) {
            for ((target, file) in targets.toMap()) {
                if (file?.typeSpecs.isNullOrEmpty()) {
                    targets.remove(target)
                }
            }
        }
    }

    private fun writeFile(
        fileSpec: FileSpec,
        location: FileLocation,
        target: KmpTarget,
        commonGroupName: String = "common"
    ) {
        val outputPath: String = (
            if (target == KmpTarget.COMMON) commonGroupName
            else target.getSourceSetName()
        ) + "Main." + location.packageName

        val file: OutputStream =
            try {
                codeGenerator.createNewFile(
                    Dependencies(false),
                    outputPath,
                    location.name,
                    extensionName =
                        if (target == KmpTarget.COMMON)
                            if (commonGroupName == "common") "kt"
                            else "$commonGroupName.kt"
                        else "${target.getSourceSetName()}.kt"
                )
            }
            catch (e: Throwable) {
                throw RuntimeException("$log\n\n$location | $target | $commonGroupName", e)
            }

        OutputStreamWriter(file, StandardCharsets.UTF_8).use(fileSpec::writeTo)
    }

    inner class Scope(
        val target: KmpTarget,
        val file: FileSpec.Builder,
        private val packageName: String
    ) {
        val logger: KSPLogger get() = this@FileGenerator.logger

        fun resolveInPackage(name: String): ClassName = ClassName(packageName, name)

        fun import(packageName: String, name: String): ClassName {
            file.addImport(packageName, name)
            return resolveInPackage(name)
        }

        fun import(cls: KClass<*>): ClassName {
            val split: List<String> = cls.qualifiedName!!.split('.')
            return import(split.dropLast(1).joinToString("."), split.last())
        }

        fun importFromPackage(name: String): ClassName =
            import(packageName, name)

        fun generateNew(className: ClassName, generationScope: Scope.() -> Unit): ClassName =
            generate(className, target, generationScope)

        fun log(msg: Any?) = this@FileGenerator.log(msg)
    }
}
