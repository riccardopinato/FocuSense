package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.sqrt

object GamificationEngine {
    const val MIN_VALID_SECONDS = 600L

    fun build(
        sessions: List<FocusSessionEntity>,
        profile: PersonalFocusProfile = PersonalFocusProfile.generic(),
        goals: WeeklyGoalSettings = WeeklyGoalSettings(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId)
    ): GamificationProgress {
        val valid = sessions
            .filter {
                it.endedAt != null &&
                    it.actualDurationSeconds >= MIN_VALID_SECONDS &&
                    SessionStatus.fromKey(it.sessionStatusKey) != SessionStatus.INTERRUPTED
            }
            .sortedBy { it.startedAt }

        val completed = valid.filter {
            val status = SessionStatus.fromKey(it.sessionStatusKey)
            status == SessionStatus.COMPLETED || status == SessionStatus.LEGACY
        }

        val validMinutes = valid.sumOf {
            (it.actualDurationSeconds / 60L).toInt()
        }

        val totalXp = valid.sumOf(::xpForSession)
        val level = levelProgress(totalXp)

        val activeDays = completed
            .map { dateOf(it.startedAt, zoneId) }
            .distinct()
            .sorted()

        val bestStreak = calculateBestStreak(activeDays)
        val currentStreak = calculateCurrentStreak(activeDays, today)
        val weekly = weeklyProgress(completed, goals, zoneId, today)

        val cores = completed.filter {
            FocusCoreStage.fromKey(it.coreStageKey) !in setOf(
                FocusCoreStage.NONE,
                FocusCoreStage.FRACTURED
            )
        }

        val unlocked = buildSet {
            if (valid.isNotEmpty()) add(ProgressBadge.FIRST_SESSION)
            if (cores.isNotEmpty()) add(ProgressBadge.FIRST_CORE)
            if (cores.any { FocusCoreStage.fromKey(it.coreStageKey) == FocusCoreStage.PRISM }) {
                add(ProgressBadge.PRISM_CORE)
            }
            if (completed.any { it.screenOffSeconds >= 30 * 60L }) {
                add(ProgressBadge.DEEP_FOCUS)
            }
            if (bestStreak >= 3) add(ProgressBadge.THREE_DAY_STREAK)
            if (bestStreak >= 7) add(ProgressBadge.SEVEN_DAY_STREAK)
            if (valid.size >= 10) add(ProgressBadge.TEN_SESSIONS)
            if (cores.size >= 10) add(ProgressBadge.TEN_CORES)
            if (valid.size >= 50) add(ProgressBadge.FIFTY_SESSIONS)
            if (validMinutes >= 500) add(ProgressBadge.FIVE_HUNDRED_MINUTES)
            if (
                profile.level == CalibrationLevel.PERSONALIZED ||
                profile.level == CalibrationLevel.STRONG
            ) {
                add(ProgressBadge.CALIBRATED)
            }

            val contextCounts = completed
                .groupingBy { SessionContext.fromKey(it.contextKey) }
                .eachCount()
            if (contextCounts.count { it.value >= 2 } >= 4) {
                add(ProgressBadge.CONTEXT_EXPLORER)
            }

            if (weekly.complete) {
                add(ProgressBadge.CONSISTENT_WEEK)
            }
        }

        return GamificationProgress(
            validSessions = valid.size,
            totalValidMinutes = validMinutes,
            totalCores = cores.size,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            level = level,
            weekly = weekly,
            unlockedBadges = unlocked
        )
    }

    fun xpForSession(session: FocusSessionEntity): Int {
        if (session.endedAt == null || session.actualDurationSeconds < MIN_VALID_SECONDS) {
            return 0
        }

        if (session.growthPoints > 0) {
            return session.growthPoints.coerceAtMost(120)
        }

        if (SessionStatus.fromKey(session.sessionStatusKey) == SessionStatus.ABANDONED) {
            return ((session.actualDurationSeconds / 60L) / 5L)
                .toInt()
                .coerceAtMost(12)
        }

        // Legacy sessions keep a deterministic fallback without posture rewards.
        return (session.actualDurationSeconds / 60L)
            .toInt()
            .coerceAtMost(60)
    }

    fun levelProgress(totalXp: Int): LevelProgress {
        val safeXp = totalXp.coerceAtLeast(0)
        val level = floor(sqrt(safeXp / 100.0)).toInt() + 1
        val start = (level - 1) * (level - 1) * 100
        val next = level * level * 100

        return LevelProgress(
            level = level,
            totalXp = safeXp,
            currentLevelStartXp = start,
            nextLevelXp = next
        )
    }

    fun calculateBestStreak(days: List<LocalDate>): Int {
        val sorted = days.distinct().sorted()
        if (sorted.isEmpty()) return 0

        var best = 1
        var current = 1

        for (i in 1 until sorted.size) {
            if (sorted[i - 1].plusDays(1) == sorted[i]) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }

        return best
    }

    fun calculateCurrentStreak(
        days: List<LocalDate>,
        today: LocalDate
    ): Int {
        val set = days.distinct().toSet()
        if (set.isEmpty()) return 0

        val start = when {
            today in set -> today
            today.minusDays(1) in set -> today.minusDays(1)
            else -> return 0
        }

        var count = 0
        var cursor = start

        while (cursor in set) {
            count++
            cursor = cursor.minusDays(1)
        }

        return count
    }

    fun weeklyProgress(
        valid: List<FocusSessionEntity>,
        goals: WeeklyGoalSettings,
        zoneId: ZoneId,
        today: LocalDate
    ): WeeklyProgress {
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        val nextMonday = monday.plusWeeks(1)

        val weeklySessions = valid.filter {
            val date = dateOf(it.startedAt, zoneId)
            !date.isBefore(monday) && date.isBefore(nextMonday)
        }

        return WeeklyProgress(
            validSessions = weeklySessions.size,
            validMinutes = weeklySessions.sumOf {
                (it.actualDurationSeconds / 60L).toInt()
            },
            sessionGoal = goals.sessionGoal.coerceIn(1, 50),
            minuteGoal = goals.minuteGoal.coerceIn(10, 5000)
        )
    }

    private fun dateOf(
        epochMillis: Long,
        zoneId: ZoneId
    ): LocalDate =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()
}
