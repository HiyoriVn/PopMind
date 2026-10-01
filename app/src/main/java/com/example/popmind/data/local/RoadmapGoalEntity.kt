package com.example.popmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roadmap_goals")
data class RoadmapGoalEntity(
    @PrimaryKey val id: String,
    val weekStart: Long,
    val title: String,
    val reason: String,
    val activity: String,
    val completed: Boolean = false
)
