package com.utilitylab.focussense.domain

data class LiveSensorSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val noiseDbfs: Float = -90f,
    val lightLux: Float = 0f,
    val motionIndex: Float = 0f,
    val gyroMagnitude: Float = 0f,
    val faceUp: Boolean = true,
    val deviceStable: Boolean = true,
    val focusScore: Int = 0,
    val sensorsAvailable: SensorAvailability = SensorAvailability()
)

data class SensorAvailability(
    val microphone: Boolean = true,
    val light: Boolean = false,
    val accelerometer: Boolean = false,
    val gyroscope: Boolean = false
)

data class FocusTargets(
    val preferredMinLightLux: Float = 250f,
    val preferredMaxLightLux: Float = 1000f,
    val quietNoiseDbfs: Float = -55f,
    val loudNoiseDbfs: Float = -20f,
    val stableMotionIndex: Float = 18f
)

data class FocusWeights(
    val noise: Float = 0.40f,
    val light: Float = 0.30f,
    val motion: Float = 0.30f
) {
    fun normalized(): FocusWeights {
        val n = noise.coerceAtLeast(0f)
        val l = light.coerceAtLeast(0f)
        val m = motion.coerceAtLeast(0f)
        val total = n + l + m
        return if (total <= 0f) FocusWeights() else FocusWeights(n / total, l / total, m / total)
    }
}

enum class CalibrationLevel { GENERIC, LEARNING, PERSONALIZED, STRONG }

data class PersonalFocusProfile(
    val targets: FocusTargets = FocusTargets(),
    val weights: FocusWeights = FocusWeights(),
    val level: CalibrationLevel = CalibrationLevel.GENERIC,
    val confidencePercent: Int = 0,
    val ratedSessions: Int = 0,
    val productiveSessions: Int = 0,
    val personalizedNoise: Boolean = false,
    val personalizedLight: Boolean = false,
    val personalizedMotion: Boolean = false
) {
    companion object { fun generic() = PersonalFocusProfile() }
}

data class FocusScoreBreakdown(
    val total: Int,
    val noise: Int? = null,
    val light: Int? = null,
    val motion: Int? = null,
    val effectiveWeights: FocusWeights = FocusWeights()
)

data class SessionSummary(
    val focusScore: Int,
    val avgNoiseDbfs: Float,
    val noiseStdDev: Float,
    val avgLightLux: Float,
    val lightStdDev: Float,
    val avgMotionIndex: Float,
    val avgGyro: Float,
    val stablePercent: Float,
    val faceUpPercent: Float,
    val interruptionCount: Int
)
