package dev.toastbits.kotules.sample.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import dev.toastbits.kotules.sample.extension.SampleKotuleImpl
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test

class SampleKotuleTest {
    private lateinit var referenceKotule: SampleKotule
    private lateinit var loadedKotule: SampleKotule

    private suspend fun setUp() {
        referenceKotule = SampleKotuleImpl()
        loadedKotule = loadSampleKotule()
    }

    private fun runTest(block: suspend TestScope.() -> Unit) {
        kotlinx.coroutines.test.runTest {
            setUp()
            block()
        }
    }

    @Test
    fun intProperty_valueMatches() = runTest {
        assertThat(loadedKotule.intProperty).isEqualTo(referenceKotule.intProperty)
    }

    @Test
    fun suspendInt_valueMatches() = runTest {
        assertThat(loadedKotule.suspendInt()).isEqualTo(referenceKotule.suspendInt())
    }

    @Test
    fun downloadFortune_resultDoesNotMatch() = runTest {
        val fortune: String = loadedKotule.downloadFortune()
        assertThat(fortune).isNotEmpty()
        assertThat(fortune.isBlank()).isFalse()
        assertThat(fortune).isNotEqualTo(referenceKotule.downloadFortune())
    }

    @Test
    fun repeatInput_resultMatches() = runTest {
        for (repeatCount in 0 until 5) {
            val input: String = "Hello "

            assertThat(loadedKotule.repeatInput(input, repeatCount)).isEqualTo(referenceKotule.repeatInput(input, repeatCount))
        }
    }

    @Test
    fun getDataClass_resultMatches() = runTest {
        assertThat(loadedKotule.getDataClass()).isEqualTo(referenceKotule.getDataClass())
    }
}
