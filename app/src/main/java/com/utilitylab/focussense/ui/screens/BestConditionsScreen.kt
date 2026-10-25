package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonMetricCard
import com.utilitylab.focussense.ui.components.NeonProgressBar
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted
import kotlin.math.roundToInt

@Composable
fun BestConditionsScreen(vm: MainViewModel) {
    val profile by vm.bestConditions.collectAsStateWithLifecycle()
    val match by vm.environmentMatch.collectAsStateWithLifecycle()
    val activeSession by vm.activeSessionId.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonSectionHeader(
                title = "Condizioni migliori",
                subtitle = "Pattern delle sessioni che hai valutato meglio"
            )
        }

        if (!profile.ready) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSensePurple
                ) {
                    Text("Sto imparando", style = MaterialTheme.typography.titleLarge, color = FocuSenseText)
                    Text(
                        "Servono almeno 5 sessioni da almeno 10 minuti con una tua valutazione del focus.",
                        color = FocuSenseTextMuted
                    )
                    NeonProgressBar(
                        progress = (profile.analyzedSessions / 5f).coerceIn(0f, 1f),
                        accent = FocuSensePurple
                    )
                    Text(
                        "${profile.analyzedSessions}/5 sessioni",
                        color = FocuSensePurple
                    )
                }
            }
            return@LazyColumn
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonMetricCard(
                    title = "Affidabilità",
                    value = "${profile.confidencePercent}%",
                    subtitle = "${profile.sampleSize} migliori",
                    accent = FocuSenseGreen,
                    modifier = Modifier.weight(1f)
                )
                NeonMetricCard(
                    title = "Analizzate",
                    value = profile.analyzedSessions.toString(),
                    subtitle = "sessioni",
                    accent = FocuSenseCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (activeSession != null && match.score != null) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSenseGreen
                ) {
                    Text("Ambiente attuale", style = MaterialTheme.typography.titleLarge, color = FocuSenseText)
                    Text(
                        "${match.score}%",
                        style = MaterialTheme.typography.displaySmall,
                        color = FocuSenseGreen
                    )
                    NeonProgressBar(match.score!! / 100f, accent = FocuSenseGreen)
                    Text(
                        "Compatibilità di rumore e luce con le condizioni ricorrenti nelle sessioni che hai valutato meglio.",
                        color = FocuSenseTextMuted
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        match.noiseScore?.let { Text("Rumore $it", color = FocuSenseCyan) }
                        match.lightScore?.let { Text("Luce $it", color = FocuSenseGreen) }
                    }
                }
            }
        }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseCyan
            ) {
                NeonSectionHeader(
                    title = "Profilo personale osservato",
                    subtitle = "Range ricavati dai rating 4–5/5 quando disponibili"
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
                    MetricRow("Movimento tipico", "≤ ${it.roundToInt()}/100")
                }
                profile.typicalStablePercent?.let {
                    MetricRow("Stabilità tipica", "${it.roundToInt()}%")
                }
                profile.typicalInterruptionMax?.let {
                    MetricRow("Interruzioni", "≤ $it")
                }
            }
        }

        if (profile.messages.isNotEmpty()) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSensePurple
                ) {
                    NeonSectionHeader(title = "Insight personalizzati")
                    profile.messages.forEach {
                        Text("• $it", color = FocuSenseTextMuted)
                    }
                }
            }
        }

        item {
            Text(
                "Le condizioni migliori descrivono associazioni nello storico, non cause. " +
                    "FocuSense non diagnostica attenzione o produttività.",
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseTextMuted
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = FocuSenseTextMuted)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}
