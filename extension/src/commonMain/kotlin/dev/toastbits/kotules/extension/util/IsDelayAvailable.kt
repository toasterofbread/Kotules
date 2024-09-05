package dev.toastbits.kotules.extension.util

import kotlin.coroutines.CoroutineContext

expect fun CoroutineContext.isDelayAvailable(): Boolean
