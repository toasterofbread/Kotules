package dev.toastbits.kotules.sample.extension

import dev.toastbits.kotules.extension.annotation.KotuleAnnotation
import dev.toastbits.kotules.extension.util.isDelayAvailable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlin.coroutines.coroutineContext

@KotuleAnnotation
class SampleKotule {
    val coolProperty: Int = 56

    fun repeatInput(input: String, repeatCount: Int): String =
        input.repeat(repeatCount)

    suspend fun downloadFortune(): String {
        println("downloadFortune: Starting")

        if (!coroutineContext.isDelayAvailable()) {
            println("downloadFortune: Delay is unavailable in current coroutine scope, skipping test")
        }
        else {
            val delayCount: Int = 3
            for (i in 0 until delayCount) {
                println("downloadFortune: Delaying by 1 second (${i + 1} of $delayCount)")
                delay(1000)
            }
        }

        val fortuneText: String = HttpClient().get("https://helloacm.com/api/fortune/").bodyAsText()

        println("downloadFortune: Returning result")
        return fortuneText
    }
}
