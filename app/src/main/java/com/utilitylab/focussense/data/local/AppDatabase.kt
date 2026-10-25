package com.utilitylab.focussense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FocusSessionEntity::class, SensorSampleEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun sensorSampleDao(): SensorSampleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN contextKey TEXT NOT NULL DEFAULT 'WORK'")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN environmentScore INTEGER")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN integrityScore INTEGER")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN strictnessKey TEXT NOT NULL DEFAULT 'STANDARD'")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN sessionStatusKey TEXT NOT NULL DEFAULT 'LEGACY'")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN foregroundBreakCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN backgroundSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN screenOffSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN unstableSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN unstablePercent REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN pickupCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN growthPoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN coreStageKey TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_sense.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }

        fun create(context: Context): AppDatabase = getInstance(context)
    }
}
