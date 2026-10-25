package com.utilitylab.focussense.domain

data class FloatRangeProfile(
    val min: Float,
    val max: Float
)

data class BestConditionsProfile(
    val ready: Boolean = false,
    val analyzedSessions: Int = 0,
    val sampleSize: Int = 0,
    val confidencePercent: Int = 0,
    val strongestContext: SessionContext? = null,
    val strongestDayPart: DayPart? = null,
    val preferredNoiseRangeDbfs: FloatRangeProfile? = null,
    val preferredLightRangeLux: FloatRangeProfile? = null,
    val preferredMotionMax: Float? = null,
    val typicalStablePercent: Float? = null,
    val typicalInterruptionMax: Int? = null,
    val messages: List<String> = emptyList()
)

data class EnvironmentMatch(
    val score: Int? = null,
    val noiseScore: Int? = null,
    val lightScore: Int? = null,
    val motionScore: Int? = null
)
