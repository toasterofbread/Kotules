package dev.toastbits.kotules.extension.util

import kotlin.coroutines.coroutineContext

private var delayWarningSent: Boolean = false

suspend fun alertIfDelayUnavailable() {
    if (!delayWarningSent && !coroutineContext.isDelayAvailable()) {
        UnsupportedOperationException("WARNING: No delay implementation is available in the current coroutine scope. Using delay() may trigger a ClassCastException().").printStackTrace()
        delayWarningSent = true
    }
}
