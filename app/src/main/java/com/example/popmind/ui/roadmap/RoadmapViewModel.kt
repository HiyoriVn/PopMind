package com.example.popmind.ui.roadmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.popmind.data.SessionRepository
import com.example.popmind.data.local.AssessmentEntity
import com.example.popmind.data.local.PopMindDatabase
import com.example.popmind.data.local.RoadmapGoalEntity
import com.example.popmind.data.local.SessionEntity
import com.example.popmind.data.usage.UsageStatsRepository
import com.example.popmind.data.usage.UsageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class DayBalance(val sleep: Float = 8f, val study: Float = 6f, val movement: Float = 1f, val screen: Float = 3f)
data class RoadmapUiState(val goals: List<RoadmapGoalEntity> = emptyList(), val usage: UsageSummary = UsageSummary(), val sessionCount: Int = 0)

object RoadmapEngine {
    // Quy tắc chọn hai mục tiêu tuần dựa trên dữ liệu gần nhất của người dùng.
    fun generate(weekStart: Long, sessions: List<SessionEntity>, assessment: AssessmentEntity?, usage: UsageSummary): List<RoadmapGoalEntity> {
        val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recent = sessions.filter { it.startTime >= since }
        val avgInterruptions = if (recent.isEmpty()) 0.0 else recent.map { it.interruptions }.average()
        val perDay = usage.shortContentMillis / 7.0 / 3_600_000.0
        val candidates = mutableListOf<RoadmapGoalEntity>()
        if (usage.hasAccess && perDay > 2.0) candidates += RoadmapGoalEntity("screen", weekStart, "Giảm 20 phút/ngày", "Nội dung ngắn chiếm khoảng ${"%.1f".format(java.util.Locale.US, perDay)} giờ mỗi ngày trong 7 ngày qua.", "Thay 20 phút lướt màn hình bằng đọc sách hoặc đi bộ nhẹ.")
        if (avgInterruptions > 2.0) candidates += RoadmapGoalEntity("interruptions", weekStart, "Thử phiên 15 phút, tăng dần", "Bạn có trung bình ${"%.1f".format(java.util.Locale.US, avgInterruptions)} lần gián đoạn mỗi phiên gần đây.", "Đặt điện thoại xa tay và thử vẽ hoặc xếp bàn trong giờ nghỉ.")
        if (recent.size < 21) candidates += RoadmapGoalEntity("sessions", weekStart, "Thêm 1 phiên tập trung mỗi ngày", "7 ngày qua bạn có ${recent.size} phiên, tương đương ${"%.1f".format(java.util.Locale.US, recent.size / 7.0)} phiên mỗi ngày.", "Chọn một bài nhỏ để bắt đầu; sau đó nghỉ bằng cách đi bộ vài phút.")
        if ((assessment?.studySleepScore ?: 0.0) >= 3.5) candidates += RoadmapGoalEntity("sleep", weekStart, "Tạo vùng không màn hình 30 phút trước khi ngủ", "Điểm nhóm học tập và giấc ngủ của bạn là ${"%.1f".format(java.util.Locale.US, assessment?.studySleepScore ?: 0.0)} / 5.", "Đọc vài trang sách giấy hoặc chuẩn bị đồ cho ngày mai.")
        if (candidates.size < 2) {
            candidates += RoadmapGoalEntity("gentle_focus", weekStart, "Giữ một khoảng học yên tĩnh", "Một nhịp nhỏ đều đặn giúp bạn quan sát điều gì phù hợp với mình.", "Thử nghe âm thanh thiên nhiên hoặc ngồi thở chậm 3 phút.")
        }
        if (candidates.size < 2) candidates += RoadmapGoalEntity("movement", weekStart, "Nghỉ giữa giờ bằng vận động nhẹ", "Khoảng nghỉ rời màn hình có thể giúp bạn quay lại thoải mái hơn.", "Đi bộ, vươn vai hoặc tưới cây trong 5 phút.")
        return candidates.take(2).map { it.copy(id = "${weekStart}_${it.id}") }
    }
}

class RoadmapViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PopMindDatabase.getInstance(application)
    private val goalDao = database.roadmapGoalDao()
    private val sessionRepository = SessionRepository(database)
    private val assessmentDao = database.assessmentDao()
    private val usageRepository = UsageStatsRepository(application)
    private val weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val usage = MutableStateFlow(UsageSummary(loading = true))
    private val goals = goalDao.observeWeek(weekStart)
    private val sessionCounts = sessionRepository.allSessions
    private val latestAssessment = assessmentDao.observeLatest()

    val state: StateFlow<RoadmapUiState> = combine(goals, usage, sessionCounts) { storedGoals, usageSummary, sessions ->
        RoadmapUiState(storedGoals, usageSummary, sessions.count { it.startTime >= System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoadmapUiState())

    init {
        refreshUsage()
        viewModelScope.launch {
            combine(sessionCounts, latestAssessment, usage) { sessions, assessment, usageSummary -> Triple(sessions, assessment, usageSummary) }
                .collect { (sessions, assessment, usageSummary) ->
                    val existing = goalsSnapshot.value
                    if (existing.isEmpty()) goalDao.insertAll(RoadmapEngine.generate(weekStart, sessions, assessment, usageSummary))
                }
        }
    }

    private val goalsSnapshot = goals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setGoalCompleted(id: String, completed: Boolean) { viewModelScope.launch { goalDao.setCompleted(id, completed) } }
    fun refreshUsage() { viewModelScope.launch { usage.value = usageRepository.readLastSevenDays() } }
}
