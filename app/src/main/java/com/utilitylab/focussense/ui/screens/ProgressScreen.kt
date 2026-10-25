package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.BadgesGrid
import com.utilitylab.focussense.ui.components.ProgressCard
import com.utilitylab.focussense.ui.components.WeeklyGoalsCard

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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Progressi",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        item {
            ProgressCard(progress)
        }

        item {
            WeeklyGoalsCard(progress.weekly)
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Personalizza obiettivi",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text("Sessioni: $sessionGoal")
                    Slider(
                        value = sessionGoal.toFloat(),
                        onValueChange = {
                            sessionGoal = it.toInt().coerceIn(1, 14)
                        },
                        valueRange = 1f..14f,
                        steps = 12
                    )

                    Text("Minuti: $minuteGoal")
                    Slider(
                        value = minuteGoal.toFloat(),
                        onValueChange = {
                            minuteGoal = ((it / 10f).toInt() * 10)
                                .coerceIn(30, 600)
                        },
                        valueRange = 30f..600f,
                        steps = 56
                    )

                    Button(
                        onClick = {
                            vm.setWeeklyGoals(
                                sessionGoal,
                                minuteGoal
                            )
                        }
                    ) {
                        Text("Salva obiettivi")
                    }
                }
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
