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
import com.utilitylab.focussense.ui.components.CalibrationCard
import com.utilitylab.focussense.ui.components.FocusTrendChart
import com.utilitylab.focussense.ui.components.NeonCard
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

    LaunchedEffect(Unit) { vm.refreshPersonalProfile() }

    val completed = sessions.filter { it.endedAt != null }
    val avg = completed.mapNotNull { it.focusScore }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toInt()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonSectionHeader(
                title = "Analisi",
                subtitle = "Pattern, trend e condizioni ricorrenti"
            )
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
                    "Scopri quali combinazioni di ambiente, contesto e orario ricorrono più spesso nelle tue sessioni migliori.",
                    color = FocuSenseTextMuted
                )
                Text(
                    "APRI ANALISI →",
                    style = MaterialTheme.typography.labelLarge,
                    color = FocuSenseCyan
                )
            }
        }

        item { CalibrationCard(profile) }

        item {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accent = FocuSenseCyan
            ) {
                Text("Focus medio", color = FocuSenseTextMuted)
                Text(
                    avg?.let { "$it/100" } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = FocuSenseCyan
                )
                Text(
                    "Descrive le condizioni rilevate dal telefono, non misura direttamente la produttività.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuSenseTextMuted
                )
            }
        }

        if (completed.isNotEmpty()) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSensePurple
                ) {
                    NeonSectionHeader(
                        title = "Trend sessioni",
                        subtitle = "Evoluzione del Focus Score"
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
                NeonSectionHeader(title = "Focus per contesto")
                contexts.buckets.forEach { bucket ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(bucket.context.label, style = MaterialTheme.typography.titleSmall)
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
                NeonSectionHeader(title = "Focus per fascia oraria")
                DayPart.entries.forEach { part ->
                    val bucket = temporal.byDayPart[part]
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(part.label, style = MaterialTheme.typography.titleSmall)
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
                    subtitle = "Quando le condizioni sono state migliori"
                )
                TemporalHeatmap(temporal.heatmap, Modifier.fillMaxWidth())
            }
        }

        temporal.recentTrend?.let { trend ->
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (trend.delta >= 0) FocuSenseGreen else FocuSensePurple
                ) {
                    NeonSectionHeader(title = "Ultime 7 vs precedenti 7")
                    Text(
                        "${trend.recentAverage}/100 vs ${trend.previousAverage}/100",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    val prefix = if (trend.delta > 0) "+" else ""
                    Text(
                        "$prefix${trend.delta} punti",
                        color = if (trend.delta >= 0) FocuSenseGreen else FocuSensePurple
                    )
                }
            }
        }

        if (contexts.messages.isNotEmpty()) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSenseGreen
                ) {
                    NeonSectionHeader(title = "Pattern per attività")
                    contexts.messages.forEach {
                        Text("• $it", color = FocuSenseTextMuted)
                    }
                }
            }
        }

        if (temporal.messages.isNotEmpty()) {
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSenseCyan
                ) {
                    NeonSectionHeader(title = "Pattern temporali")
                    temporal.messages.forEach {
                        Text("• $it", color = FocuSenseTextMuted)
                    }
                }
            }
        }

        item {
            Text(
                "I confronti descrivono lo storico personale. FocuSense non interpreta le associazioni come prova di causalità.",
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseTextMuted
            )
        }
    }
}
