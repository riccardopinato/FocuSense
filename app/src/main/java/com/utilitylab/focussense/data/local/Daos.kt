package com.utilitylab.focussense.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentCompleted(limit: Int = 30): List<FocusSessionEntity>

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM focus_sessions WHERE endedAt IS NULL")
    suspend fun deleteUnfinished(): Int
}

@Dao
interface SensorSampleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sample: SensorSampleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(samples: List<SensorSampleEntity>)

    @Query("SELECT * FROM sensor_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: Long): List<SensorSampleEntity>

    @Query("SELECT * FROM sensor_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeForSession(sessionId: Long): Flow<List<SensorSampleEntity>>

    @Query("DELETE FROM sensor_samples WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
