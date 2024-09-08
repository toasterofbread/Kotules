package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.runtime.annotation.KotuleAnnotation

@KotuleAnnotation
interface SampleKotule: Kotule {
    val coolProperty: Int
    fun repeatInput(input: String, repeatCount: Int): String

    suspend fun getList(): List<SampleDataClass> =
        listOf(
            SampleDataClass("one", SampleDataClass("1.1", SampleDataClass("1.2", null))),
            SampleDataClass("two", null, listOf(5, 6, 7, 8)),
            SampleDataClass("three", null, listOf(9, 10, 11, 12, 13))
        )

    suspend fun suspendInt(): Int

    suspend fun downloadFortune(): String

    suspend fun getDataClass(): SampleDataClass
}

data class SampleDataClass(
    val stringValue: String,
    val dataValue: SampleDataClass?,
    val list: List<Int> = emptyList()
)
