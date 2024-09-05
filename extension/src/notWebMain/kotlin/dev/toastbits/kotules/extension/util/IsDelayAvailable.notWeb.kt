package dev.toastbits.kotules.extension.util

import kotlin.coroutines.CoroutineContext

actual fun CoroutineContext.isDelayAvailable(): Boolean = true
