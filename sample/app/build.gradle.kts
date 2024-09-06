import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import util.configureAllComposeTargets

plugins {
    id("android-application-conventions")
    id("compose-conventions")

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
                implementation(projects.runtime)
                implementation(libs.ktor.core)
            }

            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin/commonMain")
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/resources/commonMain")
        }

        val jvmMain by getting {
            dependencies {
                runtimeOnly(libs.ktor.client.cio)
            }

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
    add("kspCommonMainMetadata", projects.binder.runtime)
}

project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

afterEvaluate {
    tasks.named("run") {
        dependsOn(":sample:extension:jvmJar")
    }
}

for (target in listOf("wasmJs", "js")) {
    tasks.named("${target}BrowserDistribution") {
        dependOnTaskAndCopyOutputDirectory(":sample:extension:jsBrowserDistribution", "productionExecutable")
    }

    tasks.named("${target}BrowserDevelopmentExecutableDistribution") {
        dependOnTaskAndCopyOutputDirectory(":sample:extension:jsBrowserDevelopmentExecutableDistribution", "developmentExecutable")
    }
}

fun Task.dependOnTaskAndCopyOutputDirectory(taskPath: String, dirName: String) {
    dependsOn(taskPath)

    outputs.upToDateWhen { false }

    doLast {
        val appProductionExecutable: File = outputs.files.single { it.name == dirName }

        val taskParts: List<String> = taskPath.split(':').filter { it.isNotBlank() }
        var currentProject = rootProject
        for (i in 0 until taskParts.size - 1) {
            currentProject = currentProject.project(taskParts[i])
        }

        val workerBuildTask: Task by currentProject.tasks.named(taskParts.last())
        val workerProductionExecutable: File = workerBuildTask.outputs.files.single { it.name == dirName }

        for (file in workerProductionExecutable.listFiles().orEmpty()) {
            file.copyRecursively(appProductionExecutable.resolve(file.name), overwrite = true)
        }
    }
}
