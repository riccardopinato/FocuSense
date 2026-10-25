package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.utilitylab.focussense.domain.GamificationProgress
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun ProgressCard(
    progress: GamificationProgress,
    modifier: Modifier = Modifier
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        accent = FocuSenseGreen
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NeonSectionHeader(
                title = "Livello ${progress.level.level}",
                subtitle = "${progress.level.totalXp} XP"
            )
            Text(
                "${progress.currentStreak} giorni",
                style = MaterialTheme.typography.titleMedium,
                color = FocuSenseGreen
            )
        }

        NeonProgressBar(
            progress = progress.level.progress,
            accent = FocuSenseGreen
        )

        Text(
            "Prossimo livello a ${progress.level.nextLevelXp} XP",
            style = MaterialTheme.typography.bodySmall,
            color = FocuSenseTextMuted
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Serie migliore", color = FocuSenseTextMuted)
            Text("${progress.bestStreak} giorni")
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Sessioni valide", color = FocuSenseTextMuted)
            Text("${progress.validSessions}")
        }
    }
}
