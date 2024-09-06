package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.sample.extension.SampleKotule_InputBinding
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SampleKotuleTest {
    @Test
    fun test() = runTest {
        val kotule: SampleKotule_InputBinding = loadSampleKotule()
        println(kotule)
    }
}
