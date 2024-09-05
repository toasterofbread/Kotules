package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.await
import dev.toastbits.kotules.extension.type.StringValue
import dev.toastbits.kotules.sample.extension.SampleKotule
import kotlinx.coroutines.delay

suspend fun testSampleKotule() {
    val kotule: SampleKotule = loadSampleKotule()
    println("Loaded SampleKotule: $kotule")

    println("Calling repeatInput(\"Hello \", 5) on SampleKotule")
    val repeatResult: String = kotule.repeatInput("Hello", 5)
    println("Got result from repeatInput: $repeatResult")

    println("Calling downloadFortune() on SampleKotule")
    val promise: KotulePromise<StringValue> = kotule.downloadFortune()

    println("Waiting for 2 seconds before awaiting promise...")
    delay(2000)

    println("Awaiting result from promise...")
    val fortuneResult: StringValue = promise.await()
    println("Got result from downloadFortune: ${fortuneResult.value}")
}
