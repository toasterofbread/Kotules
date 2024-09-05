package dev.toastbits.kotules.extension.type

expect interface ValueType

inline fun <reified T> checkKotulePromiseType() {
    when (T::class) {
        OutJsInt::class,
        OutStringValue::class -> return
    }

    throw IllegalArgumentException("Invalid type for KotulePromise ${T::class}")
}
