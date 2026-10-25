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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.utilitylab.focussense.ui.components.SessionDetailDialog
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

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            "Storico",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 18.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = contextFilter == null,
                    onClick = { contextFilter = null },
                    label = { Text("Tutti") }
                )
            }
            items(SessionContext.entries) { item ->
                FilterChip(
                    selected = contextFilter == item,
                    onClick = { contextFilter = item },
                    label = { Text(item.label) }
                )
            }
        }

        if (allTags.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = tagFilter == null,
                        onClick = { tagFilter = null },
                        label = { Text("Qualsiasi tag") }
                    )
                }
                items(allTags) { tag ->
                    FilterChip(
                        selected = tagFilter.equals(tag, ignoreCase = true),
                        onClick = { tagFilter = tag },
                        label = { Text("#$tag") }
                    )
                }
            }
        }

        if (contextFilter != null || tagFilter != null) {
            TextButton(
                onClick = {
                    contextFilter = null
                    tagFilter = null
                }
            ) { Text("Azzera filtri") }
        }

        when {
            sessions.isEmpty() -> Text("Nessuna sessione salvata.")
            filtered.isEmpty() -> Text("Nessuna sessione corrisponde ai filtri.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(sessionContext.label, style = MaterialTheme.typography.labelLarge)
            if (tags.isNotEmpty()) {
                Text(
                    tags.joinToString(" · ") { "#$it" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(date, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${session.focusScore ?: "—"}/100",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Text(
                "Durata ${session.actualDurationSeconds / 60} min" +
                    (session.avgLightLux?.let { " · Luce ${it.toInt()} lux" } ?: "")
            )

            val metrics = buildList {
                session.avgNoiseDbfs?.let { add("Rumore ${it.toInt()} dBFS") }
                session.stablePercent?.let { add("Stabilità ${it.toInt()}%") }
            }
            if (metrics.isNotEmpty()) Text(metrics.joinToString(" · "))

            if (session.selfFocusRating != null || session.postureRating != null) {
                Text(
                    "Focus percepito ${session.selfFocusRating ?: "—"}/5 · " +
                        "Postura ${session.postureRating ?: "—"}/5"
                )
            }

            if (session.note.isNotBlank()) {
                Text(session.note, style = MaterialTheme.typography.bodySmall)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpen) { Text("Dettagli") }
                TextButton(onClick = onDelete) { Text("Elimina") }
            }
        }
    }
}
