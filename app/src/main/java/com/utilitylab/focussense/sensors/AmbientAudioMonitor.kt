package com.utilitylab.focussense.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread
import kotlin.math.log10
import kotlin.math.sqrt

class AmbientAudioMonitor(private val context: Context) {
    private val sampleRate = 16_000
    private val channel = AudioFormat.CHANNEL_IN_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        .coerceAtLeast(sampleRate / 2)

    @Volatile private var running = false
    @Volatile var currentDbfs: Float = -90f
        private set

    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    val available: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    val active: Boolean
        get() = running

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        if (!available) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("RECORD_AUDIO non concesso")
        }

        val recorder = createRecorder(MediaRecorder.AudioSource.UNPROCESSED)
            ?: createRecorder(MediaRecorder.AudioSource.MIC)
            ?: return false

        return try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                recorder.release()
                false
            } else {
                audioRecord = recorder
                running = true
                worker = thread(name = "FocusSense-Audio", start = true) {
                    val buffer = ShortArray(minBuffer / 2)
                    while (running) {
                        val read = runCatching { recorder.read(buffer, 0, buffer.size) }.getOrDefault(0)
                        if (read > 0) currentDbfs = rmsToDbfs(buffer, read)
                    }
                }
                true
            }
        } catch (_: Throwable) {
            runCatching { recorder.release() }
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(source: Int): AudioRecord? = runCatching {
        AudioRecord(
            source,
            sampleRate,
            channel,
            encoding,
            minBuffer
        ).let { recorder ->
            if (recorder.state == AudioRecord.STATE_INITIALIZED) recorder
            else {
                recorder.release()
                null
            }
        }
    }.getOrNull()

    fun stop() {
        running = false
        val recorder = audioRecord
        if (recorder != null) {
            runCatching {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            }
        }
        runCatching { worker?.join(750) }
        runCatching { recorder?.release() }
        worker = null
        audioRecord = null
        currentDbfs = -90f
    }

    private fun rmsToDbfs(buffer: ShortArray, size: Int): Float {
        if (size <= 0) return -90f
        var sum = 0.0
        for (i in 0 until size) {
            val normalized = buffer[i] / 32768.0
            sum += normalized * normalized
        }
        val rms = sqrt(sum / size).coerceAtLeast(1e-9)
        return (20.0 * log10(rms)).toFloat().coerceIn(-90f, 0f)
    }
}
