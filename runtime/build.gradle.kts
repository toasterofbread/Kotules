import util.KmpTarget
import util.configureKmpTargets

plugins {
//    id("android-library-conventions")
    id("kmp-conventions")
    id("publishing-conventions")

    alias(libs.plugins.kotlin)
}

kotlin {
    configureKmpTargets(*KmpTarget.SUPPORTED)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.core)
                implementation(projects.extension)
            }
        }
    }
}

val projectName: String = libs.versions.project.name.get()
val projectVersion: String = libs.versions.project.version.name.get()

mavenPublishing {
    coordinates("dev.toastbits.$projectName", "runtime", projectVersion)
}
