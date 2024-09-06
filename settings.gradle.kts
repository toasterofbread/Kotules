@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "Kotules"
include(":extension")
include(":runtime")
include(":binder:extension")
include(":binder:runtime")

include(":sample:app")
include(":sample:extension")
