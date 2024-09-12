package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.core.Kotule
import dev.toastbits.kotules.runtime.annotation.KotuleDeclaration

@KotuleDeclaration
interface SampleKotule: Kotule {
    val intProperty: Int
    fun repeatInput(input: String, repeatCount: Int): String

    fun getImpl(): LogEntity

//    suspend fun getList(): List<SampleDataClass<LogEntity>> =
//        listOf(
//            SampleDataClass("name one") { PropertyContent.Text("Ello") },
//            SampleDataClass("name two") { PropertyContent.Text("Ello") },
//            SampleDataClass("name three") { PropertyContent.Text("Ello") }
//        )

    suspend fun suspendInt(): Int

    suspend fun downloadFortune(): String

//    suspend fun getDataClass(): SampleDataClass<LogEntity>
}
//
//data class SampleDataClass<T: LogEntity>(
//    val name: String,
//    val accessor: T.() -> PropertyContent
//)
//
//sealed interface PropertyContent {
//    data class Text(val text: String): PropertyContent
//}

interface LogEntity {
    fun doSomething(): String
}
