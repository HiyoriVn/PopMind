package com.example.popmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadmapGoalDao {
    @Query("SELECT * FROM roadmap_goals WHERE weekStart = :weekStart ORDER BY id")
    fun observeWeek(weekStart: Long): Flow<List<RoadmapGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(goals: List<RoadmapGoalEntity>)

    @Query("DELETE FROM roadmap_goals WHERE weekStart = :weekStart")
    suspend fun deleteWeek(weekStart: Long)

    @Query("UPDATE roadmap_goals SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)
}
