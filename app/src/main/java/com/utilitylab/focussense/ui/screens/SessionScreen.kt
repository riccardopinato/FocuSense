package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.ui.MainViewModel

@Composable
fun SessionScreen(vm: MainViewModel, activeSessionId: Long?, close: () -> Unit) {
    val live by vm.live.collectAsStateWithLifecycle()
    val error by vm.sessionError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sessionForRating = remember { mutableStateOf<Long?>(activeSessionId) }
    val showRating = remember { mutableStateOf(false) }

    LaunchedEffect(activeSessionId, error) {
        when {
            error != null -> showRating.value = false
            activeSessionId != null -> sessionForRating.value = activeSessionId
            sessionForRating.value != null -> {
                // Funziona sia con stop manuale sia con stop automatico alla fine della durata.
                showRating.value = true
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sessione live", style = MaterialTheme.typography.headlineLarge)

        if (error != null && activeSessionId == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sessione interrotta", style = MaterialTheme.typography.titleLarge)
                    Text(error ?: "Errore sconosciuto")
                    Button(onClick = {
                        vm.clearSessionError()
                        close()
                    }) { Text("Torna alla Home") }
                }
            }
            return@Column
        }

        ScoreCard(live.focusScore)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SensorCard(
                "Rumore",
                if (live.sensorsAvailable.microphone) "${live.noiseDbfs.toInt()} dBFS" else "Non disponibile",
                Modifier.weight(1f)
            )
            SensorCard(
                "Luce",
                if (live.sensorsAvailable.light) "${live.lightLux.toInt()} lux" else "Non disponibile",
                Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SensorCard(
                "Movimento",
                if (live.sensorsAvailable.accelerometer) "${live.motionIndex.toInt()}/100" else "Non disponibile",
                Modifier.weight(1f)
            )
            SensorCard(
                "Stabilità",
                when {
                    !live.sensorsAvailable.accelerometer -> "Non disponibile"
                    live.deviceStable -> "Stabile"
                    else -> "In movimento"
                },
                Modifier.weight(1f)
            )
        }

        if (live.sensorsAvailable.light && live.lightLux < 150f) {
            Text(
                "Luce bassa: prova ad aumentare l'illuminazione.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text("Ambiente monitorato in locale.", style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            "Il valore audio è relativo (dBFS), non è una misura fonica certificata.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "La sessione termina automaticamente alla durata impostata, oppure puoi fermarla prima.",
            style = MaterialTheme.typography.bodySmall
        )

        if (activeSessionId != null) {
            Button(onClick = { vm.stopSession(context) }) {
                Text("Termina sessione")
            }
        } else if (!showRating.value) {
            OutlinedButton(onClick = close) { Text("Torna alla Home") }
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
private fun ScoreCard(score: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Focus Score", style = MaterialTheme.typography.titleMedium)
            Text("$score/100", style = MaterialTheme.typography.displayMedium)
            Text("Indice indicativo basato sui sensori effettivamente disponibili.")
        }
    }
}

@Composable
private fun SensorCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
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
                androidx.compose.material3.Slider(
                    value = focus.intValue.toFloat(),
                    onValueChange = { focus.intValue = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Postura percepita: ${posture.intValue}/5")
                androidx.compose.material3.Slider(
                    value = posture.intValue.toFloat(),
                    onValueChange = { posture.intValue = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                androidx.compose.material3.OutlinedTextField(
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
            androidx.compose.material3.TextButton(onClick = onSkip) { Text("Salta") }
        }
    )
}
