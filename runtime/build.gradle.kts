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
                api(projects.extension)
            }
        }

        val jvmMain by getting {
            dependencies {
//                implementation(kotlin("reflect"))
            }
        }
    }
}

val projectName: String = libs.versions.project.name.get()
val projectVersion: String = project.libs.versions.project.name.get()

mavenPublishing {
    coordinates("dev.toastbits.$projectName", "runtime", projectVersion)
}
