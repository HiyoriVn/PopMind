package com.example.popmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime >= :fromTime AND startTime < :untilTime ORDER BY startTime DESC")
    fun getSessionsBetween(fromTime: Long, untilTime: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime >= :fromTime AND startTime < :untilTime ORDER BY startTime DESC")
    suspend fun getSessionsBetweenOnce(fromTime: Long, untilTime: Long): List<SessionEntity>

    @Query("UPDATE sessions SET durationSeconds = :durationSeconds, interruptions = :interruptions, completed = :completed WHERE id = :id")
    suspend fun updateSessionResult(id: Long, durationSeconds: Long, interruptions: Int, completed: Boolean)

    @Query("DELETE FROM sessions WHERE task LIKE 'Mẫu demo:%'")
    suspend fun deleteSampleSessions()

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}
