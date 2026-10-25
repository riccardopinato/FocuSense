package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.ui.theme.FocuSenseText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupDialog(
    defaultMinutes: Int,
    defaultContext: SessionContext = SessionContext.WORK,
    recentTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onStart: (Int, SessionContext, List<String>) -> Unit
) {
    var minutes by remember { mutableIntStateOf(defaultMinutes) }
    var context by remember { mutableStateOf(defaultContext) }
    var input by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptyList<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova sessione", color = FocuSenseText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Durata", style = MaterialTheme.typography.titleSmall, color = FocuSenseText)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(15, 25, 45, 60, 90)) { value ->
                        FilterChip(
                            selected = minutes == value,
                            onClick = { minutes = value },
                            label = { Text("$value min") }
                        )
                    }
                }

                Text("Contesto", style = MaterialTheme.typography.titleSmall, color = FocuSenseText)
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
            Button(onClick = { onStart(minutes, context, tags) }) {
                Text("Avvia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
