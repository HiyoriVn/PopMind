package com.example.popmind.ui.roadmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.popmind.data.SessionRepository
import com.example.popmind.data.PersonalProfile
import com.example.popmind.data.PersonalizationStore
import com.example.popmind.data.local.AssessmentEntity
import com.example.popmind.data.local.PopMindDatabase
import com.example.popmind.data.local.RoadmapGoalEntity
import com.example.popmind.data.local.SessionEntity
import com.example.popmind.data.usage.UsageStatsRepository
import com.example.popmind.data.usage.UsageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class DayBalance(val sleep: Float = 8f, val study: Float = 6f, val movement: Float = 1f, val screen: Float = 3f)
data class RoadmapUiState(
    val goals: List<RoadmapGoalEntity> = emptyList(), val usage: UsageSummary = UsageSummary(), val sessionCount: Int = 0,
    val profile: PersonalProfile = PersonalProfile(), val goldenHour: String = "Chưa đủ dữ liệu",
    val distractionHour: String = "Chưa có dữ liệu sử dụng", val recommendedMinutes: Int = 25,
    val adjustmentReason: String = "Hoàn thành vài phiên để mình gợi ý nhịp phù hợp.",
    val focusWeekChangeMinutes: Int = 0, val shortWeekChangeMillis: Long = 0, val interruptionWeekChange: Double = 0.0
)

object RoadmapEngine {
    // Quy tắc chọn hai mục tiêu tuần dựa trên dữ liệu gần nhất của người dùng.
    fun generate(weekStart: Long, sessions: List<SessionEntity>, assessment: AssessmentEntity?, usage: UsageSummary, profile: PersonalProfile = PersonalProfile()): List<RoadmapGoalEntity> {
        val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recent = sessions.filter { it.startTime >= since }
        val avgInterruptions = if (recent.isEmpty()) 0.0 else recent.map { it.interruptions }.average()
        val perDay = usage.shortContentMillis / 7.0 / 3_600_000.0
        val candidates = mutableListOf<RoadmapGoalEntity>()
        val chosenEntertainment = usage.topApps.firstOrNull { it.packageName in profile.favoriteApps }
        if (usage.hasAccess && perDay > 2.0) candidates += RoadmapGoalEntity("screen", weekStart, "Giảm 20 phút/ngày", "Nội dung ngắn chiếm khoảng ${"%.1f".format(java.util.Locale.US, perDay)} giờ mỗi ngày trong 7 ngày qua${chosenEntertainment?.let { "; ${it.label} là app bạn đã chọn" }.orEmpty()}.", "Thay 20 phút lướt màn hình bằng đọc sách hoặc đi bộ nhẹ.")
        if (avgInterruptions > 2.0) {
            val commonHour = recent.filter { it.interruptions > 0 }.groupingBy { java.time.Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).hour }.eachCount().maxByOrNull { it.value }
            val hourReason = commonHour?.let { entry ->
                val activeDays = recent.map { java.time.Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() }.distinct().size
                val days = recent.filter { it.interruptions > 0 && java.time.Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).hour == entry.key }
                    .map { java.time.Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() }.distinct().size
                "Cậu hay ngắt phiên lúc ${entry.key}h; khung này xuất hiện $days/$activeDays ngày có phiên trong tuần qua."
            } ?: "7 ngày qua, bạn có trung bình ${"%.1f".format(java.util.Locale.US, avgInterruptions)} lần gián đoạn mỗi phiên."
            candidates += RoadmapGoalEntity("interruptions", weekStart, "Thử phiên 15 phút, tăng dần", hourReason, "Đặt điện thoại xa tay và thử vẽ hoặc xếp bàn trong giờ nghỉ.")
        }
        if (recent.size < 21) candidates += RoadmapGoalEntity("sessions", weekStart, "Thêm 1 phiên mỗi ngày", "7 ngày qua bạn có ${recent.size} phiên, tương đương ${"%.1f".format(java.util.Locale.US, recent.size / 7.0)} phiên mỗi ngày${profile.subjects.firstOrNull()?.let { " khi học $it" }.orEmpty()}.", "Chọn một bài nhỏ để bắt đầu; sau đó nghỉ bằng cách đi bộ vài phút.")
        if ((assessment?.studySleepScore ?: 0.0) >= 3.5) candidates += RoadmapGoalEntity("sleep", weekStart, "Tạo vùng không màn hình 30 phút trước khi ngủ", "Điểm nhóm học tập và giấc ngủ của bạn là ${"%.1f".format(java.util.Locale.US, assessment?.studySleepScore ?: 0.0)} / 5.", "Đọc vài trang sách giấy hoặc chuẩn bị đồ cho ngày mai.")
        if (usage.hasAccess && usage.shortContentByHour.isNotEmpty() && candidates.size < 3) {
            val peak = usage.shortContentByHour.maxByOrNull { it.value }!!.key
            candidates += RoadmapGoalEntity("distraction_hour", weekStart, "Chuẩn bị trước giờ dễ xao nhãng", "Nội dung ngắn được dùng nhiều nhất quanh ${peak}h; bạn đã chọn khung ${profile.distractionWindow} là lúc thường dễ xao nhãng.", "Trước khung giờ đó, để điện thoại xa bàn và đọc sách hoặc đi bộ 10 phút.")
        }
        if (candidates.size < 2) {
            candidates += RoadmapGoalEntity("gentle_focus", weekStart, "Giữ một khoảng học yên tĩnh", "7 ngày qua bạn ghi nhận ${recent.size} phiên${profile.subjects.firstOrNull()?.let { " khi học $it" }.orEmpty()} — mình bắt đầu từ nhịp vừa sức nhé.", "Thử nghe âm thanh thiên nhiên hoặc ngồi thở chậm 3 phút.")
        }
        if (candidates.size < 2) candidates += RoadmapGoalEntity("movement", weekStart, "Nghỉ giữa giờ bằng vận động nhẹ", "Bạn đã chọn khung ${profile.distractionWindow} là lúc dễ xao nhãng; hãy thử một khoảng nghỉ không màn hình ở khung đó.", "Đi bộ, vươn vai hoặc tưới cây trong 5 phút.")
        return candidates.take(2).map { it.copy(id = "${weekStart}_${it.id}") }
    }
}

class RoadmapViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PopMindDatabase.getInstance(application)
    private val goalDao = database.roadmapGoalDao()
    private val sessionRepository = SessionRepository(database)
    private val assessmentDao = database.assessmentDao()
    private val usageRepository = UsageStatsRepository(application)
    private val personalizationStore = PersonalizationStore(application)
    private val weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val usage = MutableStateFlow(UsageSummary(loading = true))
    private val previousUsage = MutableStateFlow(UsageSummary(loading = true))
    private val profile = personalizationStore.profile.stateIn(viewModelScope, SharingStarted.Eagerly, PersonalProfile())
    private val goals = goalDao.observeWeek(weekStart)
    private val sessionCounts = sessionRepository.allSessions
    private val latestAssessment = assessmentDao.observeLatest()

    val state: StateFlow<RoadmapUiState> = combine(goals, usage, sessionCounts, profile, previousUsage) { storedGoals, usageSummary, sessions, personal, priorUsage ->
        val now = System.currentTimeMillis()
        val week = 7L * 24 * 60 * 60 * 1000
        val currentSessions = sessions.filter { it.startTime >= now - week }
        val previousSessions = sessions.filter { it.startTime in (now - week * 2)..(now - week) }
        val currentInterruptions = if (currentSessions.isEmpty()) 0.0 else currentSessions.map { it.interruptions }.average()
        val oldInterruptions = if (previousSessions.isEmpty()) 0.0 else previousSessions.map { it.interruptions }.average()
        val goldenHour = currentSessions.filter { it.completed }.groupingBy { java.time.Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).hour }.eachCount().maxByOrNull { it.value }?.key?.let { "%02d:00–%02d:00".format(it, (it + 1) % 24) } ?: "Chưa đủ dữ liệu"
        val distractionHour = usageSummary.shortContentByHour.maxByOrNull { it.value }?.key?.let { "%02d:00–%02d:00".format(it, (it + 1) % 24) } ?: "Chưa có dữ liệu sử dụng"
        val lastThree = sessions.filter { it.completed }.sortedByDescending { it.startTime }.take(3)
        val adjustDown = currentInterruptions > 2.0
        val adjustUp = lastThree.size == 3 && lastThree.all { it.interruptions == 0 }
        val usualDuration = lastThree.firstOrNull()?.durationSeconds?.div(60)?.toInt() ?: 25
        val suggested = when { adjustDown -> (usualDuration - 5).coerceAtLeast(15); adjustUp -> (usualDuration + 5).coerceAtMost(120); else -> usualDuration.coerceIn(5, 120) }
        RoadmapUiState(
            goals = storedGoals, usage = usageSummary, sessionCount = currentSessions.size, profile = personal,
            goldenHour = goldenHour, distractionHour = distractionHour, recommendedMinutes = suggested,
            adjustmentReason = when { adjustDown -> "Trung bình ${"%.1f".format(java.util.Locale.US, currentInterruptions)} gián đoạn/phiên tuần này — thử ngắn hơn 5 phút nhé."; adjustUp -> "3 phiên gần nhất không gián đoạn — bạn có thể thử tăng thêm 5 phút."; else -> "Giữ nhịp ${suggested} phút hiện tại, rồi xem cơ thể và sự tập trung phản hồi ra sao." },
            focusWeekChangeMinutes = (currentSessions.sumOf { it.durationSeconds } - previousSessions.sumOf { it.durationSeconds }).div(60).toInt(),
            shortWeekChangeMillis = usageSummary.shortContentMillis - priorUsage.shortContentMillis,
            interruptionWeekChange = currentInterruptions - oldInterruptions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoadmapUiState())

    init {
        refreshUsage()
        viewModelScope.launch {
            combine(sessionCounts, latestAssessment, usage, profile) { sessions, assessment, usageSummary, personal -> Quad(sessions, assessment, usageSummary, personal) }
                .collect { input ->
                    val existing = goalsSnapshot.value
                    val changedProfile = hasGeneratedGoals && lastGeneratedProfile != input.profile
                    val assessmentAt = input.assessment?.completedAt
                    val changedAssessment = hasGeneratedGoals && lastGeneratedAssessmentAt != assessmentAt
                    val changedUsageAccess = hasGeneratedGoals && lastGeneratedUsageAccess != input.usage.hasAccess
                    val sessionFingerprint = input.sessions.hashCode()
                    val usageFingerprint = 31 * input.usage.shortContentMillis.hashCode() + input.usage.shortContentByHour.hashCode()
                    val changedData = hasGeneratedGoals && (lastSessionFingerprint != sessionFingerprint || lastUsageFingerprint != usageFingerprint)
                    if (!input.usage.loading && (existing.isEmpty() || changedProfile || changedAssessment || changedUsageAccess || changedData)) {
                        val completedById = existing.associate { it.id to it.completed }
                        goalDao.deleteWeek(weekStart)
                        val refreshed = RoadmapEngine.generate(weekStart, input.sessions, input.assessment, input.usage, input.profile)
                            .map { goal -> goal.copy(completed = completedById[goal.id] ?: false) }
                        goalDao.insertAll(refreshed)
                    }
                    lastGeneratedProfile = input.profile
                    lastGeneratedAssessmentAt = assessmentAt
                    lastGeneratedUsageAccess = input.usage.hasAccess
                    lastSessionFingerprint = sessionFingerprint
                    lastUsageFingerprint = usageFingerprint
                    hasGeneratedGoals = true
                }
        }
    }

    private val goalsSnapshot = goals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setGoalCompleted(id: String, completed: Boolean) { viewModelScope.launch { goalDao.setCompleted(id, completed) } }
    private var lastGeneratedProfile: PersonalProfile? = null
    private var lastGeneratedAssessmentAt: Long? = null
    private var lastGeneratedUsageAccess: Boolean? = null
    private var lastSessionFingerprint: Int = 0
    private var lastUsageFingerprint: Int = 0
    private var hasGeneratedGoals = false
    private fun refreshUsage() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val week = 7L * 24 * 60 * 60 * 1000
            usage.value = usageRepository.readRange(now - week, now)
            previousUsage.value = usageRepository.readRange(now - week * 2, now - week)
        }
    }

    fun refreshUsageAccess() = refreshUsage()
}

private data class Quad(val sessions: List<SessionEntity>, val assessment: AssessmentEntity?, val usage: UsageSummary, val profile: PersonalProfile)
