package dev.toastbits.kotules.plugin

import com.google.devtools.ksp.gradle.KspGradleSubplugin
import dev.toastbits.kotules.binder.runtime.util.KmpTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

abstract class KotulePluginBase: Plugin<Project> {
    protected val kotulesVersion: String get() = "0.0.1" // TODO

    override fun apply(project: Project) {
        project.plugins.apply {
            @Suppress("UNCHECKED_CAST")
            apply(KspGradleSubplugin::class.java as Class<Plugin<*>>)
        }

        project.tasks.withType(KotlinCompilationTask::class.java) { task ->
            if (task.name != "kspCommonMainKotlinMetadata") {
                task.dependsOn("kspCommonMainKotlinMetadata")
            }
        }

        project.afterEvaluate {
            project.kotlinExtension.sourceSets.apply {
                val sourceSets: List<String> = listOf("common", "jvm", "wasmJs", "js") + KmpTarget.GROUPS.keys
                for (sourceSet in sourceSets.map { it + "Main" }) {

                    try {
                        getByName(sourceSet).kotlin.srcDirs(
                            "build/generated/ksp/metadata/commonMain/kotlin/$sourceSet",
                            "build/generated/ksp/metadata/commonMain/resources/$sourceSet"
                        )
                    }
                    catch (_: UnknownDomainObjectException) {}
                }
            }
        }
    }
}
