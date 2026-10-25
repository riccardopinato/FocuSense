package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.data.SessionDetailData
import com.utilitylab.focussense.data.local.SensorSampleEntity
import com.utilitylab.focussense.domain.FocusCoreStage
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionStatus
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun SessionDetailDialog(
    detail: SessionDetailData,
    onDismiss: () -> Unit
) {
    val s = detail.session
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val date = Instant.ofEpochMilli(s.startedAt)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
    val sessionContext = SessionContext.fromKey(s.contextKey)
    val tags = SessionTagCodec.decode(s.tagsCsv)
    val stage = FocusCoreStage.fromKey(s.coreStageKey)
    val status = SessionStatus.fromKey(s.sessionStatusKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dettaglio sessione", color = FocuSenseText) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    sessionContext.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = FocuSenseText
                )
                if (tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" · ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall,
                        color = FocuSenseGreen
                    )
                }
                Text(
                    date,
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuSenseTextMuted
                )

                if (stage != FocusCoreStage.NONE) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                stage.label,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (stage == FocusCoreStage.PRISM) {
                                    FocuSenseGreen
                                } else {
                                    FocuSensePurple
                                }
                            )
                            Text(
                                "${s.growthPoints} crescita · ${status.name.lowercase()}",
                                color = FocuSenseTextMuted
                            )
                        }
                        FocusCoreVisual(
                            stage = stage,
                            integrity = s.integrityScore ?: 0,
                            size = 72.dp
                        )
                    }
                }

                HorizontalDivider()

                MetricLine(
                    "Session Score",
                    s.focusScore?.let { "$it/100" },
                    FocuSenseCyan
                )
                MetricLine(
                    "Focus Integrity",
                    s.integrityScore?.let { "$it%" },
                    FocuSenseGreen
                )
                MetricLine(
                    "Environment Score",
                    s.environmentScore?.let { "$it/100" },
                    FocuSenseCyan
                )
                MetricLine(
                    "Focus percepito",
                    s.selfFocusRating?.let { "$it/5" },
                    FocuSensePurple
                )
                MetricLine(
                    "Durata",
                    "${s.actualDurationSeconds / 60} min"
                )
                MetricLine("Pickup", s.pickupCount.toString())
                MetricLine("Uscite dall'app", s.foregroundBreakCount.toString())
                MetricLine(
                    "Tempo fuori app",
                    formatDuration(s.backgroundSeconds)
                )
                MetricLine(
                    "Deep focus schermo spento",
                    formatDuration(s.screenOffSeconds)
                )
                MetricLine(
                    "Instabilità",
                    "${s.unstablePercent.roundToInt()}%"
                )

                HorizontalDivider()

                MetricLine(
                    "Rumore medio",
                    s.avgNoiseDbfs?.let { "${it.roundToInt()} dBFS" }
                )
                MetricLine(
                    "Luce media",
                    s.avgLightLux?.let { "${it.roundToInt()} lux" }
                )
                MetricLine(
                    "Movimento medio",
                    s.avgMotionIndex?.let { "${it.roundToInt()}/100" }
                )
                MetricLine(
                    "Stabilità sensore",
                    s.stablePercent?.let { "${it.roundToInt()}%" }
                )

                if (s.note.isNotBlank()) {
                    HorizontalDivider()
                    Text(
                        s.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FocuSenseTextMuted
                    )
                }

                HorizontalDivider()
                if (detail.samples.size >= 2) {
                    Text(
                        "Andamento sensori",
                        style = MaterialTheme.typography.titleMedium,
                        color = FocuSenseText
                    )
                    SessionMiniChart(
                        detail.samples,
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Text(
                        "Rumore, luce e movimento sono normalizzati separatamente nel grafico. Il movimento non entra nell'Environment Score.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FocuSenseTextMuted
                    )
                } else {
                    Text(
                        "I campioni raw non sono più disponibili; il riepilogo aggregato resta salvato.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FocuSenseTextMuted
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

@Composable
private fun MetricLine(
    label: String,
    value: String?,
    accent: androidx.compose.ui.graphics.Color? = null
) {
    if (value == null) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = FocuSenseTextMuted)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = accent ?: FocuSenseTextMuted
        )
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "0 min"
    val minutes = seconds / 60L
    val secs = seconds % 60L
    return if (minutes > 0L) "${minutes}m ${secs}s" else "${secs}s"
}

@Composable
private fun SessionMiniChart(
    samples: List<SensorSampleEntity>,
    modifier: Modifier = Modifier
) {
    val points = samples.takeLast(180)
    if (points.size < 2) return

    val noise = points.map {
        ((it.noiseDbfs + 90f) / 90f).coerceIn(0f, 1f)
    }
    val maxLight = points.maxOfOrNull { it.lightLux }
        ?.coerceAtLeast(1f)
        ?: 1f
    val light = points.map {
        (it.lightLux / maxLight).coerceIn(0f, 1f)
    }
    val motion = points.map {
        (it.motionIndex / 100f).coerceIn(0f, 1f)
    }

    Canvas(modifier) {
        fun draw(
            values: List<Float>,
            color: androidx.compose.ui.graphics.Color
        ) {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = size.width * index / (values.size - 1f)
                val y = size.height * (1f - value)
                if (index == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }
            drawPath(
                path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }

        draw(noise, FocuSenseCyan)
        draw(light, FocuSenseGreen)
        draw(motion, FocuSensePurple)
        drawLine(
            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = .3f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height)
        )
    }
}
