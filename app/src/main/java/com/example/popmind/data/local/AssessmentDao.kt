package com.example.popmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Insert
    suspend fun insert(assessment: AssessmentEntity): Long

    @Query("SELECT * FROM assessments ORDER BY completedAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<AssessmentEntity?>

    @Query("SELECT * FROM assessments ORDER BY completedAt DESC, id DESC")
    fun observeAll(): Flow<List<AssessmentEntity>>
}
