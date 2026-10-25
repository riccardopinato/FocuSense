package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

enum class DayPart(val label: String) {
    MORNING("Mattina"),
    AFTERNOON("Pomeriggio"),
    EVENING("Sera"),
    NIGHT("Notte");

    companion object {
        fun fromHour(hour: Int): DayPart = when (hour) {
            in 6..11 -> MORNING
            in 12..17 -> AFTERNOON
            in 18..22 -> EVENING
            else -> NIGHT
        }
    }
}

data class TemporalBucket(
    val sessions: Int = 0,
    val averageFocusScore: Int? = null,
    val averageSelfRating: Float? = null
)

data class HeatmapCell(
    val dayOfWeek: DayOfWeek,
    val dayPart: DayPart,
    val sessions: Int,
    val averageFocusScore: Int?
)

data class RecentTrend(
    val recentAverage: Int,
    val previousAverage: Int,
    val delta: Int
)

data class TemporalInsights(
    val byDayPart: Map<DayPart, TemporalBucket> = emptyMap(),
    val byDayOfWeek: Map<DayOfWeek, TemporalBucket> = emptyMap(),
    val heatmap: List<HeatmapCell> = emptyList(),
    val recentTrend: RecentTrend? = null,
    val messages: List<String> = emptyList()
)

object TemporalInsightsEngine {
    fun build(
        sessions: List<FocusSessionEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TemporalInsights {
        val completed = sessions
            .filter { it.endedAt != null && it.focusScore != null }
            .sortedByDescending { it.startedAt }

        if (completed.isEmpty()) {
            return TemporalInsights(
                heatmap = allHeatmapCells(emptyList(), zoneId),
                messages = listOf("Completa alcune sessioni per iniziare a vedere i pattern temporali.")
            )
        }

        val byPart = DayPart.entries.associateWith { part ->
            completed.filter { dayPartOf(it, zoneId) == part }.toBucket()
        }
        val byDay = DayOfWeek.entries.associateWith { day ->
            completed.filter { dayOfWeekOf(it, zoneId) == day }.toBucket()
        }

        val trend = if (completed.size >= 14) {
            val recent = completed.take(7).mapNotNull { it.focusScore }
            val previous = completed.drop(7).take(7).mapNotNull { it.focusScore }
            if (recent.size == 7 && previous.size == 7) {
                val a = recent.average().roundToInt()
                val b = previous.average().roundToInt()
                RecentTrend(a, b, a - b)
            } else null
        } else null

        return TemporalInsights(
            byDayPart = byPart,
            byDayOfWeek = byDay,
            heatmap = allHeatmapCells(completed, zoneId),
            recentTrend = trend,
            messages = buildMessages(byPart, byDay, trend)
        )
    }

    private fun List<FocusSessionEntity>.toBucket(): TemporalBucket {
        if (isEmpty()) return TemporalBucket()
        val scores = mapNotNull { it.focusScore }
        val ratings = mapNotNull { it.selfFocusRating }
        return TemporalBucket(
            sessions = size,
            averageFocusScore = scores.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            averageSelfRating = ratings.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        )
    }

    private fun allHeatmapCells(
        completed: List<FocusSessionEntity>,
        zoneId: ZoneId
    ): List<HeatmapCell> = buildList {
        DayOfWeek.entries.forEach { day ->
            DayPart.entries.forEach { part ->
                val group = completed.filter {
                    dayOfWeekOf(it, zoneId) == day && dayPartOf(it, zoneId) == part
                }
                add(
                    HeatmapCell(
                        dayOfWeek = day,
                        dayPart = part,
                        sessions = group.size,
                        averageFocusScore = group.mapNotNull { it.focusScore }
                            .takeIf { it.isNotEmpty() }
                            ?.average()
                            ?.roundToInt()
                    )
                )
            }
        }
    }

    private fun buildMessages(
        byPart: Map<DayPart, TemporalBucket>,
        byDay: Map<DayOfWeek, TemporalBucket>,
        trend: RecentTrend?
    ): List<String> = buildList {
        val reliableParts = byPart.filterValues { it.sessions >= 3 && it.averageFocusScore != null }
        if (reliableParts.isNotEmpty()) {
            val best = reliableParts.maxByOrNull { it.value.averageFocusScore ?: 0 }
            val worst = reliableParts.minByOrNull { it.value.averageFocusScore ?: 0 }
            if (best != null) {
                add("Nelle ${best.key.label.lowercase()} hai ${best.value.sessions} sessioni con Focus Score medio ${best.value.averageFocusScore}/100.")
            }
            if (best != null && worst != null && best.key != worst.key) {
                val diff = (best.value.averageFocusScore ?: 0) - (worst.value.averageFocusScore ?: 0)
                if (diff >= 8) {
                    add("Tra ${best.key.label.lowercase()} e ${worst.key.label.lowercase()} emerge una differenza descrittiva di circa $diff punti. Non indica un rapporto causale.")
                }
            }
        }

        val reliableDays = byDay.filterValues { it.sessions >= 3 && it.averageFocusScore != null }
        reliableDays.maxByOrNull { it.value.averageFocusScore ?: 0 }?.let {
            add("${italianDay(it.key)} è il giorno con il punteggio medio più alto tra quelli con almeno 3 sessioni (${it.value.averageFocusScore}/100).")
        }

        trend?.let {
            when {
                it.delta >= 6 -> add("Le ultime 7 sessioni sono in media ${it.delta} punti sopra le 7 precedenti.")
                it.delta <= -6 -> add("Le ultime 7 sessioni sono in media ${-it.delta} punti sotto le 7 precedenti.")
                else -> add("Le ultime 7 sessioni sono sostanzialmente in linea con le 7 precedenti.")
            }
        }

        if (isEmpty()) {
            add("Servono più sessioni distribuite nel tempo per produrre confronti temporali affidabili.")
        }
    }

    private fun dayPartOf(session: FocusSessionEntity, zoneId: ZoneId): DayPart {
        val hour = Instant.ofEpochMilli(session.startedAt).atZone(zoneId).hour
        return DayPart.fromHour(hour)
    }

    private fun dayOfWeekOf(session: FocusSessionEntity, zoneId: ZoneId): DayOfWeek =
        Instant.ofEpochMilli(session.startedAt).atZone(zoneId).dayOfWeek

    fun italianDay(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Lunedì"
        DayOfWeek.TUESDAY -> "Martedì"
        DayOfWeek.WEDNESDAY -> "Mercoledì"
        DayOfWeek.THURSDAY -> "Giovedì"
        DayOfWeek.FRIDAY -> "Venerdì"
        DayOfWeek.SATURDAY -> "Sabato"
        DayOfWeek.SUNDAY -> "Domenica"
    }
}
