package com.utilitylab.focussense

import com.utilitylab.focussense.domain.FocusScoreEngine
import com.utilitylab.focussense.domain.FocusTargets
import com.utilitylab.focussense.domain.FocusWeights
import com.utilitylab.focussense.domain.SensorAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusScoreEngineTest {
    @Test
    fun perfectEnvironmentIsHigh() {
        val score = FocusScoreEngine.liveScore(
            noiseDbfs = -65f,
            lightLux = 500f,
            motionIndex = 95f,
            availability = SensorAvailability(true, true, true, true)
        )
        assertTrue(score >= 95)
    }

    @Test
    fun unavailableLightRenormalizesToNoise() {
        val breakdown = FocusScoreEngine.liveBreakdown(
            noiseDbfs = -65f,
            lightLux = 0f,
            motionIndex = 90f,
            availability = SensorAvailability(true, false, true, false),
            weights = FocusWeights(.6f, .3f, .1f)
        )

        assertTrue(breakdown.total >= 95)
        assertEquals(1f, breakdown.effectiveWeights.noise, .0001f)
        assertEquals(0f, breakdown.effectiveWeights.light, .0001f)
        assertEquals(0f, breakdown.effectiveWeights.motion, .0001f)
    }

    @Test
    fun movementDoesNotChangeEnvironmentScore() {
        val targets = FocusTargets(stableMotionIndex = 10f)
        val stable = FocusScoreEngine.liveScore(-65f, 500f, 2f, targets = targets)
        val moving = FocusScoreEngine.liveScore(-65f, 500f, 95f, targets = targets)

        assertEquals(stable, moving)
    }

    @Test
    fun combinedScoreWeightsIntegrityMoreThanEnvironment() {
        assertEquals(80, FocusScoreEngine.combinedSessionScore(50, 100))
        assertEquals(70, FocusScoreEngine.combinedSessionScore(100, 50))
    }

    @Test
    fun noUsableEnvironmentSensorsReturnsZero() {
        assertEquals(
            0,
            FocusScoreEngine.liveScore(
                -50f,
                500f,
                10f,
                availability = SensorAvailability(false, false, true, true)
            )
        )
    }
}
