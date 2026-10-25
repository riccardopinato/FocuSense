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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.FocusTrendChart
import com.utilitylab.focussense.ui.components.PermissionAwareStartButton
import com.utilitylab.focussense.ui.components.ProgressCard
import com.utilitylab.focussense.ui.components.SessionSetupDialog

@Composable
fun HomeScreen(vm: MainViewModel, openSession: () -> Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val progress by vm.gamificationProgress.collectAsStateWithLifecycle()
    val active by vm.activeSessionId.collectAsStateWithLifecycle()
    val error by vm.sessionError.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    var showSessionSetup by remember { mutableStateOf(false) }

    val completed = sessions.filter { it.focusScore != null }
    val avg = completed.mapNotNull { it.focusScore }.take(7).let {
        if (it.isEmpty()) 0 else it.average().toInt()
    }
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("FocuSense", style = MaterialTheme.typography.headlineLarge)
        Text("Capisci come rumore, luce e movimento si associano alle tue sessioni di concentrazione.")

        error?.let { message ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Sessione non avviata", style = MaterialTheme.typography.titleMedium)
                    Text(message)
                    TextButton(onClick = vm::clearSessionError) { Text("Chiudi") }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard("Media recente", "$avg/100", Modifier.weight(1f))
            MetricCard("Sessioni", completed.size.toString(), Modifier.weight(1f))
        }

        ProgressCard(progress)

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Nuova sessione", style = MaterialTheme.typography.titleLarge)
                Text("Durata predefinita: ${prefs.defaultSessionMinutes} min")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 50, 90).forEach { value ->
                        OutlinedButton(onClick = { vm.setSessionMinutes(value) }) {
                            Text("$value min")
                        }
                    }
                }
                if (active != null) {
                    OutlinedButton(onClick = openSession) {
                        Text("Torna alla sessione attiva")
                    }
                } else {
                    PermissionAwareStartButton(
                        enabled = true,
                        label = "Avvia analisi"
                    ) {
                        showSessionSetup = true
                    }
                }
            }
        }

        if (completed.isNotEmpty()) {
            Text("Andamento", style = MaterialTheme.typography.titleLarge)
            FocusTrendChart(
                sessions = completed,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
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

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
