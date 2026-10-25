package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.data.local.FocusSessionEntity
import com.utilitylab.focussense.domain.FocusCoreStage
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.AdaptiveBannerAd
import com.utilitylab.focussense.ui.components.BadgesGrid
import com.utilitylab.focussense.ui.components.FocusCoreVisual
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonPrimaryButton
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.components.ProgressCard
import com.utilitylab.focussense.ui.components.WeeklyGoalsCard
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SanctuaryScreen(vm: MainViewModel) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val progress by vm.gamificationProgress.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val monetization by vm.monetization.collectAsStateWithLifecycle()

    var sessionGoal by remember(prefs.weeklySessionGoal) {
        mutableIntStateOf(prefs.weeklySessionGoal)
    }
    var minuteGoal by remember(prefs.weeklyMinuteGoal) {
        mutableIntStateOf(prefs.weeklyMinuteGoal)
    }

    val cores = remember(sessions) {
        sessions
            .filter { FocusCoreStage.fromKey(it.coreStageKey) != FocusCoreStage.NONE }
            .sortedByDescending { it.startedAt }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonSectionHeader(
                title = "Sanctuary",
                subtitle = "Il tuo archivio permanente di concentrazione"
            )
        }

        item { ProgressCard(progress) }
        item { WeeklyGoalsCard(progress.weekly) }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseGreen
            ) {
                NeonSectionHeader(
                    title = "Focus Core",
                    subtitle = "${cores.size} risultati conservati"
                )

                if (cores.isEmpty()) {
                    Text(
                        "Completa una sessione per creare il primo Core.",
                        color = FocuSenseTextMuted
                    )
                } else {
                    cores.take(12).chunked(2).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { session ->
                                CoreTile(
                                    session = session,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Column(Modifier.weight(1f)) {}
                            }
                        }
                    }
                }
            }
        }

        item {
            AdaptiveBannerAd(
                enabled = monetization.adsEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseCyan
            ) {
                NeonSectionHeader(
                    title = "Obiettivi",
                    subtitle = "Imposta un ritmo sostenibile"
                )

                Text(
                    "Sessioni: $sessionGoal",
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuSenseText
                )
                Slider(
                    value = sessionGoal.toFloat(),
                    onValueChange = {
                        sessionGoal = it.toInt().coerceIn(1, 14)
                    },
                    valueRange = 1f..14f,
                    steps = 12
                )

                Text(
                    "Minuti: $minuteGoal",
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuSenseText
                )
                Slider(
                    value = minuteGoal.toFloat(),
                    onValueChange = {
                        minuteGoal =
                            ((it / 10f).toInt() * 10).coerceIn(30, 600)
                    },
                    valueRange = 30f..600f,
                    steps = 56
                )

                NeonPrimaryButton(
                    text = "Salva obiettivi",
                    subtitle = "$sessionGoal sessioni · $minuteGoal minuti",
                    onClick = {
                        vm.setWeeklyGoals(sessionGoal, minuteGoal)
                    },
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

@Composable
private fun CoreTile(
    session: FocusSessionEntity,
    modifier: Modifier = Modifier
) {
    val stage = FocusCoreStage.fromKey(session.coreStageKey)
    val date = Instant.ofEpochMilli(session.startedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM"))

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FocusCoreVisual(
            stage = stage,
            integrity = session.integrityScore ?: 0,
            size = 82.dp
        )
        Text(
            stage.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (stage == FocusCoreStage.PRISM) {
                FocuSenseGreen
            } else if (stage == FocusCoreStage.FRACTURED) {
                FocuSensePurple
            } else {
                FocuSenseText
            }
        )
        Text(
            "${session.integrityScore ?: 0}% · $date",
            style = MaterialTheme.typography.labelSmall,
            color = FocuSenseTextMuted
        )
    }
}
