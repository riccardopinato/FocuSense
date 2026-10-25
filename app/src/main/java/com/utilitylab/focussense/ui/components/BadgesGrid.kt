package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.ProgressBadge

@Composable
fun BadgesGrid(
    unlocked: Set<ProgressBadge>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Traguardi",
            style = MaterialTheme.typography.titleLarge
        )

        ProgressBadge.entries.forEach { badge ->
            val isUnlocked = badge in unlocked

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        badge.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        badge.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        if (isUnlocked) "Sbloccato" else "Da sbloccare",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
