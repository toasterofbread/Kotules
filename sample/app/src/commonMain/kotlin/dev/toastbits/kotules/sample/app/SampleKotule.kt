package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.core.Kotule
import dev.toastbits.kotules.runtime.annotation.KotuleDeclaration
import kotlin.js.JsName

@KotuleDeclaration
interface SampleKotule: Kotule {
    val intProperty: Int
    fun repeatInput(input: String, repeatCount: Int): String

    fun getList(): List<SampleDataClass> =
        listOf(
            SampleDataClass("one"),
            SampleDataClass("two", listOf(5, 6, 7, 8)),
            SampleDataClass("three", listOf(9, 10, 11, 12, 13))
        )

    fun suspendInt(): Int

    fun downloadFortune(): String

    fun getDataClass(): SampleDataClass

    fun inputTest(input: SampleInputInterface): String
}

interface SampleInputInterface {
    @JsName("getText")
    fun getText(): String
}

data class SampleDataClass(
    val stringValue: String,
//    val dataValue: SampleDataClass?,
    val list: List<Int> = emptyList()
)
