package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BestConditionsEngineTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun session(
        id: Long,
        rating: Int?,
        hour: Int = 9,
        context: SessionContext = SessionContext.WORK,
        noise: Float? = -40f,
        light: Float? = 500f,
        motion: Float? = 10f,
        legacyScore: Int = 70
    ): FocusSessionEntity {
        val start = LocalDateTime.of(
            2026,
            9,
            id.toInt().coerceIn(1, 20),
            hour,
            0
        ).atZone(zone).toInstant().toEpochMilli()

        return FocusSessionEntity(
            id = id,
            startedAt = start,
            endedAt = start + 20 * 60_000L,
            plannedDurationMinutes = 20,
            actualDurationSeconds = 1200,
            focusScore = legacyScore,
            selfFocusRating = rating,
            avgNoiseDbfs = noise,
            avgLightLux = light,
            avgMotionIndex = motion,
            stablePercent = 90f,
            contextKey = context.key
        )
    }

    @Test
    fun unratedSessionsDoNotMakeProfileReady() {
        val result = BestConditionsEngine.build(
            (1L..8L).map { session(it, rating = null) },
            zone
        )

        assertFalse(result.ready)
        assertEquals(0, result.analyzedSessions)
    }

    @Test
    fun fiveRatedSessionsCreateProfile() {
        val result = BestConditionsEngine.build(
            (1L..5L).map { session(it, rating = 4) },
            zone
        )

        assertTrue(result.ready)
        assertEquals(5, result.analyzedSessions)
        assertTrue(result.sampleSize >= 3)
    }

    @Test
    fun highSelfRatingWinsOverAutomaticLegacyScore() {
        val data = listOf(
            session(1, rating = 5, context = SessionContext.STUDY, legacyScore = 30),
            session(2, rating = 5, context = SessionContext.STUDY, legacyScore = 35),
            session(3, rating = 4, context = SessionContext.STUDY, legacyScore = 40),
            session(4, rating = 2, context = SessionContext.WORK, legacyScore = 99),
            session(5, rating = 2, context = SessionContext.WORK, legacyScore = 98),
            session(6, rating = 1, context = SessionContext.WORK, legacyScore = 97)
        )

        val result = BestConditionsEngine.build(data, zone)

        assertTrue(result.ready)
        assertEquals(SessionContext.STUDY, result.strongestContext)
    }

    @Test
    fun missingNoiseDoesNotCreateNoiseRange() {
        val data = (1L..6L).map {
            session(id = it, rating = 4, noise = null)
        }

        val result = BestConditionsEngine.build(data, zone)
        assertNull(result.preferredNoiseRangeDbfs)
    }

    @Test
    fun environmentMatchIgnoresMotion() {
        val profile = BestConditionsEngine.build(
            (1L..6L).map { session(it, rating = 5) },
            zone
        )

        val match = BestConditionsEngine.environmentMatch(
            profile = profile,
            live = LiveSensorSnapshot(
                noiseDbfs = -40f,
                lightLux = 500f,
                motionIndex = 100f
            ),
            availability = SensorAvailability(true, true, true, true)
        )

        assertTrue((match.score ?: 0) >= 95)
        assertNull(match.motionScore)
    }

    @Test
    fun percentileWorks() {
        val p = BestConditionsEngine.percentile(
            listOf(10f, 20f, 30f, 40f),
            .50f
        )
        assertEquals(25f, p ?: 0f, .001f)
    }
}
