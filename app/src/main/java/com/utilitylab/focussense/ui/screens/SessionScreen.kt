package com.utilitylab.focussense.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.domain.FocusRuntimeSnapshot
import com.utilitylab.focussense.domain.LiveSensorSnapshot
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.FocusCoreVisual
import com.utilitylab.focussense.ui.components.NeonCard
import com.utilitylab.focussense.ui.components.NeonMetricCard
import com.utilitylab.focussense.ui.components.NeonOutlineButton
import com.utilitylab.focussense.ui.components.NeonPrimaryButton
import com.utilitylab.focussense.ui.components.NeonSectionHeader
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseDanger
import com.utilitylab.focussense.ui.theme.FocuSenseGreen
import com.utilitylab.focussense.ui.theme.FocuSensePurple
import com.utilitylab.focussense.ui.theme.FocuSenseText
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

private val PastelMint = Color(0xFFA8E6CF)
private val PastelSky = Color(0xFFAEC6FF)
private val PastelLavender = Color(0xFFCDB4DB)
private val PastelRose = Color(0xFFF3B7C8)

@Composable
fun SessionScreen(
    vm: MainViewModel,
    activeSessionId: Long?,
    close: () -> Unit
) {
    val live by vm.live.collectAsStateWithLifecycle()
    val runtime by vm.runtime.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val error by vm.sessionError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val sessionForRating = remember { mutableStateOf<Long?>(activeSessionId) }
    val showRating = remember { mutableStateOf(false) }
    val controlsVisible = remember { mutableStateOf(false) }

    LaunchedEffect(activeSessionId, error) {
        when {
            error != null -> showRating.value = false
            activeSessionId != null -> sessionForRating.value = activeSessionId
            sessionForRating.value != null -> showRating.value = true
        }
    }

    BackHandler(enabled = activeSessionId != null) {
        controlsVisible.value = true
    }

    FocusDisplayWindowEffect(
        activity = activity,
        enabled = activeSessionId != null && prefs.focusDisplayEnabled
    )

    when {
        error != null && activeSessionId == null -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.Center
            ) {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = FocuSenseDanger
                ) {
                    Text(
                        "Sessione interrotta",
                        style = MaterialTheme.typography.titleLarge,
                        color = FocuSenseText
                    )
                    Text(error ?: "Errore sconosciuto", color = FocuSenseTextMuted)
                    NeonPrimaryButton(
                        text = "Torna alla Home",
                        onClick = {
                            vm.clearSessionError()
                            close()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        activeSessionId != null && prefs.focusDisplayEnabled -> {
            AmoledFocusDisplay(
                runtime = runtime,
                controlsVisible = controlsVisible.value,
                onToggleControls = {
                    controlsVisible.value = !controlsVisible.value
                },
                onStop = { vm.stopSession(context) }
            )
        }

        activeSessionId != null -> {
            DetailedFocusDisplay(
                live = live,
                runtime = runtime,
                onStop = { vm.stopSession(context) }
            )
        }

        !showRating.value -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NeonOutlineButton(
                    text = "Torna alla Home",
                    onClick = close
                )
            }
        }
    }

    if (showRating.value) {
        RatingDialog(
            onSave = { focus, note ->
                sessionForRating.value?.let {
                    vm.saveRatings(it, focus, null, note)
                }
                sessionForRating.value = null
                showRating.value = false
                close()
            },
            onSkip = {
                sessionForRating.value = null
                showRating.value = false
                close()
            }
        )
    }
}

@Composable
private fun FocusDisplayWindowEffect(
    activity: Activity?,
    enabled: Boolean
) {
    DisposableEffect(activity, enabled) {
        if (activity == null || !enabled) {
            return@DisposableEffect onDispose { }
        }

        val window = activity.window
        val previousBrightness = window.attributes.screenBrightness

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply {
            screenBrightness = 0.08f
        }

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply {
                screenBrightness = previousBrightness
            }
        }
    }
}

@Composable
private fun AmoledFocusDisplay(
    runtime: FocusRuntimeSnapshot,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onStop: () -> Unit
) {
    val accent = when {
        runtime.integrityScore >= 92 -> PastelMint
        runtime.integrityScore >= 75 -> PastelSky
        runtime.integrityScore >= 55 -> PastelLavender
        else -> PastelRose
    }

    val drift = when ((runtime.elapsedSeconds / 60L) % 4L) {
        0L -> (-2).dp to (-1).dp
        1L -> 2.dp to (-1).dp
        2L -> 1.dp to 2.dp
        else -> (-1).dp to 2.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onToggleControls),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = drift.first, y = drift.second)
                .padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                formatTimer(runtime.remainingSeconds),
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFFDCE6F2)
            )

            Spacer(Modifier.height(26.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF0B0E12))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(runtime.remainingFraction.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = .48f),
                                    accent.copy(alpha = .92f)
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Integrity ${runtime.integrityScore}%",
                style = MaterialTheme.typography.labelMedium,
                color = accent.copy(alpha = .72f)
            )

            if (controlsVisible) {
                Spacer(Modifier.height(34.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${runtime.pickupCount} pickup",
                        color = Color(0xFF7D8796),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${runtime.foregroundBreakCount} uscite",
                        color = Color(0xFF7D8796),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Termina sessione",
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable(onClick = onStop)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    color = Color(0xFF788294),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DetailedFocusDisplay(
    live: LiveSensorSnapshot,
    runtime: FocusRuntimeSnapshot,
    onStop: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeonSectionHeader(
            title = "Sessione live",
            subtitle = "${runtime.strictness.label} · ${formatTimer(runtime.remainingSeconds)} rimanenti"
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocusCoreVisual(
                stage = runtime.coreStage,
                integrity = runtime.integrityScore
            )
            Column {
                Text(
                    "${runtime.integrityScore}%",
                    style = MaterialTheme.typography.displaySmall,
                    color = FocuSenseGreen
                )
                Text("Focus Integrity", color = FocuSenseTextMuted)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonMetricCard(
                "Rumore",
                if (live.sensorsAvailable.microphone) "${live.noiseDbfs.toInt()} dBFS" else "N/D",
                Modifier.weight(1f),
                accent = FocuSenseCyan
            )
            NeonMetricCard(
                "Luce",
                if (live.sensorsAvailable.light) "${live.lightLux.toInt()} lux" else "N/D",
                Modifier.weight(1f),
                accent = FocuSenseGreen
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonMetricCard(
                "Pickup",
                runtime.pickupCount.toString(),
                Modifier.weight(1f),
                accent = FocuSensePurple
            )
            NeonMetricCard(
                "Fuori app",
                runtime.foregroundBreakCount.toString(),
                Modifier.weight(1f),
                accent = FocuSensePurple
            )
        }

        NeonPrimaryButton(
            text = "Termina sessione",
            subtitle = "Il Core verrà salvato in base ai progressi raggiunti",
            onClick = onStop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RatingDialog(
    onSave: (Int, String) -> Unit,
    onSkip: () -> Unit
) {
    val focus = remember { mutableIntStateOf(4) }
    val note = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Text(
                "Quanto eri concentrato?",
                color = FocuSenseText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Focus percepito: ${focus.intValue}/5",
                    color = FocuSenseTextMuted
                )
                Slider(
                    value = focus.intValue.toFloat(),
                    onValueChange = { focus.intValue = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                OutlinedTextField(
                    value = note.value,
                    onValueChange = { note.value = it },
                    label = { Text("Nota facoltativa") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(focus.intValue, note.value)
                }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Salta") }
        }
    )
}

private fun formatTimer(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val secs = safe % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}
