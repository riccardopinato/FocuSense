package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import org.junit.Assert.*
import org.junit.Test

class ContextAnalyticsTest {
    private fun session(id: Long, context: SessionContext, score: Int) =
        FocusSessionEntity(
            id = id,
            startedAt = 1_000_000L + id,
            endedAt = 2_000_000L + id,
            plannedDurationMinutes = 25,
            actualDurationSeconds = 1500,
            focusScore = score,
            contextKey = context.key
        )

    @Test
    fun rawBucketWorksWithFewSessions() {
        val result = ContextAnalyticsEngine.build(
            listOf(
                session(1, SessionContext.STUDY, 80),
                session(2, SessionContext.STUDY, 60)
            )
        )
        val study = result.buckets.first { it.context == SessionContext.STUDY }
        assertEquals(2, study.sessions)
        assertEquals(70, study.averageFocusScore)
    }

    @Test
    fun insightRequiresThreeSessions() {
        val result = ContextAnalyticsEngine.build(
            listOf(
                session(1, SessionContext.STUDY, 80),
                session(2, SessionContext.STUDY, 70)
            )
        )
        assertFalse(
            result.messages.any {
                it.contains("studio", true) && it.contains("Session Score medio")
            }
        )
    }

    @Test
    fun trendRequiresSixSessions() {
        val five = (1L..5L).map { session(it, SessionContext.WORK, 70) }
        assertTrue(ContextAnalyticsEngine.build(five).trends.isEmpty())

        val six = (1L..6L).map {
            session(it, SessionContext.WORK, if (it <= 3) 60 else 80)
        }
        assertEquals(1, ContextAnalyticsEngine.build(six).trends.size)
    }
}
