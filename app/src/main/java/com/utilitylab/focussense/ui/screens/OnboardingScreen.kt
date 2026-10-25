package com.utilitylab.focussense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitylab.focussense.domain.FocusCoreStage
import com.utilitylab.focussense.ui.components.FocusCoreVisual
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonOutlineButton
import com.utilitylab.focussense.ui.components.NeonPrimaryButton
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pageState = remember { mutableIntStateOf(0) }
    val page = pageState.intValue

    val title = when (page) {
        0 -> "Proteggi il tuo focus"
        1 -> "Impara le condizioni migliori"
        else -> "Costruisci il tuo Sanctuary"
    }

    val body = when (page) {
        0 -> "Durante una sessione FocuSense misura quanto lasci in pace il telefono. Pickup, movimento e uscite dall'app riducono la Focus Integrity. Spegnere lo schermo non è una penalità."
        1 -> "Rumore e luce vengono analizzati localmente. Dopo ogni sessione dai un voto al tuo focus: FocuSense usa quelle valutazioni per capire quali condizioni ricorrono quando lavori meglio."
        else -> "Ogni sessione completata crea un Focus Core. Più proteggi la concentrazione, più il Core evolve. I risultati restano nel Sanctuary e alimentano livelli, streak e obiettivi."
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FocusCoreVisual(
            stage = when (page) {
                0 -> FocusCoreStage.SPARK
                1 -> FocusCoreStage.CRYSTAL
                else -> FocusCoreStage.PRISM
            },
            integrity = 100,
            size = 150.dp
        )

        Spacer(Modifier.height(24.dp))

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = when (page) {
                0 -> FocuSenseCyan
                1 -> FocuSensePurple
                else -> FocuSenseGreen
            }
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = FocuSenseText
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = FocuSenseTextMuted
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "${page + 1}/3",
            color = FocuSenseTextMuted,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(12.dp))

        if (page < 2) {
            NeonPrimaryButton(
                text = "Continua",
                onClick = { pageState.intValue++ },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            NeonOutlineButton(
                text = "Salta",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            NeonPrimaryButton(
                text = "Inizia con FocuSense",
                subtitle = "Tutto il monitoraggio resta sul dispositivo",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
