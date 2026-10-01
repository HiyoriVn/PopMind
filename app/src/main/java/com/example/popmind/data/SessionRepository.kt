package com.example.popmind.data

import com.example.popmind.data.local.PopMindDatabase
import com.example.popmind.data.local.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SessionRepository(private val database: PopMindDatabase) {
    val allSessions: Flow<List<SessionEntity>> = database.sessionDao().getAllSessions()

    suspend fun insertSession(session: SessionEntity): Long = database.sessionDao().insertSession(session)

    suspend fun updateSessionResult(id: Long, durationSeconds: Long, interruptions: Int, completed: Boolean) {
        database.sessionDao().updateSessionResult(id, durationSeconds, interruptions, completed)
    }

    suspend fun deleteAllSessions() = database.sessionDao().deleteAllSessions()

    suspend fun addDemoWeek(now: Long = System.currentTimeMillis()) {
        val dao = database.sessionDao()
        dao.deleteSampleSessions()
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val samples = mutableListOf<SessionEntity>()
        for (daysAgo in 6 downTo 0) {
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val count = if (daysAgo % 3 == 0 || daysAgo == 0) 2 else 1
            repeat(count) { index ->
                val minutes = if (index == 0 && daysAgo % 2 == 0) 50 else 25
                samples += SessionEntity(
                    startTime = day.timeInMillis + (9 + index * 2) * 60 * 60 * 1000L,
                    durationSeconds = minutes * 60L,
                    task = "Mẫu demo: ${if (index == 0) "Ôn bài" else "Bài tập"} · ngày ${7 - daysAgo}",
                    interruptions = if (daysAgo == 3 && index == 0) 0 else (daysAgo + index) % 3,
                    usedPomodoro = true,
                    completed = true
                )
            }
        }
        samples.forEach { dao.insertSession(it) }
    }
}
