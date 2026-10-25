package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.domain.DayPart
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.CalibrationCard
import com.utilitylab.focussense.ui.components.FocusTrendChart
import com.utilitylab.focussense.ui.components.TemporalHeatmap

@Composable
fun AnalyticsScreen(
    vm: MainViewModel,
    onOpenBestConditions: () -> Unit = {}
) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val profile by vm.personalProfile.collectAsStateWithLifecycle()
    val temporal by vm.temporalInsights.collectAsStateWithLifecycle()
    val contexts by vm.contextAnalytics.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refreshPersonalProfile() }

    val completed = sessions.filter { it.endedAt != null }
    val avg = completed.mapNotNull { it.focusScore }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toInt()

    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Analisi", style = MaterialTheme.typography.headlineLarge) }

        item {
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBestConditions() }
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Condizioni migliori", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Scopri quali combinazioni di ambiente, contesto e orario ricorrono più spesso nelle tue sessioni migliori."
                    )
                    Text("Apri analisi", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item { CalibrationCard(profile) }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Focus medio", style = MaterialTheme.typography.titleMedium)
                    Text(
                        avg?.let { "$it/100" } ?: "—",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        "Il punteggio descrive le condizioni rilevate dal telefono, non misura direttamente la produttività."
                    )
                }
            }
        }

        if (completed.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Trend sessioni", style = MaterialTheme.typography.titleLarge)
                        FocusTrendChart(
                            completed.take(20).reversed(),
                            Modifier.fillMaxWidth().height(240.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Focus per contesto", style = MaterialTheme.typography.titleLarge)
                    contexts.buckets.forEach { bucket ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(bucket.context.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${bucket.sessions} sessioni",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                bucket.averageFocusScore?.let { "$it/100" } ?: "—",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Focus per fascia oraria", style = MaterialTheme.typography.titleLarge)
                    DayPart.entries.forEach { part ->
                        val b = temporal.byDayPart[part]
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(part.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${b?.sessions ?: 0} sessioni",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                b?.averageFocusScore?.let { "$it/100" } ?: "—",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Heatmap settimanale", style = MaterialTheme.typography.titleLarge)
                    TemporalHeatmap(temporal.heatmap, Modifier.fillMaxWidth())
                }
            }
        }

        temporal.recentTrend?.let { trend ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("Ultime 7 vs precedenti 7", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${trend.recentAverage}/100 vs ${trend.previousAverage}/100",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        val prefix = if (trend.delta > 0) "+" else ""
                        Text("$prefix${trend.delta} punti")
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
                    Text("Pattern per attività", style = MaterialTheme.typography.titleLarge)
                    contexts.messages.forEach { Text("• $it") }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pattern temporali", style = MaterialTheme.typography.titleLarge)
                    temporal.messages.forEach { Text("• $it") }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Come leggere questi dati", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "I confronti descrivono ciò che è successo nelle tue sessioni. " +
                            "FocuSense evita conclusioni con campioni insufficienti e non interpreta le associazioni come prova di causalità."
                    )
                    Text(
                        "La calibrazione personale usa le sessioni che valuti 4-5/5; gli insight temporali e contestuali riassumono lo storico completato."
                    )
                }
            }
        }
    }
}
