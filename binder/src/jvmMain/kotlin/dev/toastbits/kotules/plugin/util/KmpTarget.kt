package dev.toastbits.kotules.plugin.util

internal enum class KmpTarget {
    COMMON,
    JVM,
    WASMJS,
    JS;

    fun isWeb(): Boolean =
        this == WASMJS || this == JS
}

internal fun KmpTarget.getSourceSetName(): String =
    when (this) {
        KmpTarget.COMMON -> "commonMain"
        KmpTarget.JVM -> "jvmMain"
        KmpTarget.WASMJS -> "wasmJsMain"
        KmpTarget.JS -> "jsMain"
    }
