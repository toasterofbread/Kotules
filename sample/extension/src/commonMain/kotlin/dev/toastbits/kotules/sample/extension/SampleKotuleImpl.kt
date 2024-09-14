package dev.toastbits.kotules.sample.extension

import dev.toastbits.kotules.extension.annotation.KotuleDefinition
import dev.toastbits.kotules.extension.util.isDelayAvailable
import dev.toastbits.kotules.sample.app.SampleDataClass
import dev.toastbits.kotules.sample.app.SampleInputInterface
import dev.toastbits.kotules.sample.app.SampleKotule
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlin.coroutines.coroutineContext

@KotuleDefinition
@Suppress("JS_NAME_CLASH") // TODO
class SampleKotuleImpl: SampleKotule {
    override val intProperty: Int = 56

    override fun repeatInput(input: String, repeatCount: Int): String =
        input.repeat(repeatCount)

    override suspend fun suspendInt(): Int = 64

    override suspend fun downloadFortune(): String {
        println("downloadFortune: Starting")

        if (!coroutineContext.isDelayAvailable()) {
            println("downloadFortune: Delay is unavailable in current coroutine scope, skipping test")
        }
        else {
            val delayCount: Int = 5
            for (i in 0 until delayCount) {
                println("downloadFortune: Delaying by 1 second (${i + 1} of $delayCount)")
                delay(1000)
            }
        }

        val fortuneText: String = HttpClient().get("https://helloacm.com/api/fortune/").bodyAsText()

        println("downloadFortune: Returning result")
        return fortuneText
    }

    override suspend fun getDataClass(): SampleDataClass =
        SampleDataClass(
            "SampleDataClass string value",
            SampleDataClass(
                "string 2",
                SampleDataClass(
                    "string 3",
                    SampleDataClass(
                        "string 4",
                        SampleDataClass(
                            "string 5",
                            null
                        )
                    )
                )
            )
        )

//    override fun inputTest(input: SampleInputInterface): String =
//        (input.getText() + " ").repeat(5)
}
