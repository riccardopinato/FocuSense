package com.utilitylab.focussense.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FocuSenseNavy = Color(0xFF020B1D)
val FocuSenseNavySoft = Color(0xFF06152D)
val FocuSenseCardTop = Color(0xFF0C2850)
val FocuSenseCardBottom = Color(0xFF071A36)
val FocuSenseCyan = Color(0xFF24E5FF)
val FocuSenseBlue = Color(0xFF4A72FF)
val FocuSenseGreen = Color(0xFF52F2A5)
val FocuSensePurple = Color(0xFF9B7CFF)
val FocuSenseText = Color(0xFFF4F8FF)
val FocuSenseTextMuted = Color(0xFF9CB0D2)
val FocuSenseOutline = Color(0xFF315B91)
val FocuSenseDanger = Color(0xFFFF6F8E)

private val FocuSenseColors = darkColorScheme(
    primary = FocuSenseCyan,
    onPrimary = Color(0xFF00141B),
    primaryContainer = Color(0xFF083659),
    onPrimaryContainer = FocuSenseText,
    secondary = FocuSenseGreen,
    onSecondary = Color(0xFF002114),
    secondaryContainer = Color(0xFF073D35),
    onSecondaryContainer = FocuSenseText,
    tertiary = FocuSensePurple,
    onTertiary = Color.White,
    background = FocuSenseNavy,
    onBackground = FocuSenseText,
    surface = FocuSenseNavySoft,
    onSurface = FocuSenseText,
    surfaceVariant = FocuSenseCardBottom,
    onSurfaceVariant = FocuSenseTextMuted,
    outline = FocuSenseOutline,
    error = FocuSenseDanger
)

private val BaseTypography = Typography()
private val FocuSenseTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = BaseTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun FocusSenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FocuSenseColors,
        typography = FocuSenseTypography,
        content = content
    )
}
