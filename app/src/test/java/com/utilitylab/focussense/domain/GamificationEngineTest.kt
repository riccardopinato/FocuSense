package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GamificationEngineTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun session(
        id: Long,
        date: LocalDate,
        minutes: Int,
        status: SessionStatus = SessionStatus.COMPLETED,
        growth: Int = 0,
        core: FocusCoreStage = FocusCoreStage.SPARK,
        integrity: Int = 90,
        screenOffSeconds: Long = 0,
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
            contextKey = context.key,
            sessionStatusKey = status.name,
            growthPoints = growth,
            coreStageKey = core.name,
            integrityScore = integrity,
            screenOffSeconds = screenOffSeconds
        )
    }

    @Test
    fun shortSessionGivesNoXp() {
        assertEquals(
            0,
            GamificationEngine.xpForSession(
                session(1, LocalDate.of(2026, 9, 19), 9, growth = 50)
            )
        )
    }

    @Test
    fun newSessionXpUsesGrowthPoints() {
        val s = session(
            1,
            LocalDate.of(2026, 9, 19),
            50,
            growth = 47
        )
        assertEquals(47, GamificationEngine.xpForSession(s))
    }

    @Test
    fun abandonedSessionGetsOnlySmallFallbackWithoutGrowth() {
        val s = session(
            1,
            LocalDate.of(2026, 9, 19),
            50,
            status = SessionStatus.ABANDONED,
            growth = 0,
            core = FocusCoreStage.FRACTURED
        )
        assertEquals(10, GamificationEngine.xpForSession(s))
    }

    @Test
    fun completedCoresDriveSanctuaryCountAndBadges() {
        val today = LocalDate.of(2026, 9, 19)
        val sessions = (1L..10L).map { id ->
            session(
                id = id,
                date = today.minusDays((id % 3).toLong()),
                minutes = 30,
                growth = 30,
                core = FocusCoreStage.ORB
            )
        }

        val result = GamificationEngine.build(
            sessions = sessions,
            zoneId = zone,
            today = today
        )

        assertEquals(10, result.totalCores)
        assertTrue(ProgressBadge.FIRST_CORE in result.unlockedBadges)
        assertTrue(ProgressBadge.TEN_CORES in result.unlockedBadges)
    }

    @Test
    fun prismAndDeepFocusUnlockDedicatedBadges() {
        val today = LocalDate.of(2026, 9, 19)
        val result = GamificationEngine.build(
            sessions = listOf(
                session(
                    1,
                    today,
                    50,
                    growth = 60,
                    core = FocusCoreStage.PRISM,
                    integrity = 99,
                    screenOffSeconds = 31 * 60L
                )
            ),
            zoneId = zone,
            today = today
        )

        assertTrue(ProgressBadge.PRISM_CORE in result.unlockedBadges)
        assertTrue(ProgressBadge.DEEP_FOCUS in result.unlockedBadges)
    }

    @Test
    fun interruptedSessionDoesNotCountForProgress() {
        val today = LocalDate.of(2026, 9, 19)
        val result = GamificationEngine.build(
            sessions = listOf(
                session(
                    1,
                    today,
                    30,
                    status = SessionStatus.INTERRUPTED,
                    growth = 20,
                    core = FocusCoreStage.FRACTURED
                )
            ),
            zoneId = zone,
            today = today
        )

        assertEquals(0, result.validSessions)
        assertEquals(0, result.totalCores)
    }
}
