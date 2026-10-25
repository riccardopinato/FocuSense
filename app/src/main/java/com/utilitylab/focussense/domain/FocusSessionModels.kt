package com.utilitylab.focussense.domain

enum class FocusStrictness(
    val label: String,
    val allowedUnstablePercent: Float,
    val motionTrigger: Float,
    val pickupPenalty: Int,
    val breakPenalty: Int
) {
    RELAXED("Relaxed", 15f, 34f, 3, 6),
    STANDARD("Standard", 8f, 28f, 5, 9),
    STRICT("Strict", 3f, 22f, 7, 12);

    companion object {
        fun fromKey(value: String?): FocusStrictness =
            entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    ABANDONED,
    INTERRUPTED,
    LEGACY;

    companion object {
        fun fromKey(value: String?): SessionStatus =
            entries.firstOrNull { it.name == value } ?: LEGACY
    }
}

enum class FocusCoreStage(val label: String) {
    NONE("Nessun Core"),
    FRACTURED("Core spezzato"),
    SPARK("Spark"),
    ORB("Orb"),
    CRYSTAL("Crystal"),
    NOVA("Nova"),
    PRISM("Prism");

    companion object {
        fun fromKey(value: String?): FocusCoreStage =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

data class FocusRuntimeSnapshot(
    val elapsedSeconds: Long = 0L,
    val plannedSeconds: Long = 0L,
    val integrityScore: Int = 100,
    val unstableSeconds: Long = 0L,
    val unstablePercent: Float = 0f,
    val pickupCount: Int = 0,
    val foregroundBreakCount: Int = 0,
    val backgroundSeconds: Long = 0L,
    val screenOffSeconds: Long = 0L,
    val strictness: FocusStrictness = FocusStrictness.STANDARD,
    val coreStage: FocusCoreStage = FocusCoreStage.SPARK
) {
    val progress: Float
        get() = if (plannedSeconds <= 0L) 0f
        else (elapsedSeconds.toFloat() / plannedSeconds).coerceIn(0f, 1f)

    val remainingFraction: Float
        get() = 1f - progress

    val remainingSeconds: Long
        get() = (plannedSeconds - elapsedSeconds).coerceAtLeast(0L)
}

object FocusIntegrityEngine {
    fun calculate(
        elapsedSeconds: Long,
        unstableSeconds: Long,
        pickupCount: Int,
        foregroundBreakCount: Int,
        backgroundSeconds: Long,
        strictness: FocusStrictness
    ): Int {
        if (elapsedSeconds <= 0L) return 100

        val unstablePercent = unstableSeconds * 100f / elapsedSeconds
        val excessMovement = (unstablePercent - strictness.allowedUnstablePercent)
            .coerceAtLeast(0f)

        val movementPenalty = (excessMovement * 2.1f)
            .toInt()
            .coerceAtMost(45)
        val pickupPenalty = (pickupCount * strictness.pickupPenalty)
            .coerceAtMost(24)
        val breakPenalty = (foregroundBreakCount * strictness.breakPenalty)
            .coerceAtMost(36)
        val backgroundPenalty = (backgroundSeconds / 15L)
            .toInt()
            .coerceAtMost(20)

        return (100 - movementPenalty - pickupPenalty - breakPenalty - backgroundPenalty)
            .coerceIn(0, 100)
    }

    fun growthPoints(
        elapsedSeconds: Long,
        plannedSeconds: Long,
        integrityScore: Int,
        screenOffSeconds: Long,
        status: SessionStatus
    ): Int {
        val completedMinutes = (elapsedSeconds / 60L).toInt().coerceAtLeast(0)
        if (completedMinutes == 0) return 0

        val completion = if (plannedSeconds <= 0L) 1f
        else (elapsedSeconds.toFloat() / plannedSeconds).coerceIn(0f, 1f)

        val integrityMultiplier = 0.35f + integrityScore.coerceIn(0, 100) / 100f * 0.65f
        val deepFocusRatio = if (elapsedSeconds <= 0L) 0f
        else (screenOffSeconds.toFloat() / elapsedSeconds).coerceIn(0f, 1f)
        val deepFocusMultiplier = 1f + deepFocusRatio * 0.12f

        val statusMultiplier = when (status) {
            SessionStatus.COMPLETED -> 1f
            SessionStatus.ABANDONED -> 0.20f
            SessionStatus.INTERRUPTED -> 0.10f
            SessionStatus.ACTIVE, SessionStatus.LEGACY -> 0f
        }

        return (completedMinutes * completion * integrityMultiplier * deepFocusMultiplier * statusMultiplier)
            .toInt()
            .coerceAtLeast(0)
    }

    fun coreStage(
        growthPoints: Int,
        integrityScore: Int,
        status: SessionStatus
    ): FocusCoreStage {
        if (status != SessionStatus.COMPLETED) {
            return if (growthPoints > 0) FocusCoreStage.FRACTURED else FocusCoreStage.NONE
        }
        if (integrityScore >= 97 && growthPoints >= 35) return FocusCoreStage.PRISM
        return when {
            growthPoints >= 70 -> FocusCoreStage.NOVA
            growthPoints >= 45 -> FocusCoreStage.CRYSTAL
            growthPoints >= 25 -> FocusCoreStage.ORB
            else -> FocusCoreStage.SPARK
        }
    }
}
