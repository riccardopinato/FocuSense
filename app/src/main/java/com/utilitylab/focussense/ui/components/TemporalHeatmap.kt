package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.DayPart
import com.utilitylab.focussense.domain.HeatmapCell
import com.utilitylab.focussense.domain.TemporalInsightsEngine
import com.utilitylab.focussense.ui.theme.FocuSenseCardBottom
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSenseOutline
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted
import java.time.DayOfWeek

@Composable
fun TemporalHeatmap(
    cells: List<HeatmapCell>,
    modifier: Modifier = Modifier
) {
    val map = cells.associateBy { it.dayOfWeek to it.dayPart }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Spacer(Modifier.width(34.dp))
            DayPart.entries.forEach {
                Text(
                    text = when (it) {
                        DayPart.MORNING -> "M"
                        DayPart.AFTERNOON -> "P"
                        DayPart.EVENING -> "S"
                        DayPart.NIGHT -> "N"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = FocuSenseTextMuted,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        DayOfWeek.entries.forEach { day ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    TemporalInsightsEngine.italianDay(day).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = FocuSenseTextMuted,
                    modifier = Modifier.width(34.dp)
                )

                DayPart.entries.forEach { part ->
                    val cell = map[day to part]
                    val score = cell?.averageFocusScore
                    val color = heatColor(score)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(color, RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (score == null) FocuSenseOutline.copy(alpha = .2f)
                                else FocuSenseCyan.copy(alpha = .25f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cell == null || cell.sessions == 0) "—" else "${score ?: "—"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (score != null && score >= 70)
                                FocuSenseGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            "M mattina · P pomeriggio · S sera · N notte",
            style = MaterialTheme.typography.bodySmall,
            color = FocuSenseTextMuted
        )
    }
}

private fun heatColor(score: Int?): Color {
    if (score == null) return FocuSenseCardBottom.copy(alpha = .65f)
    val strength = score.coerceIn(0, 100) / 100f
    return lerp(
        FocuSenseCardBottom,
        FocuSenseCyan.copy(alpha = 0.82f),
        strength * 0.72f
    )
}
