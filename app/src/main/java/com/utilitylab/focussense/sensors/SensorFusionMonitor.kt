package com.utilitylab.focussense.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.utilitylab.focussense.domain.SensorAvailability
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class SensorFusionMonitor(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val light = manager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    @Volatile var lightLux: Float = 0f
        private set
    @Volatile var motionIndex: Float = 0f
        private set
    @Volatile var gyroMagnitude: Float = 0f
        private set
    @Volatile var faceUp: Boolean = true
        private set

    @Volatile private var peakMotionIndex: Float = 0f

    val availability = SensorAvailability(
        microphone = true,
        light = light != null,
        accelerometer = accelerometer != null,
        gyroscope = gyroscope != null
    )

    private var smoothedMotion = 0f
    private var smoothedGyro = 0f

    fun start() {
        light?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() = manager.unregisterListener(this)

    @Synchronized
    fun consumePeakMotion(): Float {
        val peak = max(peakMotionIndex, motionIndex)
        peakMotionIndex = motionIndex
        return peak
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> lightLux =
                event.values.firstOrNull()?.coerceAtLeast(0f) ?: 0f
            Sensor.TYPE_ACCELEROMETER -> handleAcceleration(event.values)
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event.values)
        }
    }

    private fun handleAcceleration(v: FloatArray) {
        if (v.size < 3) return
        val magnitude = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        val linearDeviation = abs(magnitude - SensorManager.GRAVITY_EARTH)
        val instantIndex = (linearDeviation * 18f + smoothedGyro * 5f)
            .coerceIn(0f, 100f)
        smoothedMotion = smoothedMotion * 0.82f + instantIndex * 0.18f
        motionIndex = smoothedMotion.coerceIn(0f, 100f)
        peakMotionIndex = max(peakMotionIndex, instantIndex)
        faceUp = v[2] > 5.8f
    }

    private fun handleGyroscope(v: FloatArray) {
        if (v.size < 3) return
        val magnitude = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        smoothedGyro = smoothedGyro * 0.80f + magnitude * 0.20f
        gyroMagnitude = smoothedGyro
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
