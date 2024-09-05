import util.configureAllKmpTargets

plugins {
    id("kmp-conventions")
    id("android-library-conventions")
    id("publishing-conventions")

    alias(libs.plugins.kotlin)
    alias(libs.plugins.publish)
}

kotlin {
    configureAllKmpTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

val projectName: String = libs.versions.project.name.get()
val projectVersion: String = project.libs.versions.project.name.get()

mavenPublishing {
    coordinates("dev.toastbits.$projectName", "extension", projectVersion)
}
