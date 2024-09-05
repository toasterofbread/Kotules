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
                implementation(projects.extension)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.core)
            }

            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/commonMain")
        }

        val jvmMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/jvmMain")
        }

        val wasmJsMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/wasmJsMain")
        }

        val jsMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/jsMain")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.binder)
}

project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    outputs.upToDateWhen { false }
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
