package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.ui.MainViewModel
import kotlin.math.roundToInt

@Composable
fun BestConditionsScreen(
    vm: MainViewModel
) {
    val profile by vm.bestConditions.collectAsStateWithLifecycle()
    val match by vm.environmentMatch.collectAsStateWithLifecycle()
    val activeSession by vm.activeSessionId.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Condizioni migliori",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        if (!profile.ready) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Dati insufficienti",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Servono almeno 5 sessioni valide di almeno 10 minuti."
                        )
                        LinearProgressIndicator(
                            progress = {
                                (profile.analyzedSessions / 5f).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${profile.analyzedSessions}/5 sessioni")
                    }
                }
            }
            return@LazyColumn
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Affidabilità",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "${profile.confidencePercent}%",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        "${profile.sampleSize} sessioni migliori usate su ${profile.analyzedSessions} analizzate."
                    )
                }
            }
        }

        if (activeSession != null && match.score != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Ambiente attuale",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${match.score}/100",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            "Compatibilità tra le condizioni attuali e quelle osservate nelle tue sessioni migliori."
                        )
                        match.noiseScore?.let { Text("Rumore: $it/100") }
                        match.lightScore?.let { Text("Luce: $it/100") }
                        match.motionScore?.let { Text("Movimento: $it/100") }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Profilo delle sessioni migliori",
                        style = MaterialTheme.typography.titleLarge
                    )

                    profile.strongestContext?.let {
                        MetricRow("Contesto", it.label)
                    }

                    profile.strongestDayPart?.let {
                        MetricRow("Fascia oraria", it.label)
                    }

                    profile.preferredNoiseRangeDbfs?.let {
                        MetricRow(
                            "Rumore",
                            "${it.min.roundToInt()} – ${it.max.roundToInt()} dBFS"
                        )
                    }

                    profile.preferredLightRangeLux?.let {
                        MetricRow(
                            "Luce",
                            "${it.min.roundToInt()} – ${it.max.roundToInt()} lux"
                        )
                    }

                    profile.preferredMotionMax?.let {
                        MetricRow(
                            "Movimento",
                            "≤ ${it.roundToInt()}/100"
                        )
                    }

                    profile.typicalStablePercent?.let {
                        MetricRow(
                            "Stabilità tipica",
                            "${it.roundToInt()}%"
                        )
                    }

                    profile.typicalInterruptionMax?.let {
                        MetricRow(
                            "Interruzioni",
                            "≤ $it"
                        )
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Pattern osservati",
                        style = MaterialTheme.typography.titleLarge
                    )
                    profile.messages.forEach {
                        Text("• $it")
                    }
                }
            }
        }

        item {
            Text(
                "Le condizioni migliori sono ricavate dallo storico personale e descrivono associazioni, non cause. " +
                    "FocuSense non usa questi dati per diagnosticare attenzione o produttività.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
