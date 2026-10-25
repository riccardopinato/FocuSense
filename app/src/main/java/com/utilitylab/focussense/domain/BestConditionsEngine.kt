package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import kotlin.math.roundToInt

object BestConditionsEngine {

    fun build(
        sessions: List<FocusSessionEntity>,
        zoneProvider: (Long) -> Pair<DayPart, java.time.DayOfWeek>
    ): BestConditionsProfile {
        val rated = sessions.filter {
            it.endedAt != null &&
                it.actualDurationSeconds >= 600 &&
                it.selfFocusRating != null
        }

        if (rated.size < 5) {
            return BestConditionsProfile(
                ready = false,
                analyzedSessions = rated.size,
                messages = listOf(
                    "Valuta almeno 5 sessioni per stimare le condizioni associate al tuo focus percepito."
                )
            )
        }

        val productive = rated.filter { (it.selfFocusRating ?: 0) >= 4 }
        val best = if (productive.size >= 3) {
            productive
        } else {
            rated.sortedByDescending { it.selfFocusRating ?: 0 }.take(3)
        }

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
        val interruptions = best.map {
            (it.foregroundBreakCount + it.pickupCount).toFloat()
        }

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

        var confidence = 35
        confidence += ((rated.size - 5).coerceAtMost(15) * 3)
        confidence += (productive.size.coerceAtMost(8) * 3)
        if (noiseRange != null && lightRange != null) confidence += 8
        confidence = confidence.coerceAtMost(100)

        val messages = buildList {
            context?.let {
                add("Tra le sessioni che hai valutato meglio ricorre più spesso il contesto ${it.label.lowercase()}.")
            }
            dayPart?.let {
                add("Tra le sessioni che hai valutato meglio ricorre più spesso la fascia ${it.label.lowercase()}.")
            }
            noiseRange?.let {
                add("Quando valuti il focus 4–5/5, il rumore medio ricade spesso tra ${it.min.roundToInt()} e ${it.max.roundToInt()} dBFS.")
            }
            lightRange?.let {
                add("Quando valuti il focus 4–5/5, la luce media ricade spesso tra ${it.min.roundToInt()} e ${it.max.roundToInt()} lux.")
            }
            stableTypical?.let {
                add("Nelle sessioni migliori il telefono resta stabile per circa ${it.roundToInt()}% del tempo.")
            }
            add("Sono associazioni del tuo storico personale: non dimostrano causalità.")
        }

        return BestConditionsProfile(
            ready = true,
            analyzedSessions = rated.size,
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

    internal fun selectBest(analyzed: List<FocusSessionEntity>): List<FocusSessionEntity> =
        analyzed
            .filter { it.selfFocusRating != null }
            .sortedWith(
                compareByDescending<FocusSessionEntity> { it.selfFocusRating ?: 0 }
                    .thenByDescending { it.startedAt }
            )
            .let { rated ->
                val productive = rated.filter { (it.selfFocusRating ?: 0) >= 4 }
                if (productive.size >= 3) productive else rated.take(3)
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

        val total = parts.takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()

        return EnvironmentMatch(
            score = total,
            noiseScore = noiseScore,
            lightScore = lightScore,
            motionScore = null
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
