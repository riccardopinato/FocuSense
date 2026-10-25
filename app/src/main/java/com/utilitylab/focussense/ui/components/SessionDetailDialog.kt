package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.data.SessionDetailData
import com.utilitylab.focussense.data.local.SensorSampleEntity
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.domain.SessionTagCodec
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dettaglio sessione", color = FocuSenseText) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(sessionContext.label, style = MaterialTheme.typography.labelLarge)
                if (tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" · ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(date, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${s.focusScore ?: "—"}/100",
                    style = MaterialTheme.typography.displaySmall
                )
                Text("Durata ${s.actualDurationSeconds / 60} min · Interruzioni ${s.interruptionCount}")
                MetricLine("Rumore medio", s.avgNoiseDbfs?.let { "${it.roundToInt()} dBFS" })
                MetricLine("Luce media", s.avgLightLux?.let { "${it.roundToInt()} lux" })
                MetricLine("Movimento medio", s.avgMotionIndex?.let { "${it.roundToInt()}/100" })
                MetricLine("Stabilità", s.stablePercent?.let { "${it.roundToInt()}%" })
                MetricLine("Schermo rivolto in alto", s.faceUpPercent?.let { "${it.roundToInt()}%" })

                if (s.selfFocusRating != null || s.postureRating != null) {
                    HorizontalDivider()
                    Text(
                        "Focus percepito ${s.selfFocusRating ?: "—"}/5 · Postura ${s.postureRating ?: "—"}/5"
                    )
                }
                if (s.note.isNotBlank()) {
                    Text(s.note, style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()
                if (detail.samples.size >= 2) {
                    Text("Andamento durante la sessione", style = MaterialTheme.typography.titleMedium, color = FocuSenseText)
                    SessionMiniChart(detail.samples, Modifier.fillMaxWidth().height(120.dp))
                    Text(
                        "Grafico indicativo: rumore, luce e movimento sono normalizzati separatamente su scala 0-1.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "I campioni raw non sono più disponibili; il riepilogo aggregato della sessione resta comunque salvato.",
                        style = MaterialTheme.typography.bodySmall
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
private fun MetricLine(label: String, value: String?) {
    if (value == null) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SessionMiniChart(
    samples: List<SensorSampleEntity>,
    modifier: Modifier = Modifier
) {
    val points = samples.takeLast(180)
    if (points.size < 2) return

    val noise = points.map { ((it.noiseDbfs + 90f) / 90f).coerceIn(0f, 1f) }
    val maxLight = points.maxOfOrNull { it.lightLux }?.coerceAtLeast(1f) ?: 1f
    val light = points.map { (it.lightLux / maxLight).coerceIn(0f, 1f) }
    val motion = points.map { (it.motionIndex / 100f).coerceIn(0f, 1f) }

    val noiseColor = MaterialTheme.colorScheme.primary
    val lightColor = MaterialTheme.colorScheme.tertiary
    val motionColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier) {
        fun draw(values: List<Float>, color: androidx.compose.ui.graphics.Color) {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = if (values.size == 1) 0f else size.width * index / (values.size - 1f)
                val y = size.height * (1f - value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
        draw(noise, noiseColor)
        draw(light, lightColor)
        draw(motion, motionColor)
        drawLine(
            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = .3f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height)
        )
    }
}
