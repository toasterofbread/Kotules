package dev.toastbits.kotules.extension.type

@Suppress("EXPECTED_EXTERNAL_DECLARATION")
expect external interface JsType

inline fun <reified T> checkKotulePromiseType() {
    when (T::class) {
        OutJsInt::class,
        OutJsString::class -> return
    }

    throw IllegalArgumentException("Invalid type for KotulePromise ${T::class}")
}
