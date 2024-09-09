@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")

    resolutionStrategy {
        eachPlugin {
            for (plugin in listOf("declaration", "definition")) {
                if (requested.id.id == "dev.toastbits.kotules.plugin.$plugin") {
                    useModule("dev.toastbits.kotules.plugin.$plugin:dev.toastbits.kotules.plugin.$plugin.gradle.plugin:${requested.version}")
                }
            }
        }
    }

    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "Kotules"

include(":extension")
include(":runtime")
include(":plugin")
include(":binder:extension")
include(":binder:core")
include(":binder:runtime")

//include(":sample:app")
//include(":sample:extension")
