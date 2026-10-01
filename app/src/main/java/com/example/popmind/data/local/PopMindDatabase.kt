package com.example.popmind.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SessionEntity::class, AssessmentEntity::class, RoadmapGoalEntity::class, ActiveFocusEntity::class], version = 5, exportSchema = false)
abstract class PopMindDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun roadmapGoalDao(): RoadmapGoalDao
    abstract fun activeFocusDao(): ActiveFocusDao

    companion object {
        @Volatile private var instance: PopMindDatabase? = null

        fun getInstance(context: Context): PopMindDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PopMindDatabase::class.java,
                "popmind_sessions.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `assessments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`completedAt` INTEGER NOT NULL, " +
                        "`rapidContentScore` REAL NOT NULL, " +
                        "`attentionScore` REAL NOT NULL, " +
                        "`digitalControlScore` REAL NOT NULL, " +
                        "`studySleepScore` REAL NOT NULL, " +
                        "`answers` TEXT NOT NULL)"
                )
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `roadmap_goals` (`id` TEXT NOT NULL, `weekStart` INTEGER NOT NULL, `title` TEXT NOT NULL, `reason` TEXT NOT NULL, `activity` TEXT NOT NULL, `completed` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `active_focus` (`id` INTEGER NOT NULL, `task` TEXT NOT NULL, `pomodoro` INTEGER NOT NULL, `focusMinutes` INTEGER NOT NULL, `breakMinutes` INTEGER NOT NULL, `phase` TEXT NOT NULL, `startedAtWall` INTEGER NOT NULL, `startedAtElapsed` INTEGER NOT NULL, `phaseEndElapsed` INTEGER NOT NULL, `interruptions` INTEGER NOT NULL, `completedCycles` INTEGER NOT NULL, `sound` TEXT NOT NULL, `soundEnabled` INTEGER NOT NULL, `volume` REAL NOT NULL, `tryDnd` INTEGER NOT NULL, `oldInterruptionFilter` INTEGER NOT NULL, `changedDnd` INTEGER NOT NULL, `sessionId` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `active_focus` ADD COLUMN `pinRequested` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
