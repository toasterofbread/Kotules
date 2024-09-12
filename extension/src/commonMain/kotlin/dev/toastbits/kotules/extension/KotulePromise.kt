package dev.toastbits.kotules.extension

import dev.toastbits.kotules.core.type.ValueType

expect class KotulePromise<T: ValueType?>

expect class OutKotulePromise

expect inline fun <reified T> kotulePromise(noinline getResult: suspend () -> T): OutKotulePromise

expect suspend fun <T: ValueType?> KotulePromise<T>.await(): T
