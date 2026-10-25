package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.BadgesGrid
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonPrimaryButton
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.components.ProgressCard
import com.utilitylab.focussense.ui.components.WeeklyGoalsCard
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen

@Composable
fun ProgressScreen(vm: MainViewModel) {
    val progress by vm.gamificationProgress.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()

    var sessionGoal by remember(prefs.weeklySessionGoal) {
        mutableIntStateOf(prefs.weeklySessionGoal)
    }
    var minuteGoal by remember(prefs.weeklyMinuteGoal) {
        mutableIntStateOf(prefs.weeklyMinuteGoal)
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonSectionHeader(
                title = "Progressi",
                subtitle = "Costanza, obiettivi e traguardi"
            )
        }

        item { ProgressCard(progress) }
        item { WeeklyGoalsCard(progress.weekly) }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseCyan
            ) {
                NeonSectionHeader(
                    title = "Personalizza obiettivi",
                    subtitle = "Imposta un ritmo realistico per la settimana"
                )

                Text("Sessioni: $sessionGoal", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = sessionGoal.toFloat(),
                    onValueChange = { sessionGoal = it.toInt().coerceIn(1, 14) },
                    valueRange = 1f..14f,
                    steps = 12
                )

                Text("Minuti: $minuteGoal", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = minuteGoal.toFloat(),
                    onValueChange = {
                        minuteGoal = ((it / 10f).toInt() * 10).coerceIn(30, 600)
                    },
                    valueRange = 30f..600f,
                    steps = 56
                )

                NeonPrimaryButton(
                    text = "Salva obiettivi",
                    subtitle = "$sessionGoal sessioni · $minuteGoal minuti",
                    onClick = { vm.setWeeklyGoals(sessionGoal, minuteGoal) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            BadgesGrid(
                unlocked = progress.unlockedBadges,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
