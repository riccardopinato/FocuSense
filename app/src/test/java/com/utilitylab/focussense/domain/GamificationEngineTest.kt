package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GamificationEngineTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun session(
        id: Long,
        date: LocalDate,
        minutes: Int,
        focusRating: Int? = null,
        postureRating: Int? = null,
        context: SessionContext = SessionContext.WORK
    ): FocusSessionEntity {
        val start = date.atTime(10, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        return FocusSessionEntity(
            id = id,
            startedAt = start,
            endedAt = start + minutes * 60_000L,
            plannedDurationMinutes = minutes,
            actualDurationSeconds = minutes * 60L,
            selfFocusRating = focusRating,
            postureRating = postureRating,
            contextKey = context.key
        )
    }

    @Test
    fun shortSessionGivesNoXp() {
        val s = session(
            1,
            LocalDate.of(2026, 9, 19),
            9
        )
        assertEquals(0, GamificationEngine.xpForSession(s))
    }

    @Test
    fun xpIsCapped() {
        val s = session(
            1,
            LocalDate.of(2026, 9, 19),
            120,
            focusRating = 5,
            postureRating = 5
        )
        assertEquals(68, GamificationEngine.xpForSession(s))
    }

    @Test
    fun multipleSessionsSameDayCountOneStreakDay() {
        val day = LocalDate.of(2026, 9, 19)
        val sessions = listOf(
            session(1, day, 20),
            session(2, day, 30),
            session(3, day.minusDays(1), 20)
        )

        val result = GamificationEngine.build(
            sessions = sessions,
            zoneId = zone,
            today = day
        )

        assertEquals(2, result.currentStreak)
    }

    @Test
    fun yesterdayKeepsCurrentStreakAlive() {
        val today = LocalDate.of(2026, 9, 19)
        val days = listOf(
            today.minusDays(1),
            today.minusDays(2),
            today.minusDays(3)
        )

        assertEquals(
            3,
            GamificationEngine.calculateCurrentStreak(days, today)
        )
    }

    @Test
    fun gapEndsCurrentStreak() {
        val today = LocalDate.of(2026, 9, 19)
        val days = listOf(
            today.minusDays(2),
            today.minusDays(3)
        )

        assertEquals(
            0,
            GamificationEngine.calculateCurrentStreak(days, today)
        )
    }

    @Test
    fun weeklyGoalsUseOnlyCurrentWeek() {
        val today = LocalDate.of(2026, 9, 19)
        val sessions = listOf(
            session(1, today, 30),
            session(2, today.minusDays(1), 30),
            session(3, today.minusDays(10), 100)
        )

        val result = GamificationEngine.build(
            sessions = sessions,
            goals = WeeklyGoalSettings(2, 60),
            zoneId = zone,
            today = today
        )

        assertEquals(2, result.weekly.validSessions)
        assertEquals(60, result.weekly.validMinutes)
        assertTrue(result.weekly.complete)
    }

    @Test
    fun contextExplorerRequiresFourContextsWithTwoSessionsEach() {
        val today = LocalDate.of(2026, 9, 19)
        val contexts = listOf(
            SessionContext.WORK,
            SessionContext.STUDY,
            SessionContext.READING,
            SessionContext.CREATIVE
        )

        val sessions = buildList {
            var id = 1L
            contexts.forEach { context ->
                add(session(id++, today, 20, context = context))
                add(session(id++, today.minusDays(1), 20, context = context))
            }
        }

        val result = GamificationEngine.build(
            sessions = sessions,
            zoneId = zone,
            today = today
        )

        assertTrue(
            ProgressBadge.CONTEXT_EXPLORER in result.unlockedBadges
        )
    }
}
