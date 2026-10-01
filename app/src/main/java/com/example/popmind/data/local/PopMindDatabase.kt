package com.example.popmind.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SessionEntity::class, AssessmentEntity::class, RoadmapGoalEntity::class], version = 3, exportSchema = false)
abstract class PopMindDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun roadmapGoalDao(): RoadmapGoalDao

    companion object {
        @Volatile private var instance: PopMindDatabase? = null

        fun getInstance(context: Context): PopMindDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PopMindDatabase::class.java,
                "popmind_sessions.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
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
    }
}
