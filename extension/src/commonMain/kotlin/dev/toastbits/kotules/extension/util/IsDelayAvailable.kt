package dev.toastbits.kotules.extension.util

import kotlinx.coroutines.CoroutineScope

expect fun CoroutineScope.isDelayAvailable(): Boolean
