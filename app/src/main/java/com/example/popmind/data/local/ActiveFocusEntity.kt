package com.example.popmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_focus")
data class ActiveFocusEntity(
    @PrimaryKey val id: Int = 1,
    val task: String,
    val pomodoro: Boolean,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val phase: String,
    val startedAtWall: Long,
    val startedAtElapsed: Long,
    val phaseEndElapsed: Long,
    val interruptions: Int,
    val completedCycles: Int,
    val sound: String,
    val soundEnabled: Boolean,
    val volume: Float,
    val pinRequested: Boolean,
    val tryDnd: Boolean,
    val oldInterruptionFilter: Int,
    val changedDnd: Boolean,
    val sessionId: Long
)
