package dev.toastbits.kotules.binder.runtime.util

internal enum class KmpTarget {
    COMMON,
    JVM,
    WASMJS,
    JS;

    fun isWeb(): Boolean =
        this == WASMJS || this == JS

    companion object {
        val GROUPS: Map<String, List<KmpTarget>> =
            mapOf(
                "web" to listOf(WASMJS, JS)
            )
    }
}

internal fun KmpTarget.getSourceSetName(): String =
    when (this) {
        KmpTarget.COMMON -> "common"
        KmpTarget.JVM -> "jvm"
        KmpTarget.WASMJS -> "wasmJs"
        KmpTarget.JS -> "js"
    }
