import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import util.configureAllComposeTargets
import util.libCatalog
import util.version

plugins {
    id("android-library-conventions")
    id("kmp-conventions")

    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
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

            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/commonMain")
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/resources/commonMain")
        }

        val jvmMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/jvmMain")
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/resources/jvmMain")
        }

        val webMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/webMain")
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/resources/webMain")
        }

        val wasmJsMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/wasmJsMain")
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/resources/wasmJsMain")
        }

        val jsMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/jsMain")
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/resources/jsMain")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.binder.extension)
}

//project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
//    if (name != "kspCommonMainKotlinMetadata") {
//        dependsOn("kspCommonMainKotlinMetadata")
//    }
//}
