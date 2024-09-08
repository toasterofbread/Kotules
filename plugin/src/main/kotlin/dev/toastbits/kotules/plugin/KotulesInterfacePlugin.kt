package dev.toastbits.kotules.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KotulesInterfacePlugin: KotulePluginBase() {
    override fun apply(project: Project) {
        super.apply(project)

        project.dependencies.add("kspCommonMainMetadata", "dev.toastbits.kotules:runtime-binder:$kotulesVersion")

        project.afterEvaluate {
            project.kotlinExtension.sourceSets.named("commonMain").configure {
                it.dependencies {
                    implementation("dev.toastbits.kotules:runtime:$kotulesVersion")
                }
            }
        }
    }
}
