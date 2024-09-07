package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import dev.toastbits.kotules.binder.runtime.util.getSourceSetName
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

class FileGenerator(
    private val codeGenerator: CodeGenerator
) {
    fun generate(
        packageName: String,
        name: String,
        target: KmpTarget,
        generationScope: Scope.() -> Unit
    ): ClassName {
        val fileSpecBuilder: FileSpec.Builder = FileSpec.builder(packageName, name)

        val scope: Scope = Scope(target, fileSpecBuilder, packageName)
        generationScope(scope)

        val outputPath: String = target.getSourceSetName() + "Main." + packageName
        val file: OutputStream =
            codeGenerator.createNewFile(
                Dependencies(false),
                outputPath,
                name,
                extensionName =
                    if (target == KmpTarget.COMMON) "kt"
                    else "${target.getSourceSetName()}.kt"
            )

        val fileSpec: FileSpec = scope.file.build()
        OutputStreamWriter(file, StandardCharsets.UTF_8).use(fileSpec::writeTo)

        return ClassName(packageName, name)
    }

    fun generate(
        className: ClassName,
        target: KmpTarget,
        generationScope: Scope.() -> Unit
    ): ClassName =
        generate(className.packageName, className.simpleName, target, generationScope)

    inner class Scope(
        val target: KmpTarget,
        val file: FileSpec.Builder,
        private val packageName: String
    ) {
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
    }
}
