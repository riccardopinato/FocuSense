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
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.utilitylab.focussense.FocusSenseApp
import com.utilitylab.focussense.MainActivity
import com.utilitylab.focussense.data.local.SensorSampleEntity
import com.utilitylab.focussense.domain.FocusCoreStage
import com.utilitylab.focussense.domain.FocusIntegrityEngine
import com.utilitylab.focussense.domain.FocusRuntimeSnapshot
import com.utilitylab.focussense.domain.FocusScoreEngine
import com.utilitylab.focussense.domain.FocusStrictness
import com.utilitylab.focussense.domain.LiveSensorSnapshot
import com.utilitylab.focussense.domain.PersonalFocusProfile
import com.utilitylab.focussense.domain.SensorAvailability
import com.utilitylab.focussense.domain.SessionStatus
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max

class FocusSessionService : Service() {
    companion object {
        const val ACTION_START = "com.utilitylab.focussense.START_FOCUS_SESSION"
        const val ACTION_STOP = "com.utilitylab.focussense.STOP_FOCUS_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_POSTURE_MINUTES = "posture_minutes"
        const val EXTRA_POSTURE_ENABLED = "posture_enabled"
        const val EXTRA_PLANNED_MINUTES = "planned_minutes"
        const val EXTRA_STRICTNESS = "strictness"

        private const val CHANNEL_SESSION = "focus_session"
        private const val CHANNEL_POSTURE = "posture_reminders"
        private const val NOTIFICATION_ID = 4001
        private const val POSTURE_NOTIFICATION_ID = 4002
        private const val TICK_MS = 1_000L
        private const val SAMPLE_MS = 5_000L
        private const val PICKUP_COOLDOWN_SECONDS = 8L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionJob: Job? = null
    private var postureJob: Job? = null

    private lateinit var audio: AmbientAudioMonitor
    private lateinit var sensors: SensorFusionMonitor

    private var wakeLock: PowerManager.WakeLock? = null
    private var sessionId: Long = -1L
    private var postureReminderCount = 0
    private var personalProfile = PersonalFocusProfile.generic()
    private var stopping = false
    private var audioActive = false

    private var startedRealtime = 0L
    private var plannedSeconds = 0L
    private var strictness = FocusStrictness.STANDARD
    private var unstableSeconds = 0L
    private var pickupCount = 0
    private var foregroundBreakCount = 0
    private var backgroundSeconds = 0L
    private var screenOffSeconds = 0L
    private var wasUnstable = false
    private var wasDistracted = false
    private var lastPickupSecond = Long.MIN_VALUE / 4
    private var lastPersistSecond = -1L

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
                postureEnabled = intent.getBooleanExtra(EXTRA_POSTURE_ENABLED, false),
                plannedMinutes = intent.getIntExtra(EXTRA_PLANNED_MINUTES, 50),
                strictness = FocusStrictness.fromKey(
                    intent.getStringExtra(EXTRA_STRICTNESS)
                )
            )
            ACTION_STOP -> stopSession(SessionStatus.ABANDONED)
        }
        return START_NOT_STICKY
    }

    private fun startSession(
        id: Long,
        postureMinutes: Int,
        postureEnabled: Boolean,
        plannedMinutes: Int,
        strictness: FocusStrictness
    ) {
        if (id <= 0 || sessionJob != null || stopping) return

        sessionId = id
        postureReminderCount = 0
        stopping = false
        personalProfile = PersonalFocusProfile.generic()
        audioActive = false
        this.strictness = strictness
        plannedSeconds = plannedMinutes.coerceIn(5, 180) * 60L
        startedRealtime = SystemClock.elapsedRealtime()
        unstableSeconds = 0
        pickupCount = 0
        foregroundBreakCount = 0
        backgroundSeconds = 0
        screenOffSeconds = 0
        wasUnstable = false
        wasDistracted = false
        lastPickupSecond = Long.MIN_VALUE / 4
        lastPersistSecond = -1L

        SensorStateBus.clearError()
        SensorStateBus.setActiveSession(id)
        SensorStateBus.publishRuntime(
            FocusRuntimeSnapshot(
                plannedSeconds = plannedSeconds,
                strictness = strictness
            )
        )

        try {
            val notification = buildSessionNotification("Focus Integrity 100%")
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
            while (isActive && !stopping) {
                val elapsed =
                    ((SystemClock.elapsedRealtime() - startedRealtime) / 1000L)
                        .coerceAtLeast(0L)

                updateBehavior(elapsed)

                val snapshot = buildSnapshot()
                val runtime = buildRuntime(elapsed)
                SensorStateBus.publish(snapshot)
                SensorStateBus.publishRuntime(runtime)

                if (
                    lastPersistSecond < 0L ||
                    elapsed - lastPersistSecond >= SAMPLE_MS / 1000L
                ) {
                    app.repository.addSample(snapshot.toEntity(sessionId))
                    lastPersistSecond = elapsed
                }

                updateForegroundText(runtime)

                if (elapsed >= plannedSeconds) {
                    stopSession(SessionStatus.COMPLETED)
                    break
                }

                delay(TICK_MS)
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
    }

    private fun updateBehavior(elapsed: Long) {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val interactive = pm.isInteractive
        val foreground = SensorStateBus.appForeground.value

        if (!interactive) {
            screenOffSeconds++
            wasDistracted = false
        } else {
            val distracted = !foreground
            if (distracted) {
                backgroundSeconds++
                if (!wasDistracted) foregroundBreakCount++
            }
            wasDistracted = distracted
        }

        val motionPeak = sensors.consumePeakMotion()
        val personalizedTrigger = personalProfile.targets.stableMotionIndex + 5f
        val threshold = max(strictness.motionTrigger, personalizedTrigger)
        val unstable = sensors.availability.accelerometer && motionPeak >= threshold

        if (unstable) unstableSeconds++

        if (
            unstable &&
            !wasUnstable &&
            elapsed >= 5L &&
            elapsed - lastPickupSecond >= PICKUP_COOLDOWN_SECONDS
        ) {
            pickupCount++
            lastPickupSecond = elapsed
        }
        wasUnstable = unstable
    }

    private fun currentAvailability(): SensorAvailability =
        sensors.availability.copy(microphone = audioActive)

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
            deviceStable = availability.accelerometer &&
                motion < max(
                    strictness.motionTrigger,
                    personalProfile.targets.stableMotionIndex + 5f
                ),
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

    private fun buildRuntime(elapsed: Long): FocusRuntimeSnapshot {
        val integrity = FocusIntegrityEngine.calculate(
            elapsedSeconds = elapsed,
            unstableSeconds = unstableSeconds,
            pickupCount = pickupCount,
            foregroundBreakCount = foregroundBreakCount,
            backgroundSeconds = backgroundSeconds,
            strictness = strictness
        )
        val unstablePercent =
            if (elapsed > 0L) unstableSeconds * 100f / elapsed else 0f

        val provisionalGrowth = FocusIntegrityEngine.growthPoints(
            elapsedSeconds = elapsed,
            plannedSeconds = plannedSeconds,
            integrityScore = integrity,
            screenOffSeconds = screenOffSeconds,
            status = SessionStatus.COMPLETED
        )
        val stage = FocusIntegrityEngine.coreStage(
            growthPoints = provisionalGrowth,
            integrityScore = integrity,
            status = SessionStatus.COMPLETED
        ).let {
            if (it == FocusCoreStage.PRISM && elapsed < plannedSeconds) {
                FocusCoreStage.NOVA
            } else {
                it
            }
        }

        return FocusRuntimeSnapshot(
            elapsedSeconds = elapsed,
            plannedSeconds = plannedSeconds,
            integrityScore = integrity,
            unstableSeconds = unstableSeconds,
            unstablePercent = unstablePercent,
            pickupCount = pickupCount,
            foregroundBreakCount = foregroundBreakCount,
            backgroundSeconds = backgroundSeconds,
            screenOffSeconds = screenOffSeconds,
            strictness = strictness,
            coreStage = stage
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

    private fun stopSession(requestedStatus: SessionStatus) {
        if (stopping) return
        stopping = true

        val id = sessionId
        val availability =
            if (::sensors.isInitialized) currentAvailability()
            else SensorAvailability()
        val runtime = SensorStateBus.runtime.value

        sessionJob?.cancel()
        postureJob?.cancel()
        sessionJob = null
        postureJob = null

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
                        availability = availability,
                        runtime = runtime,
                        requestedStatus = requestedStatus
                    )
                }.onFailure {
                    SensorStateBus.publishError(
                        "Impossibile salvare correttamente la sessione."
                    )
                }

                SensorStateBus.setActiveSession(null)
                sessionId = -1L
                audioActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } else {
            SensorStateBus.setActiveSession(null)
            audioActive = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun failStart(id: Long, error: Throwable) {
        stopping = true
        sessionJob?.cancel()
        postureJob?.cancel()
        sessionJob = null
        postureJob = null

        runCatching { audio.stop() }
        runCatching { sensors.stop() }
        releaseWakeLock()

        SensorStateBus.publishError(
            when (error) {
                is SecurityException ->
                    "Permesso microfono non disponibile: la sessione non è stata avviata."
                else ->
                    "Impossibile avviare l'analisi dei sensori. Riprova."
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

    private fun updateForegroundText(runtime: FocusRuntimeSnapshot) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            NOTIFICATION_ID,
            buildSessionNotification(
                "Integrity ${runtime.integrityScore}% · ${formatTime(runtime.remainingSeconds)}"
            )
        )
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    private fun buildSessionNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_SESSION)
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
                    Intent(this, FocusSessionService::class.java)
                        .setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun showPostureReminder() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            POSTURE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_POSTURE)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Micro-break")
                .setContentText(
                    "Se serve, rilassa spalle e sguardo per qualche secondo."
                )
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
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FocuSense::Session"
        ).apply {
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
            NotificationChannel(
                CHANNEL_SESSION,
                "Sessione Focus",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_POSTURE,
                "Micro-break",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    override fun onDestroy() {
        val unexpected = sessionId > 0 && !stopping
        val id = sessionId
        val availability =
            if (::sensors.isInitialized) currentAvailability()
            else SensorAvailability()
        val runtime = SensorStateBus.runtime.value

        sessionJob?.cancel()
        postureJob?.cancel()
        runCatching { audio.stop() }
        runCatching { sensors.stop() }
        releaseWakeLock()

        if (unexpected && id > 0) {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1_500L) {
                    runCatching {
                        app.repository.finalizeSession(
                            sessionId = id,
                            postureReminderCount = postureReminderCount,
                            profile = personalProfile,
                            availability = availability,
                            runtime = runtime,
                            requestedStatus = SessionStatus.INTERRUPTED
                        )
                    }
                }
            }
            SensorStateBus.publishError(
                "La sessione è stata interrotta dal sistema e salvata come incompleta."
            )
        }

        SensorStateBus.setActiveSession(null)
        SensorStateBus.reset()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
