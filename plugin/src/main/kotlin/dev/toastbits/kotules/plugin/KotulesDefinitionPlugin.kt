package dev.toastbits.kotules.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KotulesDefinitionPlugin: KotulePluginBase() {
    override fun apply(project: Project) {
        super.apply(project)

        project.dependencies.add("kspCommonMainMetadata", "dev.toastbits.kotules:extension-binder:$kotulesVersion")
    }
}
