package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TemporalInsightsTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun session(
        year: Int = 2026,
        month: Int = 9,
        day: Int,
        hour: Int,
        score: Int,
        rating: Int? = null
    ): FocusSessionEntity {
        val time = LocalDateTime.of(year, month, day, hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        return FocusSessionEntity(
            id = (day * 100 + hour).toLong(),
            startedAt = time,
            endedAt = time + 30 * 60_000L,
            plannedDurationMinutes = 30,
            actualDurationSeconds = 1800,
            focusScore = score,
            selfFocusRating = rating
        )
    }

    @Test
    fun emptyDataProducesNoFakeScores() {
        val result = TemporalInsightsEngine.build(emptyList(), zone)
        assertTrue(result.byDayPart.isEmpty())
        assertEquals(28, result.heatmap.size)
        assertTrue(result.heatmap.all { it.sessions == 0 && it.averageFocusScore == null })
    }

    @Test
    fun groupsSessionsByDayPart() {
        val data = listOf(
            session(day = 14, hour = 8, score = 80),
            session(day = 15, hour = 9, score = 70),
            session(day = 16, hour = 14, score = 60)
        )
        val result = TemporalInsightsEngine.build(data, zone)

        assertEquals(2, result.byDayPart[DayPart.MORNING]?.sessions)
        assertEquals(75, result.byDayPart[DayPart.MORNING]?.averageFocusScore)
        assertEquals(1, result.byDayPart[DayPart.AFTERNOON]?.sessions)
    }

    @Test
    fun insightRequiresThreeSessionsInTimeBand() {
        val data = listOf(
            session(day = 14, hour = 8, score = 90),
            session(day = 15, hour = 8, score = 90)
        )
        val result = TemporalInsightsEngine.build(data, zone)
        assertTrue(result.messages.none { it.contains("mattina", ignoreCase = true) && it.contains("Session Score medio") })
    }

    @Test
    fun recentTrendRequiresFourteenCompletedSessions() {
        val less = (1..13).map { i ->
            session(day = i.coerceAtMost(19), hour = 8, score = 60 + i)
        }
        assertNull(TemporalInsightsEngine.build(less, zone).recentTrend)

        val fourteen = (1..14).map { i ->
            session(day = i, hour = 8, score = if (i <= 7) 60 else 80)
        }
        assertNotNull(TemporalInsightsEngine.build(fourteen, zone).recentTrend)
    }
}
