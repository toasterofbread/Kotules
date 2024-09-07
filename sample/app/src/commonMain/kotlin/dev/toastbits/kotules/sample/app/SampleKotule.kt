package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.runtime.annotation.KotuleAnnotation

@KotuleAnnotation
interface SampleKotule: Kotule {
    val coolProperty: Int
    fun repeatInput(input: String, repeatCount: Int): String

    suspend fun suspendInt(): Int

    suspend fun downloadFortune(): String

    suspend fun getDataClass(): SampleDataClass
}

data class SampleDataClass(
    val stringValue: String,
    val dataValue: SampleDataClass?
//    val list: List<Int>
)
