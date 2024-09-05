package dev.toastbits.kotules.sample.extension

import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.extension.PlatformJsExport

external interface SampleKotule: Kotule {
    fun test(): String
}

@PlatformJsExport
class SampleKotuleImpl {
    fun test(): String = "Hello from SampleKotuleImpl!"
}
