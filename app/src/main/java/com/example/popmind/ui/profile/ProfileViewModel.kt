package com.example.popmind.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.popmind.data.local.AssessmentEntity
import com.example.popmind.data.local.PopMindDatabase
import com.example.popmind.data.usage.UsageStatsRepository
import com.example.popmind.data.usage.UsageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssessmentQuestion(val text: String, val group: Int)
data class AssessmentGroupScore(val title: String, val score: Double)
data class ProfileUiState(
    val usage: UsageSummary = UsageSummary(),
    val assessment: AssessmentEntity? = null,
    val scores: List<AssessmentGroupScore> = emptyList(),
    val priorities: List<String> = emptyList(),
    val answers: List<Int> = List(10) { 0 },
    val saving: Boolean = false,
    val saveVersion: Int = 0
) { val canSubmit get() = answers.all { it in 1..5 } }

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = PopMindDatabase.getInstance(application).assessmentDao()
    private val usageRepository = UsageStatsRepository(application)
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            dao.observeLatest().collect { assessment ->
                val scores = assessment?.let {
                    listOf(
                        AssessmentGroupScore(groups[0], it.rapidContentScore),
                        AssessmentGroupScore(groups[1], it.attentionScore),
                        AssessmentGroupScore(groups[2], it.digitalControlScore),
                        AssessmentGroupScore(groups[3], it.studySleepScore)
                    )
                }.orEmpty()
                _state.update { it.copy(assessment = assessment, scores = scores, priorities = scores.sortedByDescending { score -> score.score }.take(2).map { score -> score.title }) }
            }
        }
        refreshUsage()
    }

    fun refreshUsage() {
        viewModelScope.launch {
            _state.update { it.copy(usage = it.usage.copy(loading = true)) }
            val summary = usageRepository.readLastSevenDays()
            _state.update { it.copy(usage = summary) }
        }
    }

    fun selectAnswer(index: Int, value: Int) {
        if (index !in questions.indices || value !in 1..5) return
        _state.update { current -> current.copy(answers = current.answers.toMutableList().also { it[index] = value }) }
    }

    fun submitAssessment() {
        val answers = _state.value.answers
        if (answers.any { it !in 1..5 } || _state.value.saving) return
        val averages = (0..3).map { group ->
            questions.indices.filter { questions[it].group == group }.map { answers[it].toDouble() }.average()
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            dao.insert(AssessmentEntity(
                completedAt = System.currentTimeMillis(), rapidContentScore = averages[0], attentionScore = averages[1],
                digitalControlScore = averages[2], studySleepScore = averages[3], answers = answers.joinToString(",")
            ))
            _state.update { it.copy(saving = false, answers = List(10) { 0 }, saveVersion = it.saveVersion + 1) }
        }
    }

    companion object {
        val groups = listOf("Tiếp nhận nội dung số nhanh", "Duy trì chú ý", "Tự kiểm soát hành vi số", "Ảnh hưởng đến học tập và giấc ngủ")
        val questions = listOf(
            AssessmentQuestion("Mình thường xem các video ngắn nối tiếp nhau lâu hơn dự định.", 0),
            AssessmentQuestion("Khi có vài phút trống, mình thường mở nội dung ngắn để xem.", 0),
            AssessmentQuestion("Mình hay kiểm tra điện thoại khi đang học.", 1),
            AssessmentQuestion("Mình khó quay lại bài đang làm sau khi bị thông báo làm gián đoạn.", 1),
            AssessmentQuestion("Mình thường chuyển sang việc khác trước khi hoàn thành việc đang làm.", 1),
            AssessmentQuestion("Mình cầm điện thoại dù trước đó đã định cất đi.", 2),
            AssessmentQuestion("Mình hay mở điện thoại theo thói quen dù chưa có việc cần làm.", 2),
            AssessmentQuestion("Mình dùng điện thoại sau 22h gần như mỗi ngày.", 3),
            AssessmentQuestion("Việc dùng điện thoại khiến mình bắt đầu bài tập muộn hơn dự định.", 3),
            AssessmentQuestion("Mình thường ngủ muộn vì xem nội dung trên điện thoại.", 3)
        )
    }
}
