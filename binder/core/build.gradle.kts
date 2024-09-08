import util.KmpTarget
import util.configureKmpTargets

plugins {
    id("kmp-conventions")
    id("publishing-conventions")

    alias(libs.plugins.kotlin)
}

kotlin {
    configureKmpTargets(KmpTarget.JVM)

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.ksp.api)
                implementation(libs.poet)
            }
        }
    }
}


val projectName: String = libs.versions.project.name.get()
val projectVersion: String = libs.versions.project.version.name.get()

mavenPublishing {
    coordinates("dev.toastbits.$projectName", "binder-core", projectVersion)
}

