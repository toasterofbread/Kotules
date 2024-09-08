import util.KmpTarget
import util.configureKmpTargets

plugins {
//    id("android-library-conventions")
    id("kmp-conventions")
    id("publishing-conventions")

    alias(libs.plugins.kotlin)
    alias(libs.plugins.publish)
}

kotlin {
    configureKmpTargets(*KmpTarget.SUPPORTED)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

val projectName: String = libs.versions.project.name.get()
val projectVersion: String = libs.versions.project.version.name.get()

mavenPublishing {
    coordinates("dev.toastbits.$projectName", "extension", projectVersion)
}
