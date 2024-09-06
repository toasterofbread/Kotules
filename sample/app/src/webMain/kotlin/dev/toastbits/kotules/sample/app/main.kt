@file:OptIn(DelicateCoroutinesApi::class)

package dev.toastbits.kotules.sample.app

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun main() {
    GlobalScope.launch {
        runSampleKotule()
    }
}
