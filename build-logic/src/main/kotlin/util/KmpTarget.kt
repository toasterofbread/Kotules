package util

enum class KmpTarget {
    JVM,
    ANDROID,
    NATIVE,
    WASMJS,
    JS;

    companion object {
        val ALL_COMPOSE: Array<KmpTarget> = arrayOf(JVM, ANDROID, WASMJS, JS)
    }
}
