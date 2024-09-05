package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.extension.KotulePromise
import dev.toastbits.kotules.extension.type.OutStringValue
import dev.toastbits.kotules.extension.type.StringValue
import dev.toastbits.kotules.runtime.KotuleLoader
import dev.toastbits.kotules.sample.extension.SampleKotule_InputBinding
import dev.toastbits.kotules.sample.extension.SampleKotule
import dev.toastbits.kotules.sample.extension.SampleKotule_OutputBinding

actual suspend fun loadSampleKotule(): SampleKotule_InputBinding {
    println("Loading extension jar at ${SampleConfig.JVM_EXTENSION_FILE}")
    val kotuleImpl: SampleKotule_OutputBinding = KotuleLoader.loadFromJar(SampleConfig.JVM_EXTENSION_FILE, SampleConfig.EXTENSION_IMPL_CLASS)

    return SampleKotuleMapper(kotuleImpl)
}

private class SampleKotuleMapper(private val implementation: SampleKotule_OutputBinding): SampleKotule_InputBinding {
    override fun repeatInput(input: String, repeatCount: Int): String = implementation.repeatInput(input, repeatCount)

    override fun downloadFortune(): KotulePromise<StringValue> = KotulePromise {
        StringValue((implementation.downloadFortune().action() as OutStringValue).value)
    }

    override val coolProperty: Int
        get() = implementation.coolProperty
}
