package dev.toastbits.kotules.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KotulesDeclarationPlugin: KotulePluginBase(false) {
    override fun apply(project: Project) {
        super.apply(project)

//        project.dependencies.add("kspCommonMainMetadata", "dev.toastbits.kotules:runtime-binder:$kotulesVersion")
    }
}
