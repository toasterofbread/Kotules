package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.runtime.annotation.KotuleAnnotation

@KotuleAnnotation
interface SampleKotule: Kotule {
    val coolProperty: Int
    fun repeatInput(input: String, repeatCount: Int): String
    suspend fun downloadFortune(): String
}
