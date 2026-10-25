package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.domain.FocusCoreStage
import com.utilitylab.focussense.domain.FocusStrictness
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.domain.SessionStatus
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.AdaptiveBannerAd
import com.utilitylab.focussense.ui.components.FocusCoreVisual
import com.utilitylab.focussense.ui.components.FocusScoreRing
import com.utilitylab.focussense.ui.components.FocusTrendChart
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonMetricCard
import com.utilitylab.focussense.ui.components.NeonOutlineButton
import com.utilitylab.focussense.ui.components.NeonPill
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.components.PermissionAwareStartButton
import com.utilitylab.focussense.ui.components.SessionSetupDialog
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseDanger
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun HomeScreen(vm: MainViewModel, openSession: () -> Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val progress by vm.gamificationProgress.collectAsStateWithLifecycle()
    val monetization by vm.monetization.collectAsStateWithLifecycle()
    val active by vm.activeSessionId.collectAsStateWithLifecycle()
    val error by vm.sessionError.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    var showSessionSetup by remember { mutableStateOf(false) }

    val completed = remember(sessions) {
        sessions
            .filter { it.endedAt != null }
            .sortedByDescending { it.startedAt }
    }
    val recent = completed.take(7)
    val avg = recent.mapNotNull { it.focusScore }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toInt()
        ?: 0
    val latest = completed.firstOrNull()
    val recentTags = remember(sessions) {
        sessions
            .flatMap { SessionTagCodec.decode(it.tagsCsv) }
            .distinctBy { it.lowercase() }
            .take(8)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "FocuSense",
                style = MaterialTheme.typography.headlineLarge,
                color = FocuSenseText
            )
            Text(
                "Lascia in pace il telefono. Scopri quando lavori meglio.",
                style = MaterialTheme.typography.bodyMedium,
                color = FocuSenseTextMuted
            )
        }

        error?.let { message ->
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseDanger
            ) {
                Text(
                    "Sessione non avviata",
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuSenseText
                )
                Text(message, color = FocuSenseTextMuted)
                NeonOutlineButton(
                    text = "Chiudi",
                    accent = FocuSenseDanger,
                    onClick = vm::clearSessionError
                )
            }
        }

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FocusScoreRing(
                score = avg,
                title = if (completed.isEmpty()) "Pronto a iniziare" else "Session Score",
                subtitle = when {
                    completed.isEmpty() -> "Crea il tuo primo Focus Core"
                    avg >= 85 -> "Ottimo equilibrio tra ambiente e disciplina"
                    avg >= 70 -> "Buona sessione recente"
                    else -> "Puoi proteggere meglio il tuo focus"
                }
            )
        }

        if (latest != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonMetricCard(
                    title = "Integrity",
                    value = latest.integrityScore?.let { "$it%" } ?: "—",
                    subtitle = "telefono",
                    accent = FocuSenseGreen,
                    modifier = Modifier.weight(1f)
                )
                NeonMetricCard(
                    title = "Ambiente",
                    value = latest.environmentScore?.let { "$it/100" } ?: "—",
                    subtitle = "luce + suono",
                    accent = FocuSenseCyan,
                    modifier = Modifier.weight(1f)
                )
                NeonMetricCard(
                    title = "Pickup",
                    value = latest.pickupCount.toString(),
                    subtitle = "ultima sessione",
                    accent = FocuSensePurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = FocuSenseCyan
        ) {
            NeonSectionHeader(
                title = if (active != null) "Focus in corso" else "Nuova sessione",
                subtitle = "${prefs.defaultSessionMinutes} min · ${prefs.defaultStrictness.label}"
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(25, 50, 90).forEach { value ->
                    NeonPill(
                        text = "$value min",
                        selected = prefs.defaultSessionMinutes == value,
                        onClick = { vm.setSessionMinutes(value) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (active != null) {
                NeonOutlineButton(
                    text = "Torna alla sessione",
                    onClick = openSession,
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSenseGreen
                )
            } else {
                PermissionAwareStartButton(
                    enabled = true,
                    label = "Avvia Focus Core",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    showSessionSetup = true
                }
            }
        }

        latest?.let { session ->
            val stage = FocusCoreStage.fromKey(session.coreStageKey)
            val status = SessionStatus.fromKey(session.sessionStatusKey)
            if (stage != FocusCoreStage.NONE) {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (status == SessionStatus.COMPLETED) {
                        FocuSenseGreen
                    } else {
                        FocuSensePurple
                    }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            NeonSectionHeader(
                                title = "Ultimo Focus Core",
                                subtitle = stage.label
                            )
                            Text(
                                "${session.growthPoints} crescita · ${session.actualDurationSeconds / 60} min",
                                color = FocuSenseTextMuted
                            )
                        }
                        FocusCoreVisual(
                            stage = stage,
                            integrity = session.integrityScore ?: 0
                        )
                    }
                }
            }
        }

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = FocuSenseGreen
        ) {
            NeonSectionHeader(
                title = "Sanctuary",
                subtitle = "${progress.totalCores} Core · livello ${progress.level.level}"
            )
            Text(
                "Ogni sessione completata lascia un Core permanente. Più proteggi il focus, più raro sarà il risultato.",
                color = FocuSenseTextMuted
            )
        }

        if (completed.isNotEmpty()) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSensePurple
            ) {
                NeonSectionHeader(
                    title = "Andamento",
                    subtitle = "Ultime ${recent.size} sessioni"
                )
                FocusTrendChart(
                    sessions = recent.reversed(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        AdaptiveBannerAd(
            enabled = monetization.adsEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        if (!monetization.premium) {
            Text(
                "La sessione di focus non contiene pubblicità.",
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseTextMuted
            )
        }

        Spacer(Modifier.height(12.dp))
    }

    if (showSessionSetup) {
        SessionSetupDialog(
            defaultMinutes = prefs.defaultSessionMinutes,
            defaultStrictness = prefs.defaultStrictness,
            defaultContext = SessionContext.WORK,
            recentTags = recentTags,
            onDismiss = { showSessionSetup = false },
            onStart = { minutes, sessionContext, tags, strictness ->
                showSessionSetup = false
                vm.setDefaultStrictness(strictness)
                vm.startSession(
                    context = localContext,
                    minutes = minutes,
                    sessionContext = sessionContext,
                    tags = tags,
                    strictness = strictness
                ) {
                    openSession()
                }
            }
        )
    }
}
