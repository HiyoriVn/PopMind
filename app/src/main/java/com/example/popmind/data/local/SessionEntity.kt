package com.example.popmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val durationSeconds: Long,
    val task: String,
    val interruptions: Int,
    val usedPomodoro: Boolean,
    val completed: Boolean
)
