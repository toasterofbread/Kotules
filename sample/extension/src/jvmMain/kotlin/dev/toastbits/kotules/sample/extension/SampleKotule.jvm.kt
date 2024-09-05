package dev.toastbits.kotules.sample.extension

import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.type.StringValue

actual interface SampleKotule: Kotule {
    actual fun repeatInput(input: String, repeatCount: Int): String
    actual fun downloadFortune(): KotulePromise<StringValue>
}
