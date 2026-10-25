package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import kotlin.math.ceil
import kotlin.math.roundToInt

object BestConditionsEngine {

    fun build(
        sessions: List<FocusSessionEntity>,
        zoneProvider: (Long) -> Pair<DayPart, java.time.DayOfWeek>
    ): BestConditionsProfile {
        val analyzed = sessions.filter {
            it.endedAt != null &&
                it.focusScore != null &&
                it.actualDurationSeconds >= 600
        }

        if (analyzed.size < 5) {
            return BestConditionsProfile(
                ready = false,
                analyzedSessions = analyzed.size,
                messages = listOf(
                    "Servono almeno 5 sessioni valide per stimare le condizioni migliori."
                )
            )
        }

        val selected = selectBest(analyzed)
        val highlyRated = selected.filter { (it.selfFocusRating ?: 0) >= 4 }
        val best = if (highlyRated.size >= 3) highlyRated else selected

        val context = best
            .groupingBy { SessionContext.fromKey(it.contextKey) }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val dayPart = best
            .map { zoneProvider(it.startedAt).first }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val noise = best.mapNotNull { it.avgNoiseDbfs }
        val light = best.mapNotNull { it.avgLightLux }
        val motion = best.mapNotNull { it.avgMotionIndex }
        val stable = best.mapNotNull { it.stablePercent }
        val interruptions = best.map { it.interruptionCount.toFloat() }

        val noiseRange = if (noise.size >= 3) {
            FloatRangeProfile(percentile(noise, .25f)!!, percentile(noise, .75f)!!)
        } else null

        val lightRange = if (light.size >= 3) {
            FloatRangeProfile(percentile(light, .25f)!!, percentile(light, .75f)!!)
        } else null

        val motionMax = if (motion.size >= 3) percentile(motion, .75f) else null
        val stableTypical = if (stable.size >= 3) percentile(stable, .50f) else null
        val interruptionMax = if (interruptions.size >= 3) {
            percentile(interruptions, .75f)?.roundToInt()
        } else null

        val sensorMetrics = listOfNotNull(
            noiseRange,
            lightRange,
            motionMax
        ).size

        var confidence = 35
        confidence += ((analyzed.size - 5).coerceAtMost(10) * 4)
        if (best.size >= 5) confidence += 10
        if (highlyRated.size >= 3) confidence += 10
        if (sensorMetrics >= 3) confidence += 5
        confidence = confidence.coerceAtMost(100)

        val messages = buildList {
            context?.let {
                add("Nelle sessioni migliori il contesto più frequente è ${it.label.lowercase()}.")
            }
            dayPart?.let {
                add("La fascia oraria più rappresentata nelle sessioni migliori è ${it.label.lowercase()}.")
            }
            noiseRange?.let {
                add("Nelle sessioni migliori il rumore medio ricade spesso tra ${it.min.roundToInt()} e ${it.max.roundToInt()} dBFS.")
            }
            lightRange?.let {
                add("La luce media delle sessioni migliori si concentra spesso tra ${it.min.roundToInt()} e ${it.max.roundToInt()} lux.")
            }
            add("Questi pattern descrivono associazioni nello storico e non dimostrano causalità.")
        }

        return BestConditionsProfile(
            ready = true,
            analyzedSessions = analyzed.size,
            sampleSize = best.size,
            confidencePercent = confidence,
            strongestContext = context,
            strongestDayPart = dayPart,
            preferredNoiseRangeDbfs = noiseRange,
            preferredLightRangeLux = lightRange,
            preferredMotionMax = motionMax,
            typicalStablePercent = stableTypical,
            typicalInterruptionMax = interruptionMax,
            messages = messages
        )
    }

    fun build(
        sessions: List<FocusSessionEntity>,
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): BestConditionsProfile =
        build(sessions) { epoch ->
            val zdt = java.time.Instant.ofEpochMilli(epoch).atZone(zoneId)
            DayPart.fromHour(zdt.hour) to zdt.dayOfWeek
        }

    internal fun selectBest(
        analyzed: List<FocusSessionEntity>
    ): List<FocusSessionEntity> {
        val sorted = analyzed.sortedByDescending { it.focusScore ?: 0 }

        return if (analyzed.size >= 8) {
            val count = ceil(analyzed.size * .35).toInt().coerceAtLeast(3)
            sorted.take(count)
        } else {
            val median = percentile(
                analyzed.mapNotNull { it.focusScore?.toFloat() },
                .50f
            ) ?: 0f

            analyzed.filter { (it.focusScore ?: 0) >= median }
        }
    }

    fun environmentMatch(
        profile: BestConditionsProfile,
        live: LiveSensorSnapshot,
        availability: SensorAvailability
    ): EnvironmentMatch {
        if (!profile.ready) return EnvironmentMatch()

        val parts = mutableListOf<Int>()

        val noiseScore =
            if (availability.microphone && profile.preferredNoiseRangeDbfs != null) {
                rangeScore(
                    live.noiseDbfs,
                    profile.preferredNoiseRangeDbfs.min,
                    profile.preferredNoiseRangeDbfs.max,
                    20f
                ).also { parts += it }
            } else null

        val lightScore =
            if (availability.light && profile.preferredLightRangeLux != null) {
                rangeScore(
                    live.lightLux,
                    profile.preferredLightRangeLux.min,
                    profile.preferredLightRangeLux.max,
                    1000f
                ).also { parts += it }
            } else null

        val motionScore =
            if (availability.accelerometer && profile.preferredMotionMax != null) {
                upperBoundScore(
                    live.motionIndex,
                    profile.preferredMotionMax,
                    50f
                ).also { parts += it }
            } else null

        val total =
            parts.takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToInt()

        return EnvironmentMatch(
            score = total,
            noiseScore = noiseScore,
            lightScore = lightScore,
            motionScore = motionScore
        )
    }

    internal fun rangeScore(
        value: Float,
        min: Float,
        max: Float,
        decay: Float
    ): Int {
        if (value in min..max) return 100
        val distance = if (value < min) min - value else value - max
        return (100f * (1f - distance / decay))
            .coerceIn(0f, 100f)
            .roundToInt()
    }

    internal fun upperBoundScore(
        value: Float,
        max: Float,
        decay: Float
    ): Int {
        if (value <= max) return 100
        return (100f * (1f - (value - max) / decay))
            .coerceIn(0f, 100f)
            .roundToInt()
    }

    internal fun percentile(
        values: List<Float>,
        q: Float
    ): Float? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val pos = sorted.lastIndex * q.coerceIn(0f, 1f)
        val lo = pos.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val f = pos - lo
        return sorted[lo] + (sorted[hi] - sorted[lo]) * f
    }
}
