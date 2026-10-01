package com.example.popmind.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.popmind.data.SessionRepository
import com.example.popmind.data.local.SessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class DailyProgress(val date: LocalDate, val minutes: Int)

data class ProgressBadge(val title: String, val detail: String, val emoji: String, val unlocked: Boolean)

data class ProgressUiState(
    val streakDays: Int = 0,
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val averageInterruptions: Double = 0.0,
    val completedCount: Int = 0,
    val lastSevenDays: List<DailyProgress> = emptyList(),
    val challengeDays: List<Boolean> = emptyList(),
    val badges: List<ProgressBadge> = emptyList()
)

class ProgressViewModel(private val repository: SessionRepository) : ViewModel() {
    val state: StateFlow<ProgressUiState> = repository.allSessions
        .map(::calculateProgress)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun loadDemoWeek() {
        viewModelScope.launch { repository.addDemoWeek() }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAllSessions() }
    }

    private fun calculateProgress(sessions: List<SessionEntity>): ProgressUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val byDay = sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.startTime).atZone(zone).toLocalDate()
        }
        val firstDay = today.minusDays(6)
        val sevenDates = (0..6).map { firstDay.plusDays(it.toLong()) }
        val sevenDays = sevenDates.map { day ->
            DailyProgress(day, (byDay[day].orEmpty().sumOf { it.durationSeconds } / 60L).toInt())
        }
        val challengeDates = (0..20).map { today.minusDays((20 - it).toLong()) }
        val challenge = challengeDates.map { day -> byDay[day].orEmpty().any { it.completed } }
        val streakAnchor = if (byDay[today].orEmpty().isNotEmpty()) today else today.minusDays(1)
        var streak = 0
        var cursor = streakAnchor
        while (byDay[cursor].orEmpty().isNotEmpty()) {
            streak++
            cursor = cursor.minusDays(1)
        }
        val completedCount = sessions.count { it.completed }
        val uninterrupted = sessions.any { it.completed && it.interruptions == 0 }
        val average = if (sessions.isEmpty()) 0.0 else sessions.map { it.interruptions }.average()
        return ProgressUiState(
            streakDays = streak,
            todayMinutes = sevenDays.last().minutes,
            weekMinutes = sevenDays.sumOf { it.minutes },
            averageInterruptions = average,
            completedCount = completedCount,
            lastSevenDays = sevenDays,
            challengeDays = challenge,
            badges = listOf(
                ProgressBadge("Phiên đầu tiên", "Hoàn thành phiên đầu", "🌱", completedCount >= 1),
                ProgressBadge("Bền bỉ", "3 ngày liên tiếp", "🔥", streak >= 3),
                ProgressBadge("Không gián đoạn", "Một phiên không gián đoạn", "✨", uninterrupted),
                ProgressBadge("10 phiên", "Hoàn thành 10 phiên", "🏅", completedCount >= 10)
            )
        )
    }
}

fun LocalDate.shortDayLabel(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi-VN")).replaceFirstChar { it.uppercase() }
