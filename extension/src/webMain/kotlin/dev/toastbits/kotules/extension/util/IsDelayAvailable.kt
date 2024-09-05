package dev.toastbits.kotules.extension.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.ContinuationInterceptor

@OptIn(InternalCoroutinesApi::class)
actual fun CoroutineScope.isDelayAvailable(): Boolean {
    if (coroutineContext[ContinuationInterceptor] is Delay) {
        return true
    }

    // https://github.com/Kotlin/kotlinx.coroutines/blob/d38672a53ccdfda6319ab5f11669af8ad96d60a5/kotlinx-coroutines-core/jsAndWasmShared/src/CoroutineContext.kt#L7
    if (Dispatchers.Default is Delay) {
        return true
    }

    return false
}
