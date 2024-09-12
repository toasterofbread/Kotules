import util.KmpTarget
import util.configureKmpTargets

plugins {
    id("kmp-conventions")
    id("publishing-conventions")

    alias(libs.plugins.kotlin)
    alias(libs.plugins.publish)
}

kotlin {
    configureKmpTargets(KmpTarget.JVM)

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.binder.core)
                implementation(projects.runtime)
                implementation(projects.extension)
                implementation(libs.ksp.api)
                implementation(libs.poet)
                implementation(libs.poet.ksp)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

val projectName: String = libs.versions.project.name.get()
val projectVersion: String = libs.versions.project.version.name.get()

mavenPublishing {
    coordinates("dev.toastbits.$projectName", "runtime-binder", projectVersion)
}

