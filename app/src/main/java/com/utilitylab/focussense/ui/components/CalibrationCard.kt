package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.*

@Composable
fun CalibrationCard(profile: PersonalFocusProfile, modifier: Modifier = Modifier) {
    val label=when(profile.level){ CalibrationLevel.GENERIC->"Profilo generico"; CalibrationLevel.LEARNING->"Calibrazione in corso"; CalibrationLevel.PERSONALIZED->"Profilo personale"; CalibrationLevel.STRONG->"Profilo personale stabile" }
    Card(modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label, style=MaterialTheme.typography.titleLarge)
        Text("Affidabilità ${profile.confidencePercent}% · ${profile.productiveSessions} sessioni produttive su ${profile.ratedSessions} valutate")
        LinearProgressIndicator(progress={profile.confidencePercent/100f}, modifier=Modifier.fillMaxWidth())
        if(profile.level==CalibrationLevel.GENERIC) Text("Valuta almeno 3 sessioni con focus 4-5/5 per iniziare la calibrazione personale.")
        else {
            Text("Rumore: ${profile.targets.quietNoiseDbfs.toInt()} → ${profile.targets.loudNoiseDbfs.toInt()} dBFS${if(profile.personalizedNoise) " · personale" else " · standard"}")
            Text("Luce: ${profile.targets.preferredMinLightLux.toInt()}–${profile.targets.preferredMaxLightLux.toInt()} lux${if(profile.personalizedLight) " · personale" else " · standard"}")
            Text("Movimento stabile ≤ ${profile.targets.stableMotionIndex.toInt()}/100${if(profile.personalizedMotion) " · personale" else " · standard"}")
            Text("Pesi: rumore ${(profile.weights.noise*100).toInt()}% · luce ${(profile.weights.light*100).toInt()}% · movimento ${(profile.weights.motion*100).toInt()}%")
        }
    }}
}
