package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.FocusScoreRing
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonMetricCard
import com.utilitylab.focussense.ui.components.NeonOutlineButton
import com.utilitylab.focussense.ui.components.NeonPrimaryButton
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseDanger
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun SessionScreen(vm: MainViewModel, activeSessionId: Long?, close: () -> Unit) {
    val live by vm.live.collectAsStateWithLifecycle()
    val match by vm.environmentMatch.collectAsStateWithLifecycle()
    val error by vm.sessionError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sessionForRating = remember { mutableStateOf<Long?>(activeSessionId) }
    val showRating = remember { mutableStateOf(false) }

    LaunchedEffect(activeSessionId, error) {
        when {
            error != null -> showRating.value = false
            activeSessionId != null -> sessionForRating.value = activeSessionId
            sessionForRating.value != null -> showRating.value = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeonSectionHeader(
            title = "Sessione live",
            subtitle = if (activeSessionId != null) "Monitoraggio attivo sul dispositivo" else "Sessione terminata"
        )

        if (error != null && activeSessionId == null) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseDanger
            ) {
                Text("Sessione interrotta", style = MaterialTheme.typography.titleLarge)
                Text(error ?: "Errore sconosciuto", color = FocuSenseTextMuted)
                NeonPrimaryButton(
                    text = "Torna alla Home",
                    onClick = {
                        vm.clearSessionError()
                        close()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@Column
        }

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FocusScoreRing(
                score = live.focusScore,
                title = "Focus live",
                subtitle = match.score?.let { "Ambiente $it% allineato" } ?: "Sensori in ascolto"
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonMetricCard(
                "Rumore",
                if (live.sensorsAvailable.microphone) "${live.noiseDbfs.toInt()} dBFS" else "N/D",
                Modifier.weight(1f),
                accent = FocuSenseCyan
            )
            NeonMetricCard(
                "Luce",
                if (live.sensorsAvailable.light) "${live.lightLux.toInt()} lux" else "N/D",
                Modifier.weight(1f),
                accent = FocuSenseGreen
            )
            NeonMetricCard(
                "Movimento",
                if (live.sensorsAvailable.accelerometer) "${live.motionIndex.toInt()}" else "N/D",
                Modifier.weight(1f),
                accent = FocuSensePurple
            )
        }

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = if (live.deviceStable) FocuSenseGreen else FocuSensePurple
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Stato dispositivo", color = FocuSenseTextMuted)
                Text(
                    when {
                        !live.sensorsAvailable.accelerometer -> "Sensore non disponibile"
                        live.deviceStable -> "Stabile"
                        else -> "In movimento"
                    },
                    color = if (live.deviceStable) FocuSenseGreen else FocuSensePurple
                )
            }
            Text(
                when {
                    live.sensorsAvailable.light && live.lightLux < 150f ->
                        "La luce è bassa rispetto a un ambiente ben illuminato."
                    else -> "I dati rimangono sul dispositivo durante il monitoraggio."
                },
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseTextMuted
            )
        }

        Text(
            "Il valore audio è relativo (dBFS), non è una misura fonica certificata.",
            style = MaterialTheme.typography.bodySmall,
            color = FocuSenseTextMuted
        )

        if (activeSessionId != null) {
            NeonPrimaryButton(
                text = "Termina sessione",
                subtitle = "Salva il riepilogo e passa alla valutazione",
                onClick = { vm.stopSession(context) },
                modifier = Modifier.fillMaxWidth()
            )
        } else if (!showRating.value) {
            NeonOutlineButton(
                text = "Torna alla Home",
                onClick = close,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showRating.value) {
        RatingDialog(
            onSave = { focus, posture, note ->
                sessionForRating.value?.let { vm.saveRatings(it, focus, posture, note) }
                sessionForRating.value = null
                showRating.value = false
                close()
            },
            onSkip = {
                sessionForRating.value = null
                showRating.value = false
                close()
            }
        )
    }
}

@Composable
private fun RatingDialog(onSave: (Int, Int, String) -> Unit, onSkip: () -> Unit) {
    val focus = remember { mutableIntStateOf(4) }
    val posture = remember { mutableIntStateOf(3) }
    val note = remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Com'è andata?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Focus percepito: ${focus.intValue}/5")
                Slider(
                    value = focus.intValue.toFloat(),
                    onValueChange = { focus.intValue = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Postura percepita: ${posture.intValue}/5")
                Slider(
                    value = posture.intValue.toFloat(),
                    onValueChange = { posture.intValue = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                OutlinedTextField(
                    value = note.value,
                    onValueChange = { note.value = it },
                    label = { Text("Nota facoltativa") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(focus.intValue, posture.intValue, note.value) }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Salta") }
        }
    )
}
