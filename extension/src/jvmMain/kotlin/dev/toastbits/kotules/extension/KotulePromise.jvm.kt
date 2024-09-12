package dev.toastbits.kotules.extension

import dev.toastbits.kotules.core.type.ValueType

actual class KotulePromise<T : ValueType?>(val action: suspend () -> T)

actual class OutKotulePromise(val action: suspend () -> Any?)

actual inline fun <reified T> kotulePromise(noinline getResult: suspend () -> T): OutKotulePromise =
    OutKotulePromise(getResult)

actual suspend fun <T: ValueType?> KotulePromise<T>.await(): T =
    action()
