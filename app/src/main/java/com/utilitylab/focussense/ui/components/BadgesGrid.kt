package com.utilitylab.focussense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.ProgressBadge
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSenseOutline
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun BadgesGrid(
    unlocked: Set<ProgressBadge>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NeonSectionHeader(
            title = "Traguardi",
            subtitle = "${unlocked.size}/${ProgressBadge.entries.size} sbloccati"
        )

        ProgressBadge.entries.forEach { badge ->
            val isUnlocked = badge in unlocked

            NeonCard(
                accent = if (isUnlocked) FocuSenseGreen else FocuSenseOutline
            ) {
                Text(
                    badge.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUnlocked) FocuSenseGreen else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuSenseTextMuted
                )
                Text(
                    if (isUnlocked) "SBLOCCATO" else "DA SBLOCCARE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnlocked) FocuSenseGreen else FocuSenseTextMuted
                )
            }
        }
    }
}
