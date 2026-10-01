package com.example.popmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedAt: Long,
    val rapidContentScore: Double,
    val attentionScore: Double,
    val digitalControlScore: Double,
    val studySleepScore: Double,
    val answers: String
)
