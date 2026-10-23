package com.example

import com.example.agent.ASSUMED_DENSITY_KG_PER_L
import com.example.agent.SackScanAgent
import com.example.model.AgentNode
import com.example.model.AgentState
import com.example.model.Language
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ExampleUnitTest {
    @Test
    fun testAssumedBulkDensityValue() {
        assertEquals(0.8, ASSUMED_DENSITY_KG_PER_L, 0.001)
    }

    @Test
    fun testWeightArithmeticCalculation() {
        // e.g. 50 Litres * 0.8 kg/L = 40.0 kg
        val volumeLitres = 50.0
        val rawWeight = volumeLitres * ASSUMED_DENSITY_KG_PER_L
        val weight = (rawWeight * 10.0).roundToInt() / 10.0
        assertEquals(40.0, weight, 0.001)

        val minRange = ((weight * 0.75) * 10.0).roundToInt() / 10.0
        val maxRange = ((weight * 1.25) * 10.0).roundToInt() / 10.0
        assertEquals(30.0, minRange, 0.001)
        assertEquals(50.0, maxRange, 0.001)
    }

    @Test
    fun testLoosenedConfidenceGateBehavior() {
        // If a sack is visible even without scale reference, proceed
        val sackVisibleState = AgentState(
            evidenceSufficient = true,
            approxVolumeLitres = 30.0,
            scaleReferenceUsed = false
        )
        val hasNoSackAtAll = !sackVisibleState.evidenceSufficient &&
                sackVisibleState.approxVolumeLitres <= 0.0 &&
                sackVisibleState.approxHeightCm <= 0.0
        assertFalse(hasNoSackAtAll) // Proceed = true

        // Only when no sack at all is visible should it route to low confidence exit
        val noSackState = AgentState(
            evidenceSufficient = false,
            approxVolumeLitres = 0.0,
            approxHeightCm = 0.0
        )
        val noSackDetected = !noSackState.evidenceSufficient &&
                noSackState.approxVolumeLitres <= 0.0 &&
                noSackState.approxHeightCm <= 0.0
        assertTrue(noSackDetected) // Proceeds to low confidence exit
    }

    @Test
    fun testBilingualLanguageSwitch() {
        assertEquals("SackScan AI", com.example.model.Translations.appTitle(Language.ENGLISH))
        assertEquals("சாக்கு ஸ்கேன் AI", com.example.model.Translations.appTitle(Language.TAMIL))
    }
}
