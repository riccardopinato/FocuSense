package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BestConditionsEngineTest {

    private val zone = ZoneId.of("Europe/Rome")

    private fun session(
        id: Long,
        score: Int,
        hour: Int = 9,
        context: SessionContext = SessionContext.WORK,
        rating: Int? = null,
        noise: Float? = -40f,
        light: Float? = 500f,
        motion: Float? = 10f
    ): FocusSessionEntity {
        val start = LocalDateTime.of(
            2026, 9, (id.toInt().coerceIn(1, 20)), hour, 0
        ).atZone(zone).toInstant().toEpochMilli()

        return FocusSessionEntity(
            id = id,
            startedAt = start,
            endedAt = start + 20 * 60_000L,
            plannedDurationMinutes = 20,
            actualDurationSeconds = 1200,
            focusScore = score,
            selfFocusRating = rating,
            avgNoiseDbfs = noise,
            avgLightLux = light,
            avgMotionIndex = motion,
            stablePercent = 90f,
            interruptionCount = 1,
            contextKey = context.key
        )
    }

    @Test
    fun lessThanFiveSessionsIsNotReady() {
        val result = BestConditionsEngine.build(
            (1L..4L).map { session(it, 70) },
            zone
        )
        assertFalse(result.ready)
        assertEquals(4, result.analyzedSessions)
    }

    @Test
    fun fiveSessionsCreateProfile() {
        val result = BestConditionsEngine.build(
            (1L..5L).map { session(it, 60 + it.toInt()) },
            zone
        )
        assertTrue(result.ready)
        assertTrue(result.sampleSize >= 3)
    }

    @Test
    fun highlyRatedSubsetIsPreferredWhenEnough() {
        val data = (1L..8L).map {
            session(
                id = it,
                score = 90 - it.toInt(),
                rating = if (it <= 3) 5 else 2
            )
        }

        val result = BestConditionsEngine.build(data, zone)
        assertTrue(result.ready)
        assertTrue(result.sampleSize >= 3)
    }

    @Test
    fun missingNoiseDoesNotCreateNoiseRange() {
        val data = (1L..6L).map {
            session(
                id = it,
                score = 80,
                noise = null
            )
        }

        val result = BestConditionsEngine.build(data, zone)
        assertNull(result.preferredNoiseRangeDbfs)
    }

    @Test
    fun rangeScoreIsPerfectInsideRange() {
        assertEquals(
            100,
            BestConditionsEngine.rangeScore(
                value = -40f,
                min = -50f,
                max = -30f,
                decay = 20f
            )
        )
    }

    @Test
    fun upperBoundFallsLinearly() {
        assertEquals(
            50,
            BestConditionsEngine.upperBoundScore(
                value = 35f,
                max = 10f,
                decay = 50f
            )
        )
    }

    @Test
    fun strongestContextIsDetected() {
        val data = listOf(
            session(1, 95, context = SessionContext.STUDY),
            session(2, 94, context = SessionContext.STUDY),
            session(3, 93, context = SessionContext.STUDY),
            session(4, 60, context = SessionContext.WORK),
            session(5, 59, context = SessionContext.WORK),
            session(6, 58, context = SessionContext.WORK),
            session(7, 57, context = SessionContext.WORK),
            session(8, 56, context = SessionContext.WORK)
        )

        val result = BestConditionsEngine.build(data, zone)
        assertEquals(SessionContext.STUDY, result.strongestContext)
    }

    @Test
    fun percentileWorks() {
        val p = BestConditionsEngine.percentile(
            listOf(10f, 20f, 30f, 40f),
            .50f
        )
        assertEquals(25f, p ?: 0f, 0.001f)
    }
}
