package com.example.popmind.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class PopMindDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: PopMindDatabase? = null

        fun getInstance(context: Context): PopMindDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PopMindDatabase::class.java,
                "popmind_sessions.db"
            ).build().also { instance = it }
        }
    }
}
