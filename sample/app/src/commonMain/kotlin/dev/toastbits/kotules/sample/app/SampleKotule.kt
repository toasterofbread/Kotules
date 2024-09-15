package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.core.Kotule
import dev.toastbits.kotules.runtime.annotation.KotuleDeclaration
import kotlin.js.JsName

@KotuleDeclaration
interface SampleKotule: Kotule {
    val intProperty: Int
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

    fun inputTest(input: SampleInputInterface): String
}

interface SampleInputInterface {
    @JsName("getText")
    fun getText(): String
}

data class SampleDataClass(
    val stringValue: String,
    val dataValue: SampleDataClass?,
    val list: List<Int> = emptyList()
)
