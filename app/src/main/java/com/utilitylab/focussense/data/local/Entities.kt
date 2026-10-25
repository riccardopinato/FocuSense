package com.utilitylab.focussense.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Long = 0,
    val focusScore: Int? = null,
    val avgNoiseDbfs: Float? = null,
    val noiseStdDev: Float? = null,
    val avgLightLux: Float? = null,
    val lightStdDev: Float? = null,
    val avgMotionIndex: Float? = null,
    val avgGyro: Float? = null,
    val stablePercent: Float? = null,
    val faceUpPercent: Float? = null,
    val interruptionCount: Int = 0,
    val postureReminderCount: Int = 0,
    val selfFocusRating: Int? = null,
    val postureRating: Int? = null,
    val note: String = "",
    val contextKey: String = "WORK",
    val tagsCsv: String = ""
)

@Entity(
    tableName = "sensor_samples",
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("timestamp")]
)
data class SensorSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val noiseDbfs: Float,
    val lightLux: Float,
    val motionIndex: Float,
    val gyroMagnitude: Float,
    val deviceStable: Boolean,
    val faceUp: Boolean
)
