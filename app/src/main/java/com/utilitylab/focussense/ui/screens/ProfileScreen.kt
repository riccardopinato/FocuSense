package com.utilitylab.focussense.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.R
import com.utilitylab.focussense.auth.GoogleAccountManager
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.CalibrationCard
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonOutlineButton
import com.utilitylab.focussense.ui.components.NeonPill
import com.utilitylab.focussense.ui.components.NeonPrimaryButton
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val profile by vm.personalProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val message = remember { mutableStateOf<String?>(null) }
    val serverClientId = stringResource(R.string.google_server_client_id)

    LaunchedEffect(Unit) { vm.refreshPersonalProfile() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeonSectionHeader(
            title = "Profilo",
            subtitle = "Account, calibrazione e preferenze"
        )

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = FocuSenseCyan
        ) {
            if (prefs.signedIn) {
                Text(
                    prefs.googleDisplayName.ifBlank { "Account Google" },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(prefs.googleEmail, color = FocuSenseTextMuted)
                NeonOutlineButton(
                    text = "Esci dall'account",
                    onClick = {
                        if (activity != null) {
                            scope.launch {
                                runCatching { GoogleAccountManager(activity).signOut() }
                                vm.clearGoogleProfile()
                            }
                        }
                    }
                )
            } else {
                Text("Account Google", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Opzionale: associa il profilo all'app. Il backup Android può trasferire dati e preferenze, senza sync realtime.",
                    color = FocuSenseTextMuted
                )
                NeonPrimaryButton(
                    text = "Accedi con Google",
                    onClick = {
                        if (activity != null) {
                            scope.launch {
                                val manager = GoogleAccountManager(activity)
                                runCatching { manager.signIn(serverClientId) }
                                    .onSuccess { p ->
                                        vm.saveGoogleProfile(
                                            p.uniqueId,
                                            p.email,
                                            p.displayName,
                                            p.photoUri
                                        )
                                        message.value = "Account collegato"
                                    }
                                    .onFailure {
                                        message.value = it.message ?: "Accesso non riuscito"
                                    }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            message.value?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = FocuSenseGreen)
            }
        }

        CalibrationCard(profile)

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = FocuSenseGreen
        ) {
            NeonSectionHeader(
                title = "Promemoria postura",
                subtitle = "Promemoria, non misurazione corporea"
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Attivo ogni ${prefs.postureReminderMinutes} min")
                Switch(
                    checked = prefs.postureReminderEnabled,
                    onCheckedChange = {
                        vm.setPostureReminder(it, prefs.postureReminderMinutes)
                    }
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(20, 30, 45).forEach { minutes ->
                    NeonPill(
                        text = "$minutes min",
                        selected = prefs.postureReminderMinutes == minutes,
                        onClick = {
                            vm.setPostureReminder(
                                prefs.postureReminderEnabled,
                                minutes
                            )
                        },
                        modifier = Modifier.weight(1f),
                        accent = FocuSenseGreen
                    )
                }
            }
        }

        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accent = FocuSensePurple
        ) {
            NeonSectionHeader(title = "Privacy e limiti")
            Text(
                "L'audio non viene registrato né salvato: restano solo valori numerici dBFS e dati dei sensori sul dispositivo.",
                color = FocuSenseTextMuted
            )
            Text(
                "Il telefono non misura la postura del corpo. Il profilo personale deriva dalle tue valutazioni.",
                color = FocuSenseTextMuted
            )
            Text(
                "FocuSense non è un dispositivo medico né un fonometro certificato.",
                color = FocuSenseTextMuted
            )
        }
    }
}
