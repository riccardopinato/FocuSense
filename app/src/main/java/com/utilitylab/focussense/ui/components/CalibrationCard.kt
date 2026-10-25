package com.utilitylab.focussense.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.utilitylab.focussense.domain.CalibrationLevel
import com.utilitylab.focussense.domain.PersonalFocusProfile
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun CalibrationCard(
    profile: PersonalFocusProfile,
    modifier: Modifier = Modifier
) {
    val label = when (profile.level) {
        CalibrationLevel.GENERIC -> "Profilo generico"
        CalibrationLevel.LEARNING -> "Calibrazione in corso"
        CalibrationLevel.PERSONALIZED -> "Profilo personale"
        CalibrationLevel.STRONG -> "Profilo personale stabile"
    }

    NeonCard(
        modifier = modifier,
        accent = FocuSensePurple
    ) {
        NeonSectionHeader(
            title = label,
            subtitle = "${profile.productiveSessions} sessioni produttive su ${profile.ratedSessions} valutate"
        )

        Text(
            "Affidabilità ${profile.confidencePercent}%",
            style = MaterialTheme.typography.titleMedium
        )
        NeonProgressBar(
            progress = profile.confidencePercent / 100f,
            accent = FocuSensePurple
        )

        if (profile.level == CalibrationLevel.GENERIC) {
            Text(
                "Valuta almeno 3 sessioni con focus 4-5/5 per iniziare la calibrazione personale.",
                color = FocuSenseTextMuted
            )
        } else {
            Text(
                "Rumore ${profile.targets.quietNoiseDbfs.toInt()} → ${profile.targets.loudNoiseDbfs.toInt()} dBFS" +
                    if (profile.personalizedNoise) " · personale" else " · standard",
                color = FocuSenseTextMuted
            )
            Text(
                "Luce ${profile.targets.preferredMinLightLux.toInt()}–${profile.targets.preferredMaxLightLux.toInt()} lux" +
                    if (profile.personalizedLight) " · personale" else " · standard",
                color = FocuSenseTextMuted
            )
            Text(
                "Movimento stabile ≤ ${profile.targets.stableMotionIndex.toInt()}/100" +
                    if (profile.personalizedMotion) " · personale" else " · standard",
                color = FocuSenseTextMuted
            )
        }
    }
}
