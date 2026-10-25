package com.utilitylab.focussense.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.utilitylab.focussense.FocusSenseApp
import com.utilitylab.focussense.MainActivity
import com.utilitylab.focussense.data.local.SensorSampleEntity
import com.utilitylab.focussense.domain.FocusScoreEngine
import com.utilitylab.focussense.domain.PersonalFocusProfile
import com.utilitylab.focussense.domain.LiveSensorSnapshot
import com.utilitylab.focussense.domain.SensorAvailability
import com.utilitylab.focussense.sensors.AmbientAudioMonitor
import com.utilitylab.focussense.sensors.SensorFusionMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusSessionService : Service() {
    companion object {
        const val ACTION_START = "com.utilitylab.focussense.START_FOCUS_SESSION"
        const val ACTION_STOP = "com.utilitylab.focussense.STOP_FOCUS_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_POSTURE_MINUTES = "posture_minutes"
        const val EXTRA_POSTURE_ENABLED = "posture_enabled"
        const val EXTRA_PLANNED_MINUTES = "planned_minutes"

        private const val CHANNEL_SESSION = "focus_session"
        private const val CHANNEL_POSTURE = "posture_reminders"
        private const val NOTIFICATION_ID = 4001
        private const val POSTURE_NOTIFICATION_ID = 4002
        private const val SAMPLE_MS = 5_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionJob: Job? = null
    private var postureJob: Job? = null
    private var durationJob: Job? = null
    private lateinit var audio: AmbientAudioMonitor
    private lateinit var sensors: SensorFusionMonitor
    private var wakeLock: PowerManager.WakeLock? = null
    private var sessionId: Long = -1L
    private var postureReminderCount = 0
    private var personalProfile = PersonalFocusProfile.generic()
    private var stopping = false
    private var audioActive = false

    private val app: FocusSenseApp get() = application as FocusSenseApp

    override fun onCreate() {
        super.onCreate()
        audio = AmbientAudioMonitor(this)
        sensors = SensorFusionMonitor(this)
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(
                id = intent.getLongExtra(EXTRA_SESSION_ID, -1L),
                postureMinutes = intent.getIntExtra(EXTRA_POSTURE_MINUTES, 30),
                postureEnabled = intent.getBooleanExtra(EXTRA_POSTURE_ENABLED, true),
                plannedMinutes = intent.getIntExtra(EXTRA_PLANNED_MINUTES, 50)
            )
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(
        id: Long,
        postureMinutes: Int,
        postureEnabled: Boolean,
        plannedMinutes: Int
    ) {
        if (id <= 0 || sessionJob != null || stopping) return

        sessionId = id
        postureReminderCount = 0
        stopping = false
        personalProfile = PersonalFocusProfile.generic()
        audioActive = false
        SensorStateBus.clearError()
        SensorStateBus.setActiveSession(id)

        try {
            val notification = buildSessionNotification("Analisi ambientale in corso")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            acquireWakeLock()
            audioActive = audio.start()
            sensors.start()
        } catch (t: Throwable) {
            failStart(id, t)
            return
        }

        scope.launch(Dispatchers.IO) {
            personalProfile = app.repository.buildPersonalProfile()
        }

        sessionJob = scope.launch {
            while (isActive) {
                val snapshot = buildSnapshot()
                SensorStateBus.publish(snapshot)
                app.repository.addSample(snapshot.toEntity(sessionId))
                updateForegroundText(snapshot)
                delay(SAMPLE_MS)
            }
        }

        if (postureEnabled) {
            postureJob = scope.launch {
                val delayMs = postureMinutes.coerceIn(10, 120) * 60_000L
                while (isActive) {
                    delay(delayMs)
                    postureReminderCount++
                    showPostureReminder()
                }
            }
        }

        durationJob = scope.launch {
            delay(plannedMinutes.coerceIn(5, 180) * 60_000L)
            stopSession()
        }
    }

    private fun currentAvailability(): SensorAvailability = sensors.availability.copy(
        microphone = audioActive
    )

    private fun buildSnapshot(): LiveSensorSnapshot {
        val noise = audio.currentDbfs
        val light = sensors.lightLux
        val motion = sensors.motionIndex
        val availability = currentAvailability()

        return LiveSensorSnapshot(
            timestamp = System.currentTimeMillis(),
            noiseDbfs = noise,
            lightLux = light,
            motionIndex = motion,
            gyroMagnitude = sensors.gyroMagnitude,
            faceUp = sensors.faceUp,
            deviceStable = availability.accelerometer && motion < personalProfile.targets.stableMotionIndex,
            focusScore = FocusScoreEngine.liveScore(
                noiseDbfs = noise,
                lightLux = light,
                motionIndex = motion,
                targets = personalProfile.targets,
                availability = availability,
                weights = personalProfile.weights
            ),
            sensorsAvailable = availability
        )
    }

    private fun LiveSensorSnapshot.toEntity(id: Long) = SensorSampleEntity(
        sessionId = id,
        timestamp = timestamp,
        noiseDbfs = noiseDbfs,
        lightLux = lightLux,
        motionIndex = motionIndex,
        gyroMagnitude = gyroMagnitude,
        deviceStable = deviceStable,
        faceUp = faceUp
    )

    private fun stopSession() {
        if (stopping) return
        stopping = true

        val id = sessionId
        val availability = if (::sensors.isInitialized) currentAvailability() else SensorAvailability()

        sessionJob?.cancel()
        postureJob?.cancel()
        durationJob?.cancel()
        sessionJob = null
        postureJob = null
        durationJob = null

        runCatching { audio.stop() }
        runCatching { sensors.stop() }
        releaseWakeLock()

        if (id > 0) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    app.repository.finalizeSession(
                        sessionId = id,
                        postureReminderCount = postureReminderCount,
                        profile = personalProfile,
                        availability = availability
                    )
                }.onFailure {
                    SensorStateBus.publishError("Impossibile salvare correttamente la sessione.")
                }

                SensorStateBus.setActiveSession(null)
                SensorStateBus.reset()
                sessionId = -1L
                audioActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } else {
            SensorStateBus.setActiveSession(null)
            SensorStateBus.reset()
            audioActive = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun failStart(id: Long, error: Throwable) {
        stopping = true
        sessionJob?.cancel()
        postureJob?.cancel()
        durationJob?.cancel()
        sessionJob = null
        postureJob = null
        durationJob = null

        runCatching { audio.stop() }
        runCatching { sensors.stop() }
        releaseWakeLock()

        SensorStateBus.publishError(
            when (error) {
                is SecurityException -> "Permesso microfono non disponibile: la sessione non è stata avviata."
                else -> "Impossibile avviare l'analisi dei sensori. Riprova."
            }
        )
        SensorStateBus.setActiveSession(null)
        SensorStateBus.reset()

        scope.launch(Dispatchers.IO) {
            runCatching { app.repository.deleteSession(id) }
            sessionId = -1L
            audioActive = false
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
        }
    }

    private fun updateForegroundText(snapshot: LiveSensorSnapshot) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            NOTIFICATION_ID,
            buildSessionNotification("Focus ${snapshot.focusScore}/100 · Luce ${snapshot.lightLux.toInt()} lux")
        )
    }

    private fun buildSessionNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_SESSION)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("FocuSense attivo")
        .setContentText(text)
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(mainPendingIntent())
        .addAction(
            android.R.drawable.ic_media_pause,
            "Termina",
            PendingIntent.getService(
                this,
                12,
                Intent(this, FocusSessionService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun showPostureReminder() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            POSTURE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_POSTURE)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Controllo postura")
                .setContentText("Spalle rilassate, schiena neutra, piedi appoggiati. Fai un micro-break se serve.")
                .setAutoCancel(true)
                .setContentIntent(mainPendingIntent())
                .build()
        )
    }

    private fun mainPendingIntent() = PendingIntent.getActivity(
        this,
        10,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusSense::Session").apply {
            setReferenceCounted(false)
            acquire(4 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SESSION, "Sessione Focus", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_POSTURE, "Promemoria postura", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    override fun onDestroy() {
        val wasUnexpected = sessionId > 0 && !stopping
        sessionJob?.cancel()
        postureJob?.cancel()
        durationJob?.cancel()
        runCatching { audio.stop() }
        runCatching { sensors.stop() }
        releaseWakeLock()
        SensorStateBus.setActiveSession(null)
        SensorStateBus.reset()
        if (wasUnexpected) {
            SensorStateBus.publishError("La sessione è stata interrotta dal sistema e non è stata conteggiata.")
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
