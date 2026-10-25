package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.data.local.FocusSessionEntity
import java.time.Instant
import java.time.ZoneId

@Composable
fun FocusTrendChart(
    sessions: List<FocusSessionEntity>,
    modifier: Modifier = Modifier
) {
    val points = sessions
        .filter { it.focusScore != null }
        .sortedBy { it.startedAt }
        .takeLast(30)
        .map { it.focusScore!!.coerceIn(0, 100).toFloat() }

    if (points.isEmpty()) {
        Text(
            "Nessun dato disponibile",
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxWidth()) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val width = (right - left).coerceAtLeast(1f)
        val height = (bottom - top).coerceAtLeast(1f)

        listOf(0f, .25f, .5f, .75f, 1f).forEach { fraction ->
            val y = bottom - (height * fraction)
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (points.size == 1) {
            val y = bottom - height * (points.first() / 100f)
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(left + width / 2f, y)
            )
            return@Canvas
        }

        val path = Path()

        points.forEachIndexed { index, score ->
            val x = left + width * index / (points.size - 1f)
            val y = bottom - height * (score / 100f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx())
        )

        points.forEachIndexed { index, score ->
            val x = left + width * index / (points.size - 1f)
            val y = bottom - height * (score / 100f)
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun FocusHeatmap(
    sessions: List<FocusSessionEntity>,
    modifier: Modifier = Modifier
) {
    val cellMap = sessions
        .filter { it.focusScore != null }
        .groupBy {
            val dt = Instant.ofEpochMilli(it.startedAt)
                .atZone(ZoneId.systemDefault())
            (dt.dayOfWeek.value - 1) to dt.hour
        }
        .mapValues { (_, list) ->
            list.mapNotNull { it.focusScore }.average().toFloat()
        }

    val low = MaterialTheme.colorScheme.surfaceVariant
    val high = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Heatmap settimanale",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Intensità media del Focus Score per fascia oraria",
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(24) { hour ->
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(7) { day ->
                            val score = cellMap[day to hour] ?: 0f
                            val ratio = (score / 100f).coerceIn(0f, 1f)

                            Canvas(
                                Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            ) {
                                drawRoundRect(
                                    color = lerp(low, high, ratio),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00", style = MaterialTheme.typography.labelSmall)
                Text("06", style = MaterialTheme.typography.labelSmall)
                Text("12", style = MaterialTheme.typography.labelSmall)
                Text("18", style = MaterialTheme.typography.labelSmall)
                Text("23", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
