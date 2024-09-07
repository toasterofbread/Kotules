import util.KmpTarget
import util.configureKmpTargets

plugins {
    id("kmp-conventions")
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
