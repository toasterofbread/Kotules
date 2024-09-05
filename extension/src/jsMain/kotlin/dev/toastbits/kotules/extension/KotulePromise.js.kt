@file:OptIn(ExperimentalJsExport::class)

package dev.toastbits.kotules.extension

import dev.toastbits.kotules.extension.type.JsType
import dev.toastbits.kotules.extension.type.checkKotulePromiseType
import dev.toastbits.kotules.extension.util.alertIfDelayUnavailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import kotlin.js.Promise

actual external class KotulePromise<T: JsType?> {
    val promise: Promise<T>
}

@JsExport
actual class OutKotulePromise(val promise: Promise<*>)

@OptIn(DelicateCoroutinesApi::class)
actual inline fun <reified T> kotulePromise(noinline getResult: suspend CoroutineScope.() -> T): OutKotulePromise =
    OutKotulePromise(
        GlobalScope.promise {
            alertIfDelayUnavailable()
            checkKotulePromiseType<T>()
            return@promise getResult()
        }
    )

actual suspend fun <T: JsType?> KotulePromise<T>.await(): T = promise.await()
