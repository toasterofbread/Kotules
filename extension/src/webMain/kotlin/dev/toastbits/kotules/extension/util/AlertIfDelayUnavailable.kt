package dev.toastbits.kotules.extension.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.ContinuationInterceptor

private var delayWarningSent: Boolean = false

fun CoroutineScope.alertIfDelayUnavailable() {
    if (!delayWarningSent && !isDelayAvailable()) {
        UnsupportedOperationException("WARNING: No delay implementation is available in the current coroutine scope. Using delay() may trigger a ClassCastException().").printStackTrace()
        delayWarningSent = true
    }
}
