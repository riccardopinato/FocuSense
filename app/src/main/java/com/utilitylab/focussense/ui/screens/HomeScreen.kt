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
import androidx.compose.foundation.layout.weight
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
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.FocusScoreRing
import com.utilitylab.focussense.ui.components.FocusTrendChart
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonMetricCard
import com.utilitylab.focussense.ui.components.NeonOutlineButton
import com.utilitylab.focussense.ui.components.NeonPill
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.components.PermissionAwareStartButton
import com.utilitylab.focussense.ui.components.ProgressCard
import com.utilitylab.focussense.ui.components.SessionSetupDialog
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseDanger
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun HomeScreen(vm: MainViewModel, openSession: () -> Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val progress by vm.gamificationProgress.collectAsStateWithLifecycle()
    val active by vm.activeSessionId.collectAsStateWithLifecycle()
    val error by vm.sessionError.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    var showSessionSetup by remember { mutableStateOf(false) }

    val completed = remember(sessions) {
        sessions
            .filter { it.focusScore != null }
            .sortedByDescending { it.startedAt }
    }
    val recent = completed.take(7)
    val avg = if (recent.isEmpty()) 0 else recent.mapNotNull { it.focusScore }.average().toInt()
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
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                "Il tuo ambiente, tradotto in condizioni di focus.",
                style = MaterialTheme.typography.bodyMedium,
                color = FocuSenseTextMuted
            )
        }

        error?.let { message ->
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseDanger
            ) {
                Text("Sessione non avviata", style = MaterialTheme.typography.titleMedium)
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
                title = if (completed.isEmpty()) "Pronto a iniziare" else "Focus Score",
                subtitle = when {
                    completed.isEmpty() -> "Completa la prima sessione"
                    avg >= 75 -> "Condizioni recenti favorevoli"
                    avg >= 50 -> "Condizioni recenti discrete"
                    else -> "C'è margine per migliorare l'ambiente"
                }
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonMetricCard(
                title = "Rumore",
                value = latest?.avgNoiseDbfs?.let { "${it.toInt()} dBFS" } ?: "—",
                subtitle = "ultimo dato",
                accent = FocuSenseCyan,
                modifier = Modifier.weight(1f)
            )
            NeonMetricCard(
                title = "Luce",
                value = latest?.avgLightLux?.let { "${it.toInt()} lux" } ?: "—",
                subtitle = "ultimo dato",
                accent = FocuSenseGreen,
                modifier = Modifier.weight(1f)
            )
            NeonMetricCard(
                title = "Stabilità",
                value = latest?.stablePercent?.let { "${it.toInt()}%" } ?: "—",
                subtitle = "telefono",
                accent = FocuSensePurple,
                modifier = Modifier.weight(1f)
            )
        }

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = FocuSenseCyan
        ) {
            NeonSectionHeader(
                title = if (active != null) "Sessione in corso" else "Nuova sessione",
                subtitle = "Durata predefinita ${prefs.defaultSessionMinutes} min"
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
                    label = "Avvia sessione",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    showSessionSetup = true
                }
            }
        }

        ProgressCard(progress)

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

        Spacer(Modifier.height(12.dp))
    }

    if (showSessionSetup) {
        SessionSetupDialog(
            defaultMinutes = prefs.defaultSessionMinutes,
            defaultContext = SessionContext.WORK,
            recentTags = recentTags,
            onDismiss = { showSessionSetup = false },
            onStart = { minutes, sessionContext, tags ->
                showSessionSetup = false
                vm.startSession(
                    context = localContext,
                    minutes = minutes,
                    sessionContext = sessionContext,
                    tags = tags
                ) { openSession() }
            }
        )
    }
}
