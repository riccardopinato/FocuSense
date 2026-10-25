package com.utilitylab.focussense.domain

import com.utilitylab.focussense.data.local.FocusSessionEntity
import kotlin.math.roundToInt

data class ContextBucket(
    val context: SessionContext,
    val sessions: Int,
    val averageFocusScore: Int?,
    val averageSelfRating: Float?
)

data class ContextTrend(
    val context: SessionContext,
    val recentAverage: Int,
    val previousAverage: Int,
    val delta: Int
)

data class ContextAnalytics(
    val buckets: List<ContextBucket> = emptyList(),
    val trends: List<ContextTrend> = emptyList(),
    val messages: List<String> = emptyList()
)

object ContextAnalyticsEngine {
    fun build(sessions: List<FocusSessionEntity>): ContextAnalytics {
        val completed = sessions
            .filter { it.endedAt != null && it.focusScore != null }
            .sortedByDescending { it.startedAt }

        val buckets = SessionContext.entries.map { context ->
            val group = completed.filter { SessionContext.fromKey(it.contextKey) == context }
            ContextBucket(
                context = context,
                sessions = group.size,
                averageFocusScore = group.mapNotNull { it.focusScore }
                    .takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
                averageSelfRating = group.mapNotNull { it.selfFocusRating }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat()
            )
        }

        val trends = SessionContext.entries.mapNotNull { context ->
            val group = completed.filter { SessionContext.fromKey(it.contextKey) == context }
            if (group.size < 6) return@mapNotNull null

            val recent = group.take(3).mapNotNull { it.focusScore }
            val previous = group.drop(3).take(3).mapNotNull { it.focusScore }
            if (recent.size != 3 || previous.size != 3) return@mapNotNull null

            val r = recent.average().roundToInt()
            val p = previous.average().roundToInt()
            ContextTrend(context, r, p, r - p)
        }

        val messages = buildList {
            val reliable = buckets.filter { it.sessions >= 3 && it.averageFocusScore != null }

            reliable.maxByOrNull { it.averageFocusScore ?: 0 }?.let { best ->
                add("Nel contesto ${best.context.label.lowercase()} hai ${best.sessions} sessioni con Focus Score medio ${best.averageFocusScore}/100.")
            }

            if (reliable.size >= 2) {
                val best = reliable.maxByOrNull { it.averageFocusScore ?: 0 }
                val worst = reliable.minByOrNull { it.averageFocusScore ?: 0 }
                if (best != null && worst != null && best.context != worst.context) {
                    val delta = (best.averageFocusScore ?: 0) - (worst.averageFocusScore ?: 0)
                    if (delta >= 8) {
                        add("Tra ${best.context.label.lowercase()} e ${worst.context.label.lowercase()} emerge una differenza descrittiva di circa $delta punti. Non dimostra un rapporto causale.")
                    }
                }
            }

            trends.forEach { trend ->
                when {
                    trend.delta >= 6 -> add("Nel contesto ${trend.context.label.lowercase()}, le ultime 3 sessioni sono ${trend.delta} punti sopra le 3 precedenti.")
                    trend.delta <= -6 -> add("Nel contesto ${trend.context.label.lowercase()}, le ultime 3 sessioni sono ${-trend.delta} punti sotto le 3 precedenti.")
                }
            }

            if (isEmpty()) {
                add("Servono almeno 3 sessioni nello stesso contesto per produrre confronti contestuali affidabili.")
            }
        }

        return ContextAnalytics(buckets, trends, messages)
    }
}
