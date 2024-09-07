package dev.toastbits.kotules.sample.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import dev.toastbits.kotules.sample.extension.SampleKotuleImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SampleKotuleTest {
    private lateinit var referenceKotule: SampleKotule
    private lateinit var loadedKotule: SampleKotule

    @BeforeTest
    fun setUp() = runTest {
        referenceKotule = SampleKotuleImpl()
        loadedKotule = loadSampleKotule()
    }

    @Test
    fun coolProperty_valueMatches() = runTest {
        assertThat(loadedKotule.coolProperty).isEqualTo(referenceKotule.coolProperty)
    }

    @Test
    fun repeatInput_resultMatches() = runTest {
        for (repeatCount in 0 until 5) {
            val input: String = "Hello "

            assertThat(loadedKotule.repeatInput(input, repeatCount)).isEqualTo(referenceKotule.repeatInput(input, repeatCount))
        }
    }

    @Test
    fun downloadFortune_resultDoesNotMatch() = runTest {
        val fortune: String = loadedKotule.downloadFortune()
        assertThat(fortune).isNotEmpty()
        assertThat(fortune.isBlank()).isFalse()
        assertThat(fortune).isNotEqualTo(referenceKotule.downloadFortune())
    }
}
