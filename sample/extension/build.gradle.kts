import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import util.configureAllComposeTargets

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
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.core)
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

project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
