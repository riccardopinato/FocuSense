package com.utilitylab.focussense.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusIntegrityEngineTest {
    @Test
    fun cleanStandardSessionKeepsFullIntegrity() {
        assertEquals(
            100,
            FocusIntegrityEngine.calculate(
                elapsedSeconds = 1800,
                unstableSeconds = 60,
                pickupCount = 0,
                foregroundBreakCount = 0,
                backgroundSeconds = 0,
                strictness = FocusStrictness.STANDARD
            )
        )
    }

    @Test
    fun excessiveMovementReducesIntegrity() {
        val score = FocusIntegrityEngine.calculate(
            elapsedSeconds = 1000,
            unstableSeconds = 250,
            pickupCount = 0,
            foregroundBreakCount = 0,
            backgroundSeconds = 0,
            strictness = FocusStrictness.STANDARD
        )
        assertTrue(score < 100)
    }

    @Test
    fun pickupsAndAppBreaksReduceIntegrity() {
        val score = FocusIntegrityEngine.calculate(
            elapsedSeconds = 1800,
            unstableSeconds = 0,
            pickupCount = 2,
            foregroundBreakCount = 2,
            backgroundSeconds = 30,
            strictness = FocusStrictness.STANDARD
        )
        assertTrue(score <= 70)
    }

    @Test
    fun strictModePenalizesSameBehaviorMoreThanRelaxed() {
        val relaxed = FocusIntegrityEngine.calculate(
            1200, 120, 1, 1, 10, FocusStrictness.RELAXED
        )
        val strict = FocusIntegrityEngine.calculate(
            1200, 120, 1, 1, 10, FocusStrictness.STRICT
        )
        assertTrue(strict < relaxed)
    }

    @Test
    fun screenOffTimeOnlyCreatesPositiveGrowthBonus() {
        val noScreenOff = FocusIntegrityEngine.growthPoints(
            elapsedSeconds = 3000,
            plannedSeconds = 3000,
            integrityScore = 95,
            screenOffSeconds = 0,
            status = SessionStatus.COMPLETED
        )
        val deepFocus = FocusIntegrityEngine.growthPoints(
            elapsedSeconds = 3000,
            plannedSeconds = 3000,
            integrityScore = 95,
            screenOffSeconds = 2400,
            status = SessionStatus.COMPLETED
        )

        assertTrue(deepFocus > noScreenOff)
    }

    @Test
    fun abandonedSessionCreatesFracturedCore() {
        assertEquals(
            FocusCoreStage.FRACTURED,
            FocusIntegrityEngine.coreStage(
                growthPoints = 10,
                integrityScore = 80,
                status = SessionStatus.ABANDONED
            )
        )
    }

    @Test
    fun nearPerfectCompletedSessionCanCreatePrism() {
        assertEquals(
            FocusCoreStage.PRISM,
            FocusIntegrityEngine.coreStage(
                growthPoints = 40,
                integrityScore = 98,
                status = SessionStatus.COMPLETED
            )
        )
    }
}
