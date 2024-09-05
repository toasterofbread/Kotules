package util

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.version(path: String): String =
    try {
        findVersion(path).get().requiredVersion
    }
    catch (e: Throwable) {
        throw Exception("Version of '$path' not found", e)
    }

fun VersionCatalog.library(path: String): MinimalExternalModuleDependency =
    try {
        findLibrary(path).get().get()
    }
    catch (e: Throwable) {
        throw Exception("Library '$path' not found", e)
    }

fun VersionCatalog.plugin(path: String): String =
    try {
        findPlugin(path).get().get().pluginId
    }
    catch (e: Throwable) {
        throw Exception("Plugin '$path' not found", e)
    }
