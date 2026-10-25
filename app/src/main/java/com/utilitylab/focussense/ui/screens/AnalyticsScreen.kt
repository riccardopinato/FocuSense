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
import com.utilitylab.focussense.ui.components.AdaptiveBannerAd
import com.utilitylab.focussense.ui.components.CalibrationCard
import com.utilitylab.focussense.ui.components.FocusTrendChart
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonMetricCard
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.components.TemporalHeatmap
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun AnalyticsScreen(
    vm: MainViewModel,
    onOpenBestConditions: () -> Unit = {}
) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val profile by vm.personalProfile.collectAsStateWithLifecycle()
    val temporal by vm.temporalInsights.collectAsStateWithLifecycle()
    val contexts by vm.contextAnalytics.collectAsStateWithLifecycle()
    val monetization by vm.monetization.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refreshPersonalProfile() }

    val completed = sessions.filter { it.endedAt != null }
    val environmentAvg = completed.mapNotNull { it.environmentScore }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toInt()
    val integrityAvg = completed.mapNotNull { it.integrityScore }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toInt()
    val selfAvg = completed.mapNotNull { it.selfFocusRating }
        .takeIf { it.isNotEmpty() }
        ?.average()
    val avgPickups = completed
        .takeIf { it.isNotEmpty() }
        ?.map { it.pickupCount }
        ?.average()
    val avgBreaks = completed
        .takeIf { it.isNotEmpty() }
        ?.map { it.foregroundBreakCount }
        ?.average()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonSectionHeader(
                title = "Analisi",
                subtitle = "Ambiente, disciplina e focus percepito restano separati"
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonMetricCard(
                    title = "Integrity",
                    value = integrityAvg?.let { "$it%" } ?: "—",
                    modifier = Modifier.weight(1f),
                    subtitle = "uso telefono",
                    accent = FocuSenseGreen
                )
                NeonMetricCard(
                    title = "Ambiente",
                    value = environmentAvg?.let { "$it/100" } ?: "—",
                    modifier = Modifier.weight(1f),
                    subtitle = "luce + suono",
                    accent = FocuSenseCyan
                )
                NeonMetricCard(
                    title = "Percepito",
                    value = selfAvg?.let { "%.1f/5".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                    subtitle = "tuo rating",
                    accent = FocuSensePurple
                )
            }
        }

        item {
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBestConditions() },
                accent = FocuSenseGreen
            ) {
                Text(
                    "Condizioni migliori",
                    style = MaterialTheme.typography.titleLarge,
                    color = FocuSenseGreen
                )
                Text(
                    "Rumore, luce, contesto e orario vengono confrontati con le sessioni che tu hai valutato meglio, evitando di usare il punteggio automatico come prova di se stesso.",
                    color = FocuSenseTextMuted
                )
                Text(
                    "APRI PROFILO PERSONALE →",
                    style = MaterialTheme.typography.labelLarge,
                    color = FocuSenseCyan
                )
            }
        }

        item { CalibrationCard(profile) }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSensePurple
            ) {
                NeonSectionHeader(
                    title = "Disciplina telefono",
                    subtitle = "Quanto il dispositivo interrompe le sessioni"
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pickup medi", color = FocuSenseTextMuted)
                    Text(
                        avgPickups?.let { "%.1f".format(it) } ?: "—",
                        color = FocuSensePurple
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Uscite dall'app medie", color = FocuSenseTextMuted)
                    Text(
                        avgBreaks?.let { "%.1f".format(it) } ?: "—",
                        color = FocuSensePurple
                    )
                }
                Text(
                    "Schermo spento durante il focus non viene conteggiato come distrazione.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuSenseTextMuted
                )
            }
        }

        item {
            AdaptiveBannerAd(
                enabled = monetization.adsEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (completed.isNotEmpty()) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSensePurple
                ) {
                    NeonSectionHeader(
                        title = "Trend sessioni",
                        subtitle = "Session Score = 60% Integrity + 40% ambiente"
                    )
                    FocusTrendChart(
                        completed.take(20).reversed(),
                        Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    )
                }
            }
        }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseGreen
            ) {
                NeonSectionHeader(title = "Session Score per contesto")
                contexts.buckets.forEach { bucket ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                bucket.context.label,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${bucket.sessions} sessioni",
                                style = MaterialTheme.typography.bodySmall,
                                color = FocuSenseTextMuted
                            )
                        }
                        Text(
                            bucket.averageFocusScore?.let { "$it/100" } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = FocuSenseGreen
                        )
                    }
                }
            }
        }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseCyan
            ) {
                NeonSectionHeader(title = "Session Score per fascia oraria")
                DayPart.entries.forEach { part ->
                    val bucket = temporal.byDayPart[part]
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                part.label,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${bucket?.sessions ?: 0} sessioni",
                                style = MaterialTheme.typography.bodySmall,
                                color = FocuSenseTextMuted
                            )
                        }
                        Text(
                            bucket?.averageFocusScore?.let { "$it/100" } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = FocuSenseCyan
                        )
                    }
                }
            }
        }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSensePurple
            ) {
                NeonSectionHeader(
                    title = "Heatmap settimanale",
                    subtitle = "Quando le sessioni hanno avuto score migliori"
                )
                TemporalHeatmap(
                    temporal.heatmap,
                    Modifier.fillMaxWidth()
                )
            }
        }

        temporal.recentTrend?.let { trend ->
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (trend.delta >= 0) {
                        FocuSenseGreen
                    } else {
                        FocuSensePurple
                    }
                ) {
                    NeonSectionHeader(
                        title = "Ultime 7 vs precedenti 7"
                    )
                    Text(
                        "${trend.recentAverage}/100 vs ${trend.previousAverage}/100",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    val prefix = if (trend.delta > 0) "+" else ""
                    Text(
                        "$prefix${trend.delta} punti",
                        color = if (trend.delta >= 0) {
                            FocuSenseGreen
                        } else {
                            FocuSensePurple
                        }
                    )
                }
            }
        }

        item {
            Text(
                "Le associazioni descrivono soltanto il tuo storico. FocuSense non misura direttamente la produttività e non dimostra causalità.",
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseTextMuted
            )
        }
    }
}
