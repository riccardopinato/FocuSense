package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.FocusStrictness
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun SessionSetupDialog(
    defaultMinutes: Int,
    defaultStrictness: FocusStrictness = FocusStrictness.STANDARD,
    defaultContext: SessionContext = SessionContext.WORK,
    recentTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onStart: (Int, SessionContext, List<String>, FocusStrictness) -> Unit
) {
    var minutes by remember { mutableIntStateOf(defaultMinutes) }
    var strictness by remember { mutableStateOf(defaultStrictness) }
    var context by remember { mutableStateOf(defaultContext) }
    var input by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptyList<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova sessione", color = FocuSenseText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Durata",
                    style = MaterialTheme.typography.titleSmall,
                    color = FocuSenseText
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(15, 25, 45, 60, 90)) { value ->
                        NeonPill(
                            text = "$value min",
                            selected = minutes == value,
                            onClick = { minutes = value }
                        )
                    }
                }

                Text(
                    "Disciplina",
                    style = MaterialTheme.typography.titleSmall,
                    color = FocuSenseText
                )
                FocusStrictness.entries.forEach { mode ->
                    NeonPill(
                        text = when (mode) {
                            FocusStrictness.RELAXED -> "Relaxed · fino al 15% instabile"
                            FocusStrictness.STANDARD -> "Standard · fino all'8%"
                            FocusStrictness.STRICT -> "Strict · fino al 3%"
                        },
                        selected = strictness == mode,
                        onClick = { strictness = mode },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    "Spegnere lo schermo non penalizza la sessione. Uscire dall'app a schermo acceso e prendere spesso il telefono riducono l'Integrity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuSenseTextMuted
                )

                Text(
                    "Contesto",
                    style = MaterialTheme.typography.titleSmall,
                    color = FocuSenseText
                )
                SessionContext.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item ->
                            FilterChip(
                                selected = context == item,
                                onClick = { context = item },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(24) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Aggiungi tag") },
                    supportingText = { Text("${tags.size}/5") },
                    trailingIcon = {
                        TextButton(
                            enabled = input.isNotBlank() && tags.size < 5,
                            onClick = {
                                tags = SessionTagCodec.normalize(tags + input)
                                input = ""
                            }
                        ) { Text("Aggiungi") }
                    }
                )

                if (recentTags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recentTags.take(8)) { tag ->
                            AssistChip(
                                onClick = {
                                    tags = SessionTagCodec.normalize(tags + tag)
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                if (tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tags) { tag ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    tags = tags.filterNot {
                                        it.equals(tag, ignoreCase = true)
                                    }
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            NeonPrimaryButton(
                text = "Avvia Focus Core",
                subtitle = "${strictness.label} · $minutes min",
                onClick = { onStart(minutes, context, tags, strictness) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
