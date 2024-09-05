package dev.toastbits.kotules.sample.extension

import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.OutKotulePromise
import dev.toastbits.kotules.extension.PlatformJsExport
import dev.toastbits.kotules.extension.kotulePromise
import dev.toastbits.kotules.extension.type.StringValue
import dev.toastbits.kotules.extension.type.OutStringValue
import dev.toastbits.kotules.extension.util.isDelayAvailable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlin.coroutines.coroutineContext

@PlatformJsExport
class SampleKotuleImpl {
    fun repeatInput(input: String, repeatCount: Int): String =
        input.repeat(repeatCount)

    fun downloadFortune(): OutKotulePromise = kotulePromise {
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
        return@kotulePromise OutStringValue(fortuneText)
    }
}

expect interface SampleKotule {
    fun repeatInput(input: String, repeatCount: Int): String
    fun downloadFortune(): KotulePromise<StringValue>
}
