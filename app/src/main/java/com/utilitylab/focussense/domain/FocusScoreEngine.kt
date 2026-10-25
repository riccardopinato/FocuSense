package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.SensorSampleEntity
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Scores only the environment (noise + light).
 *
 * Phone movement is deliberately excluded from this score and is handled by
 * [FocusIntegrityEngine]. This prevents the product from conflating ambient
 * conditions with the user's device-use discipline.
 */
object FocusScoreEngine {
    private val allSensorsAvailable = SensorAvailability(true, true, true, true)

    fun liveScore(
        noiseDbfs: Float,
        lightLux: Float,
        motionIndex: Float,
        targets: FocusTargets = FocusTargets(),
        availability: SensorAvailability = allSensorsAvailable,
        weights: FocusWeights = FocusWeights()
    ): Int = liveBreakdown(
        noiseDbfs = noiseDbfs,
        lightLux = lightLux,
        motionIndex = motionIndex,
        targets = targets,
        availability = availability,
        weights = weights
    ).total

    fun liveBreakdown(
        noiseDbfs: Float,
        lightLux: Float,
        motionIndex: Float,
        targets: FocusTargets = FocusTargets(),
        availability: SensorAvailability = allSensorsAvailable,
        weights: FocusWeights = FocusWeights()
    ): FocusScoreBreakdown {
        val baseNoise = weights.noise.coerceAtLeast(0f)
        val baseLight = weights.light.coerceAtLeast(0f)
        val wn = if (availability.microphone) baseNoise else 0f
        val wl = if (availability.light) baseLight else 0f
        val totalWeight = wn + wl

        if (totalWeight <= 0f) {
            return FocusScoreBreakdown(
                total = 0,
                effectiveWeights = FocusWeights(0f, 0f, 0f)
            )
        }

        val effective = FocusWeights(
            noise = wn / totalWeight,
            light = wl / totalWeight,
            motion = 0f
        )
        val ns = if (availability.microphone) noiseScore(noiseDbfs, targets).toInt() else null
        val ls = if (availability.light) lightScore(lightLux, targets).toInt() else null
        val movementDiagnostic =
            if (availability.accelerometer) movementScore(motionIndex, targets).toInt() else null

        val total = (
            (ns ?: 0) * effective.noise +
                (ls ?: 0) * effective.light
            ).toInt().coerceIn(0, 100)

        return FocusScoreBreakdown(
            total = total,
            noise = ns,
            light = ls,
            motion = movementDiagnostic,
            effectiveWeights = effective
        )
    }

    fun summarize(
        samples: List<SensorSampleEntity>,
        targets: FocusTargets = FocusTargets(),
        availability: SensorAvailability = allSensorsAvailable,
        weights: FocusWeights = FocusWeights()
    ): SessionSummary {
        if (samples.isEmpty()) {
            return SessionSummary(
                environmentScore = 0,
                avgNoiseDbfs = -90f,
                noiseStdDev = 0f,
                avgLightLux = 0f,
                lightStdDev = 0f,
                avgMotionIndex = 0f,
                avgGyro = 0f,
                stablePercent = 0f,
                faceUpPercent = 0f,
                interruptionCount = 0
            )
        }

        val avgNoise = samples.map { it.noiseDbfs }.average().toFloat()
        val avgLight = samples.map { it.lightLux }.average().toFloat()
        val avgMotion = samples.map { it.motionIndex }.average().toFloat()
        val avgGyro = samples.map { it.gyroMagnitude }.average().toFloat()
        val stablePercent = samples.count { it.deviceStable } * 100f / samples.size
        val faceUpPercent = samples.count { it.faceUp } * 100f / samples.size
        val noiseStd = if (availability.microphone) stdDev(samples.map { it.noiseDbfs }) else 0f
        val lightStd = if (availability.light) stdDev(samples.map { it.lightLux }) else 0f

        val base = liveScore(
            noiseDbfs = avgNoise,
            lightLux = avgLight,
            motionIndex = avgMotion,
            targets = targets,
            availability = availability,
            weights = weights
        )

        var variabilityPenalty = 0f
        if (availability.microphone) variabilityPenalty += (noiseStd / 15f) * 7f
        if (availability.light) variabilityPenalty += (lightStd / 800f) * 4f
        variabilityPenalty = variabilityPenalty.coerceIn(0f, 12f)

        return SessionSummary(
            environmentScore = (base - variabilityPenalty).toInt().coerceIn(0, 100),
            avgNoiseDbfs = avgNoise,
            noiseStdDev = noiseStd,
            avgLightLux = avgLight,
            lightStdDev = lightStd,
            avgMotionIndex = avgMotion,
            avgGyro = avgGyro,
            stablePercent = stablePercent,
            faceUpPercent = faceUpPercent,
            interruptionCount = if (availability.accelerometer) countMotionBursts(samples) else 0
        )
    }

    fun combinedSessionScore(environmentScore: Int, integrityScore: Int): Int =
        (environmentScore.coerceIn(0, 100) * 0.40f +
            integrityScore.coerceIn(0, 100) * 0.60f)
            .toInt()
            .coerceIn(0, 100)

    private fun noiseScore(dbfs: Float, t: FocusTargets): Float {
        if (dbfs <= t.quietNoiseDbfs) return 100f
        if (dbfs >= t.loudNoiseDbfs) return 0f
        return (
            100f *
                (1f - (dbfs - t.quietNoiseDbfs) / (t.loudNoiseDbfs - t.quietNoiseDbfs))
            ).coerceIn(0f, 100f)
    }

    private fun lightScore(lux: Float, t: FocusTargets): Float {
        if (lux in t.preferredMinLightLux..t.preferredMaxLightLux) return 100f
        if (lux < t.preferredMinLightLux) {
            return (lux / t.preferredMinLightLux * 100f).coerceIn(0f, 100f)
        }
        return (
            100f - ((lux - t.preferredMaxLightLux) / 2500f) * 100f
            ).coerceIn(0f, 100f)
    }

    private fun movementScore(value: Float, t: FocusTargets): Float {
        val stable = t.stableMotionIndex.coerceIn(5f, 45f)
        if (value <= stable) return 100f
        val bad = (stable + 60f).coerceAtMost(100f)
        return 100f * (
            1f - ((value - stable) / (bad - stable)).coerceIn(0f, 1f)
            )
    }

    private fun stdDev(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average()
        return sqrt(
            values.sumOf { (it.toDouble() - mean).pow(2.0) } / values.size
        ).toFloat()
    }

    private fun countMotionBursts(samples: List<SensorSampleEntity>): Int {
        var count = 0
        var inside = false
        samples.forEach {
            val high = it.motionIndex >= 45f
            if (high && !inside) count++
            inside = high
        }
        return count
    }
}
