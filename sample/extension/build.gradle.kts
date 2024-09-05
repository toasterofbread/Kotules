import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import util.configureAllComposeTargets

plugins {
    id("android-library-conventions")
    id("kmp-conventions")

    alias(libs.plugins.kotlin)
}

kotlin {
    configureAllComposeTargets {
        if (this is KotlinJsTargetDsl) {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.extension)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.core)
            }
        }
    }
}
