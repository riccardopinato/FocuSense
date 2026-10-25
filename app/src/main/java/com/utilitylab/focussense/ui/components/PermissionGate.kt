package com.utilitylab.focussense.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.utilitylab.focussense.ui.theme.FocuSenseText

fun hasCorePermissions(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

@Composable
fun PermissionAwareStartButton(
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onReady: () -> Unit
) {
    val context = LocalContext.current
    val explain = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.RECORD_AUDIO] == true) onReady()
    }

    NeonPrimaryButton(
        text = label,
        subtitle = "Analisi locale dei sensori",
        enabled = enabled,
        modifier = modifier,
        onClick = {
            if (hasCorePermissions(context)) onReady() else explain.value = true
        }
    )

    if (explain.value) {
        AlertDialog(
            onDismissRequest = { explain.value = false },
            title = { Text("Permesso microfono", color = FocuSenseText) },
            text = {
                Text(
                    "FocuSense usa il microfono soltanto per stimare il livello sonoro relativo. " +
                        "Non salva né trasmette registrazioni audio."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        explain.value = false
                        val permissions = buildList {
                            add(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= 33) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                        launcher.launch(permissions)
                    }
                ) { Text("Continua") }
            },
            dismissButton = {
                TextButton(onClick = { explain.value = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
