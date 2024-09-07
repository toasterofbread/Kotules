@file:OptIn(ExperimentalJsExport::class)

package dev.toastbits.kotules.extension

import dev.toastbits.kotules.extension.type.ValueType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import kotlin.js.Promise

actual external class KotulePromise<T: ValueType?> {
    val promise: Promise<T>
}

@JsExport
actual class OutKotulePromise(val promise: Promise<*>)

@OptIn(DelicateCoroutinesApi::class)
actual inline fun <reified T> kotulePromise(noinline getResult: suspend () -> T): OutKotulePromise =
    OutKotulePromise(
        GlobalScope.promise { getResult() }
    )

actual suspend fun <T: ValueType?> KotulePromise<T>.await(): T = promise.await()
