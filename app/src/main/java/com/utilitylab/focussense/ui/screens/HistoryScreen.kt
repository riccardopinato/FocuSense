package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.data.local.FocusSessionEntity
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonPill
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.components.SessionDetailDialog
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseDanger
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val detail by vm.selectedSession.collectAsStateWithLifecycle()
    var contextFilter by remember { mutableStateOf<SessionContext?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }

    val allTags = remember(sessions) {
        sessions.flatMap { SessionTagCodec.decode(it.tagsCsv) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    val filtered = remember(sessions, contextFilter, tagFilter) {
        sessions.filter { session ->
            val contextOk = contextFilter == null ||
                SessionContext.fromKey(session.contextKey) == contextFilter
            val tagOk = tagFilter == null ||
                SessionTagCodec.decode(session.tagsCsv)
                    .any { it.equals(tagFilter, ignoreCase = true) }
            contextOk && tagOk
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NeonSectionHeader(
            title = "Storico",
            subtitle = "${sessions.size} sessioni salvate"
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                NeonPill(
                    text = "Tutti",
                    selected = contextFilter == null,
                    onClick = { contextFilter = null }
                )
            }
            items(SessionContext.entries) { item ->
                NeonPill(
                    text = item.label,
                    selected = contextFilter == item,
                    onClick = { contextFilter = item }
                )
            }
        }

        if (allTags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    NeonPill(
                        text = "Qualsiasi tag",
                        selected = tagFilter == null,
                        onClick = { tagFilter = null },
                        accent = FocuSenseGreen
                    )
                }
                items(allTags) { tag ->
                    NeonPill(
                        text = "#$tag",
                        selected = tagFilter.equals(tag, ignoreCase = true),
                        onClick = { tagFilter = tag },
                        accent = FocuSenseGreen
                    )
                }
            }
        }

        if (contextFilter != null || tagFilter != null) {
            Text(
                "Azzera filtri",
                modifier = Modifier.clickable {
                    contextFilter = null
                    tagFilter = null
                },
                color = FocuSenseCyan,
                style = MaterialTheme.typography.labelLarge
            )
        }

        when {
            sessions.isEmpty() -> Text("Nessuna sessione salvata.", color = FocuSenseTextMuted)
            filtered.isEmpty() -> Text("Nessuna sessione corrisponde ai filtri.", color = FocuSenseTextMuted)
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { session ->
                    HistoryCard(
                        session = session,
                        onOpen = { vm.openSessionDetail(session.id) },
                        onDelete = { vm.deleteSession(session.id) }
                    )
                }
            }
        }
    }

    detail?.let {
        SessionDetailDialog(
            detail = it,
            onDismiss = vm::closeSessionDetail
        )
    }
}

@Composable
private fun HistoryCard(
    session: FocusSessionEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val date = Instant.ofEpochMilli(session.startedAt)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
    val sessionContext = SessionContext.fromKey(session.contextKey)
    val tags = SessionTagCodec.decode(session.tagsCsv)

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        accent = if ((session.focusScore ?: 0) >= 75) FocuSenseGreen else FocuSenseCyan
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(sessionContext.label, style = MaterialTheme.typography.titleMedium, color = FocuSenseText)
                Text(date, style = MaterialTheme.typography.bodySmall, color = FocuSenseTextMuted)
            }
            Text(
                "${session.focusScore ?: "—"}/100",
                style = MaterialTheme.typography.titleLarge,
                color = FocuSenseCyan
            )
        }

        if (tags.isNotEmpty()) {
            Text(
                tags.joinToString(" · ") { "#$it" },
                style = MaterialTheme.typography.bodySmall,
                color = FocuSenseGreen
            )
        }

        Text(
            "Durata ${session.actualDurationSeconds / 60} min" +
                (session.avgLightLux?.let { " · ${it.toInt()} lux" } ?: ""),
            color = FocuSenseTextMuted
        )

        val metrics = buildList {
            session.avgNoiseDbfs?.let { add("${it.toInt()} dBFS") }
            session.stablePercent?.let { add("Stabilità ${it.toInt()}%") }
        }
        if (metrics.isNotEmpty()) {
            Text(metrics.joinToString(" · "), color = FocuSenseTextMuted)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Dettagli",
                modifier = Modifier.clickable(onClick = onOpen),
                color = FocuSenseCyan,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Elimina",
                modifier = Modifier.clickable(onClick = onDelete),
                color = FocuSenseDanger,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
