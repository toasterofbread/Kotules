package dev.toastbits.kotules.extension

import dev.toastbits.kotules.extension.type.ValueType
import dev.toastbits.kotules.extension.type.checkKotulePromiseType
import dev.toastbits.kotules.extension.util.alertIfDelayUnavailable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
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
        Promise { resolve, reject ->
            GlobalScope.launch(Dispatchers.Default) {
                val result: T =
                    try {
                        getResult()
                    }
                    catch (e: Throwable) {
                        reject(e)
                        return@launch
                    }

                resolve(result)
            }
        }
//        GlobalScope.promise {
//            alertIfDelayUnavailable()
//            checkKotulePromiseType<T>()
//            return@promise getResult()
//        }
    )

actual suspend fun <T: ValueType?> KotulePromise<T>.await(): T = promise.await()
