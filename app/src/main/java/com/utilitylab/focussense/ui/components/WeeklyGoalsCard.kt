package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.WeeklyProgress

@Composable
fun WeeklyGoalsCard(
    weekly: WeeklyProgress,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Obiettivi settimanali",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "${weekly.validSessions}/${weekly.sessionGoal} sessioni"
            )
            LinearProgressIndicator(
                progress = { weekly.sessionProgress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "${weekly.validMinutes}/${weekly.minuteGoal} minuti"
            )
            LinearProgressIndicator(
                progress = { weekly.minuteProgress },
                modifier = Modifier.fillMaxWidth()
            )

            if (weekly.complete) {
                Text(
                    "Obiettivi della settimana raggiunti",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
