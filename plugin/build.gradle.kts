@file:Suppress("UnstableApiUsage")

plugins {
    id("jvm-conventions")
    alias(libs.plugins.gradle.plugin.publish)
}

group = "dev.toastbits.kotules"
version = project.libs.versions.project.version.name.get()

gradlePlugin {
    website = "https://github.com/toasterofbread/Kotules"
    vcsUrl = "https://github.com/toasterofbread/Kotules.git"
    plugins {
        create("kotules-declaration") {
            id = "dev.toastbits.kotules.plugin.declaration"
            displayName = "Kotules"
            description = "TODO"
            tags = listOf("kmp", "multiplatform")
            implementationClass = "dev.toastbits.kotules.plugin.KotulesDeclarationPlugin"
        }
        create("kotules-definition") {
            id = "dev.toastbits.kotules.plugin.definition"
            displayName = "Kotules"
            description = "TODO"
            tags = listOf("kmp", "multiplatform")
            implementationClass = "dev.toastbits.kotules.plugin.KotulesDefinitionPlugin"
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(projects.binder.core)

    implementation(libs.kotlin.plugin)
    implementation(libs.ksp.plugin)
}

publishing {
    repositories {
        mavenLocal()
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named("publishToMavenLocal") {
    dependsOn(":runtime:publishToMavenLocal")
    dependsOn(":extension:publishToMavenLocal")
    dependsOn(":binder:runtime:publishToMavenLocal")
    dependsOn(":binder:core:publishToMavenLocal")
    dependsOn(":binder:extension:publishToMavenLocal")
}
