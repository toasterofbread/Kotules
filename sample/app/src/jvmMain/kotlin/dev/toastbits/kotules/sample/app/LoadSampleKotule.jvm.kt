package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.type.OutStringValue
import dev.toastbits.kotules.extension.type.StringValue
import dev.toastbits.kotules.runtime.KotuleLoader
import dev.toastbits.kotules.sample.extension.SampleKotule
import dev.toastbits.kotules.sample.extension.SampleKotuleImpl

actual suspend fun loadSampleKotule(): SampleKotule {
    println("Loading extension jar at ${SampleConfig.JVM_EXTENSION_FILE}")
    val kotuleImpl: SampleKotuleImpl = KotuleLoader.loadFromJar(SampleConfig.JVM_EXTENSION_FILE, SampleConfig.EXTENSION_IMPL_CLASS)

    return SampleKotuleMapper(kotuleImpl)
}

private class SampleKotuleMapper(private val implementation: SampleKotuleImpl): SampleKotule {
    override fun repeatInput(input: String, repeatCount: Int): String = implementation.repeatInput(input, repeatCount)

    override fun downloadFortune(): KotulePromise<StringValue> = KotulePromise {
        StringValue((implementation.downloadFortune().action() as OutStringValue).value)
    }
}
