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
                implementation(projects.extension)
                implementation(libs.ksp.api)
                implementation(libs.poet)
                implementation(libs.poet.ksp)
            }
        }
    }
}

val projectName: String = libs.versions.project.name.get()
val projectVersion: String = project.libs.versions.project.name.get()

mavenPublishing {
    coordinates("dev.toastbits.$projectName", "binder", projectVersion)
}

