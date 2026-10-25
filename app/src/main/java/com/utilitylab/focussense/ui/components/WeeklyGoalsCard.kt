package com.utilitylab.focussense.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.utilitylab.focussense.domain.WeeklyProgress
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun WeeklyGoalsCard(
    weekly: WeeklyProgress,
    modifier: Modifier = Modifier
) {
    NeonCard(
        modifier = modifier,
        accent = if (weekly.complete) FocuSenseGreen else FocuSenseCyan
    ) {
        NeonSectionHeader(
            title = "Obiettivi settimanali",
            subtitle = if (weekly.complete) "Settimana completata" else "Continua con costanza"
        )

        Text(
            "${weekly.validSessions}/${weekly.sessionGoal} sessioni",
            style = MaterialTheme.typography.titleMedium
        )
        NeonProgressBar(weekly.sessionProgress)

        Text(
            "${weekly.validMinutes}/${weekly.minuteGoal} minuti",
            style = MaterialTheme.typography.titleMedium
        )
        NeonProgressBar(weekly.minuteProgress, accent = FocuSenseGreen)

        if (weekly.complete) {
            Text(
                "Obiettivi raggiunti",
                style = MaterialTheme.typography.labelLarge,
                color = FocuSenseGreen
            )
        } else {
            Text(
                "Il progresso viene calcolato solo sulle sessioni valide.",
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseTextMuted
            )
        }
    }
}
