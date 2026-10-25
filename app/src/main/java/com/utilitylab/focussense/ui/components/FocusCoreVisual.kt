package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.FocusCoreStage
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FocusCoreVisual(
    stage: FocusCoreStage,
    integrity: Int,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val accent = when (stage) {
        FocusCoreStage.PRISM -> FocuSenseGreen
        FocusCoreStage.NOVA -> FocuSenseCyan
        FocusCoreStage.CRYSTAL -> FocuSensePurple
        FocusCoreStage.ORB -> Color(0xFFA8C7FF)
        FocusCoreStage.SPARK -> Color(0xFFB6F3FF)
        FocusCoreStage.FRACTURED -> Color(0xFF8793A8)
        FocusCoreStage.NONE -> Color(0xFF526078)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension * 0.28f

            drawCircle(
                color = accent.copy(alpha = 0.08f),
                radius = radius * 1.7f,
                center = center
            )
            drawCircle(
                color = accent.copy(alpha = 0.16f),
                radius = radius * 1.3f,
                center = center
            )

            when (stage) {
                FocusCoreStage.NONE -> {
                    drawCircle(
                        color = accent.copy(alpha = 0.45f),
                        radius = radius * 0.38f,
                        center = center
                    )
                }

                FocusCoreStage.SPARK -> {
                    drawCircle(
                        color = accent,
                        radius = radius * 0.45f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = radius * 0.12f,
                        center = center
                    )
                }

                FocusCoreStage.ORB -> {
                    drawCircle(
                        color = accent.copy(alpha = 0.34f),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = accent,
                        radius = radius * 0.68f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                FocusCoreStage.CRYSTAL,
                FocusCoreStage.NOVA,
                FocusCoreStage.PRISM -> {
                    val points = when (stage) {
                        FocusCoreStage.CRYSTAL -> 6
                        FocusCoreStage.NOVA -> 8
                        else -> 10
                    }
                    val path = Path()
                    repeat(points) { index ->
                        val angle = -Math.PI / 2 + (Math.PI * 2 * index / points)
                        val r = if (index % 2 == 0) radius else radius * 0.68f
                        val p = Offset(
                            center.x + cos(angle).toFloat() * r,
                            center.y + sin(angle).toFloat() * r
                        )
                        if (index == 0) path.moveTo(p.x, p.y)
                        else path.lineTo(p.x, p.y)
                    }
                    path.close()
                    drawPath(path, color = accent.copy(alpha = 0.44f))
                    drawPath(
                        path,
                        color = accent,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.88f),
                        radius = radius * 0.13f,
                        center = center
                    )
                }

                FocusCoreStage.FRACTURED -> {
                    drawCircle(
                        color = accent.copy(alpha = 0.26f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    val crack = Path().apply {
                        moveTo(center.x - radius * .55f, center.y - radius * .5f)
                        lineTo(center.x - radius * .08f, center.y - radius * .08f)
                        lineTo(center.x - radius * .30f, center.y + radius * .22f)
                        lineTo(center.x + radius * .50f, center.y + radius * .60f)
                    }
                    drawPath(
                        crack,
                        color = accent.copy(alpha = .9f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            val healthAlpha = (integrity.coerceIn(0, 100) / 100f)
                .coerceIn(.25f, 1f)
            drawCircle(
                color = accent.copy(alpha = healthAlpha * .55f),
                radius = radius * 1.12f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
