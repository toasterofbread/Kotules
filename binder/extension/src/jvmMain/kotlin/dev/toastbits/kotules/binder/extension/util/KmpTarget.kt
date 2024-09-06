package dev.toastbits.kotules.binder.extension.util

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
        KmpTarget.COMMON -> "common"
        KmpTarget.JVM -> "jvm"
        KmpTarget.WASMJS -> "wasmJs"
        KmpTarget.JS -> "js"
    }
