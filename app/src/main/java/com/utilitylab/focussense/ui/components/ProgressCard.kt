package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.GamificationProgress

@Composable
fun ProgressCard(
    progress: GamificationProgress,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Progressi",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Livello ${progress.level.level}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    "${progress.currentStreak} giorni",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            LinearProgressIndicator(
                progress = { progress.level.progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "${progress.level.totalXp} XP · pross. livello ${progress.level.nextLevelXp} XP",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Serie migliore")
                Text("${progress.bestStreak} giorni")
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sessioni valide")
                Text("${progress.validSessions}")
            }
        }
    }
}
