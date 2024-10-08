import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import util.configureAllComposeTargets
import util.libCatalog
import util.version

plugins {
    id("android-library-conventions")
    id("kmp-conventions")

    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotules.definition)
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
                implementation(projects.sample.app)
                implementation(projects.extension)
                implementation(libs.ktor.core)

                // https://github.com/Kotlin/kotlinx.coroutines/issues/3874#issuecomment-2130428084
                implementation(devNpm("string-replace-loader", libCatalog.version("npm.string_replace_loader")))
            }
        }
    }
}
