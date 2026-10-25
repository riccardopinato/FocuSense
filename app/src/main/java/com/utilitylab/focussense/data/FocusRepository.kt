package com.utilitylab.focussense.data

import com.utilitylab.focussense.data.local.AppDatabase
import com.utilitylab.focussense.data.local.FocusSessionEntity
import com.utilitylab.focussense.data.local.SensorSampleEntity
import com.utilitylab.focussense.domain.*
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

data class SessionDetailData(
    val session: FocusSessionEntity,
    val samples: List<SensorSampleEntity>
)

class FocusRepository(private val db: AppDatabase) {
    val sessions: Flow<List<FocusSessionEntity>> = db.focusSessionDao().observeAll()

    suspend fun createSession(
        plannedMinutes: Int,
        context: SessionContext = SessionContext.WORK,
        tags: List<String> = emptyList()
    ): Long = db.focusSessionDao().insert(
        FocusSessionEntity(
            startedAt = System.currentTimeMillis(),
            plannedDurationMinutes = plannedMinutes,
            contextKey = context.key,
            tagsCsv = SessionTagCodec.encode(tags)
        )
    )

    suspend fun addSample(sample: SensorSampleEntity) = db.sensorSampleDao().insert(sample)

    suspend fun samples(sessionId: Long): List<SensorSampleEntity> =
        db.sensorSampleDao().getForSession(sessionId)

    fun observeSamples(sessionId: Long): Flow<List<SensorSampleEntity>> =
        db.sensorSampleDao().observeForSession(sessionId)

    suspend fun sessionDetail(sessionId: Long): SessionDetailData? {
        val session = db.focusSessionDao().getById(sessionId) ?: return null
        return SessionDetailData(
            session = session,
            samples = db.sensorSampleDao().getForSession(sessionId)
        )
    }

    suspend fun finalizeSession(
        sessionId: Long,
        postureReminderCount: Int = 0,
        profile: PersonalFocusProfile = PersonalFocusProfile.generic(),
        availability: SensorAvailability = SensorAvailability(true, true, true, true)
    ) {
        val session = db.focusSessionDao().getById(sessionId) ?: return
        val samples = db.sensorSampleDao().getForSession(sessionId)
        val summary = FocusScoreEngine.summarize(samples, profile.targets, availability, profile.weights)
        val end = System.currentTimeMillis()
        db.focusSessionDao().update(session.copy(
            endedAt = end,
            actualDurationSeconds = ((end - session.startedAt) / 1000L).coerceAtLeast(0),
            focusScore = summary.focusScore,
            avgNoiseDbfs = summary.avgNoiseDbfs.takeIf { availability.microphone },
            noiseStdDev = summary.noiseStdDev.takeIf { availability.microphone },
            avgLightLux = summary.avgLightLux.takeIf { availability.light },
            lightStdDev = summary.lightStdDev.takeIf { availability.light },
            avgMotionIndex = summary.avgMotionIndex.takeIf { availability.accelerometer },
            avgGyro = summary.avgGyro.takeIf { availability.gyroscope },
            stablePercent = summary.stablePercent.takeIf { availability.accelerometer },
            faceUpPercent = summary.faceUpPercent.takeIf { availability.accelerometer },
            interruptionCount = if (availability.accelerometer) summary.interruptionCount else 0,
            postureReminderCount = postureReminderCount
        ))
    }

    suspend fun buildFocusTargets(): FocusTargets = buildPersonalProfile().targets

    suspend fun buildPersonalProfile(): PersonalFocusProfile {
        val completed = db.focusSessionDao().getRecentCompleted(60)
        val rated = completed.filter { it.selfFocusRating != null }.take(30)
        val productive = rated.filter { (it.selfFocusRating ?: 0) >= 4 }.take(15)
        if (productive.size < 3) return PersonalFocusProfile(
            level = CalibrationLevel.GENERIC,
            confidencePercent = (productive.size * 12).coerceAtMost(30),
            ratedSessions = rated.size,
            productiveSessions = productive.size
        )

        val defaults = FocusTargets()
        val noises = productive.mapNotNull { it.avgNoiseDbfs }.filter { it in -90f..0f }
        val lights = productive.mapNotNull { it.avgLightLux }.filter { it >= 0f }
        val motions = productive.mapNotNull { it.avgMotionIndex }.filter { it >= 0f }
        val personalNoise = noises.size >= 3
        val personalLight = lights.size >= 3
        val personalMotion = motions.size >= 3

        val medianNoise = percentile(noises, .50f)
        val p25Light = percentile(lights, .25f)
        val p75Light = percentile(lights, .75f)
        val p75Motion = percentile(motions, .75f)

        val targets = FocusTargets(
            quietNoiseDbfs = if (personalNoise) (medianNoise!! - 10f).coerceIn(-80f, -35f) else defaults.quietNoiseDbfs,
            loudNoiseDbfs = if (personalNoise) (medianNoise!! + 18f).coerceIn(-35f, -8f) else defaults.loudNoiseDbfs,
            preferredMinLightLux = if (personalLight) (p25Light!! * .75f).coerceIn(100f, 900f) else defaults.preferredMinLightLux,
            preferredMaxLightLux = if (personalLight) (p75Light!! * 1.35f + 80f).coerceIn(350f, 2500f) else defaults.preferredMaxLightLux,
            stableMotionIndex = if (personalMotion) (p75Motion!! + 5f).coerceIn(10f, 35f) else defaults.stableMotionIndex
        ).let {
            if (it.preferredMaxLightLux <= it.preferredMinLightLux + 100f)
                it.copy(preferredMaxLightLux = it.preferredMinLightLux + 150f)
            else it
        }

        val level = when {
            productive.size >= 10 -> CalibrationLevel.STRONG
            productive.size >= 6 -> CalibrationLevel.PERSONALIZED
            else -> CalibrationLevel.LEARNING
        }
        val confidence = when (level) {
            CalibrationLevel.GENERIC -> 0
            CalibrationLevel.LEARNING -> 35 + (productive.size - 3) * 10
            CalibrationLevel.PERSONALIZED -> 65 + (productive.size - 6) * 6
            CalibrationLevel.STRONG -> 90 + (productive.size - 10) * 2
        }.coerceIn(0, 100)

        val adaptive = buildAdaptiveWeights(rated, productive)
        val blend = (confidence / 100f * .65f).coerceIn(0f, .65f)
        val d = FocusWeights()
        val weights = FocusWeights(
            noise = d.noise * (1f - blend) + adaptive.noise * blend,
            light = d.light * (1f - blend) + adaptive.light * blend,
            motion = d.motion * (1f - blend) + adaptive.motion * blend
        ).normalized()

        return PersonalFocusProfile(
            targets = targets,
            weights = weights,
            level = level,
            confidencePercent = confidence,
            ratedSessions = rated.size,
            productiveSessions = productive.size,
            personalizedNoise = personalNoise,
            personalizedLight = personalLight,
            personalizedMotion = personalMotion
        )
    }

    private fun buildAdaptiveWeights(
        rated: List<FocusSessionEntity>,
        productive: List<FocusSessionEntity>
    ): FocusWeights {
        val lower = rated.filter { (it.selfFocusRating ?: 5) <= 3 }
        if (rated.size < 6 || productive.size < 3 || lower.size < 2) return FocusWeights()

        fun avg(list: List<FocusSessionEntity>, pick: (FocusSessionEntity) -> Float?) =
            list.mapNotNull(pick).takeIf { it.isNotEmpty() }?.average()?.toFloat()

        val hn = avg(productive) { it.avgNoiseDbfs }
        val ln = avg(lower) { it.avgNoiseDbfs }
        val hm = avg(productive) { it.avgMotionIndex }
        val lm = avg(lower) { it.avgMotionIndex }
        val highLights = productive.mapNotNull { it.avgLightLux }
        val lowLights = lower.mapNotNull { it.avgLightLux }
        val lightCenter = percentile(highLights, .50f)

        val noiseEvidence = if (hn != null && ln != null) ((ln - hn) / 20f).coerceIn(0f, 1f) else .35f
        val motionEvidence = if (hm != null && lm != null) ((lm - hm) / 30f).coerceIn(0f, 1f) else .35f
        val lightEvidence = if (lightCenter != null && lowLights.isNotEmpty())
            (lowLights.map { abs(it - lightCenter) }.average().toFloat() / 700f).coerceIn(0f, 1f)
        else .35f

        return FocusWeights(
            .25f + noiseEvidence,
            .25f + lightEvidence,
            .25f + motionEvidence
        ).normalized()
    }

    private fun percentile(values: List<Float>, q: Float): Float? {
        if (values.isEmpty()) return null
        val s = values.sorted()
        val pos = s.lastIndex * q.coerceIn(0f, 1f)
        val lo = pos.toInt()
        val hi = (lo + 1).coerceAtMost(s.lastIndex)
        val f = pos - lo
        return s[lo] + (s[hi] - s[lo]) * f
    }

    suspend fun saveSelfRatings(
        sessionId: Long,
        focusRating: Int?,
        postureRating: Int?,
        note: String
    ) {
        val s = db.focusSessionDao().getById(sessionId) ?: return
        db.focusSessionDao().update(
            s.copy(
                selfFocusRating = focusRating?.coerceIn(1, 5),
                postureRating = postureRating?.coerceIn(1, 5),
                note = note.take(500)
            )
        )
    }

    suspend fun cleanupRawSamples(daysToKeep: Int = 30): Int =
        db.sensorSampleDao().deleteOlderThan(
            System.currentTimeMillis() - daysToKeep * 86400000L
        )

    suspend fun deleteUnfinishedSessions(): Int =
        db.focusSessionDao().deleteUnfinished()

    suspend fun deleteSession(id: Long) =
        db.focusSessionDao().deleteById(id)
}
