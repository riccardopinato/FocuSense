package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.ui.theme.FocuSenseBlue
import com.utilitylab.focussense.ui.theme.FocuSenseCardBottom
import com.utilitylab.focussense.ui.theme.FocuSenseCardTop
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSenseNavy
import com.utilitylab.focussense.ui.theme.FocuSenseNavySoft
import com.utilitylab.focussense.ui.theme.FocuSenseOutline
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun FocuSenseBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF061B3B),
                        FocuSenseNavySoft,
                        FocuSenseNavy
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = FocuSenseBlue.copy(alpha = 0.12f),
                radius = size.width * 0.62f,
                center = Offset(size.width * 0.84f, size.height * 0.06f)
            )
            drawCircle(
                color = FocuSenseCyan.copy(alpha = 0.06f),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.08f, size.height * 0.62f)
            )
        }
        content()
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    accent: Color = FocuSenseCyan,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        FocuSenseCardTop.copy(alpha = 0.94f),
                        FocuSenseCardBottom.copy(alpha = 0.96f)
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.24f), shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun NeonMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = FocuSenseCyan
) {
    NeonCard(
        modifier = modifier,
        accent = accent,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(
            Modifier
                .width(30.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(accent)
        )
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = FocuSenseTextMuted
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}

@Composable
fun NeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(28.dp)
    val alpha = if (enabled) 1f else 0.42f
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        FocuSenseBlue.copy(alpha = alpha),
                        FocuSenseCyan.copy(alpha = alpha),
                        FocuSenseGreen.copy(alpha = alpha)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.48f * alpha), shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NeonOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = FocuSenseCyan,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(FocuSenseCardBottom.copy(alpha = 0.72f))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.65f else 0.25f), shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else FocuSenseTextMuted
        )
    }
}

@Composable
fun NeonPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = FocuSenseCyan
) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) accent.copy(alpha = 0.16f)
                else FocuSenseCardBottom.copy(alpha = 0.62f)
            )
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.75f) else FocuSenseOutline.copy(alpha = 0.45f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else FocuSenseTextMuted
        )
    }
}

@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = FocuSenseCyan
) {
    val safe = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(FocuSenseOutline.copy(alpha = 0.24f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(safe)
                .background(
                    Brush.horizontalGradient(
                        listOf(FocuSenseBlue, accent, FocuSenseGreen)
                    )
                )
        )
    }
}

@Composable
fun FocusScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    title: String = "Focus Score",
    subtitle: String? = null
) {
    val safe = score.coerceIn(0, 100)
    val accent = when {
        safe >= 75 -> FocuSenseGreen
        safe >= 50 -> FocuSenseCyan
        else -> FocuSensePurple
    }

    Box(
        modifier = modifier.size(224.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            inset(stroke) {
                val diameter = min(size.width, size.height)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = FocuSenseOutline.copy(alpha = 0.28f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = accent.copy(alpha = 0.18f),
                    startAngle = 135f,
                    sweepAngle = 270f * safe / 100f,
                    useCenter = false,
                    size = arcSize,
                    style = Stroke(width = stroke * 1.65f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = accent,
                    startAngle = 135f,
                    sweepAngle = 270f * safe / 100f,
                    useCenter = false,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                val endAngle = Math.toRadians((135f + 270f * safe / 100f).toDouble())
                val radius = diameter / 2f
                val center = Offset(diameter / 2f, diameter / 2f)
                val dot = Offset(
                    center.x + cos(endAngle).toFloat() * radius,
                    center.y + sin(endAngle).toFloat() * radius
                )
                drawCircle(accent, radius = 4.dp.toPx(), center = dot)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = FocuSenseTextMuted
            )
            Text(
                safe.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "/100",
                style = MaterialTheme.typography.labelMedium,
                color = FocuSenseTextMuted
            )
            subtitle?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun NeonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseTextMuted
            )
        }
    }
}
