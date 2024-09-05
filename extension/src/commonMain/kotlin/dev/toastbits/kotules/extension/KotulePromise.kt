package dev.toastbits.kotules.extension

import dev.toastbits.kotules.extension.type.JsType
import kotlinx.coroutines.CoroutineScope

@Suppress("EXPECTED_EXTERNAL_DECLARATION")
expect external class KotulePromise<T: JsType?>

expect class OutKotulePromise

expect inline fun <reified T> kotulePromise(noinline getResult: suspend CoroutineScope.() -> T): OutKotulePromise

expect suspend fun <T: JsType?> KotulePromise<T>.await(): T
