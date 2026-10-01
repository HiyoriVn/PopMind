package com.example.popmind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.popmind.data.usage.UsageSummary
import com.example.popmind.ui.profile.ProfileViewModel
import com.example.popmind.ui.profile.ProfileUiState
import java.util.Locale

@Composable
fun UsageStatsCard(summary: UsageSummary, onRequestAccess: () -> Unit, onRefresh: () -> Unit) {
    var explain by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Thời gian dùng ứng dụng", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            when {
                summary.loading -> CircularProgressIndicator()
                !summary.hasAccess -> Text("Cho phép xem thống kê để cùng bạn quan sát thói quen dùng ứng dụng trong 7 ngày.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                summary.topApps.isEmpty() -> Text("Chưa có dữ liệu sử dụng ứng dụng trong 7 ngày gần đây. Bạn thử quay lại sau nhé.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    val perDay = summary.shortContentMillis / 7.0 / 3_600_000.0
                    Text("Nhóm nội dung ngắn: ${String.format(Locale.forLanguageTag("vi-VN"), "%.1f", perDay)} giờ/ngày", style = MaterialTheme.typography.titleSmall)
                    Text("Tổng 7 ngày: ${formatMillis(summary.shortContentMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp)); Text("Ứng dụng dùng nhiều nhất", style = MaterialTheme.typography.labelLarge)
                    summary.topApps.forEachIndexed { index, app -> Text("${index + 1}. ${app.label} · ${formatMillis(app.foregroundMillis)}", modifier = Modifier.padding(top = 5.dp)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!summary.hasAccess) Button(onClick = { explain = true }, modifier = Modifier.fillMaxWidth()) { Text("Mở quyền Thời gian sử dụng") }
            else TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Cập nhật số liệu") }
        }
    }
    if (explain) AlertDialog(
        onDismissRequest = { explain = false }, title = { Text("Vì sao cần quyền này?") },
        text = { Text("Pop-Mind dùng quyền Thời gian sử dụng để tổng hợp thời lượng các ứng dụng trong 7 ngày gần đây, ngay trên thiết bị. Bạn có thể bật hoặc tắt quyền này trong Cài đặt bất cứ lúc nào.") },
        confirmButton = { TextButton(onClick = { explain = false; onRequestAccess() }) { Text("Mở Cài đặt") } },
        dismissButton = { TextButton(onClick = { explain = false }) { Text("Để sau") } }
    )
}

@Composable
fun AssessmentProfileCard(state: ProfileUiState, onAssess: () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Hồ sơ tập trung cá nhân", style = MaterialTheme.typography.titleMedium)
            if (state.assessment == null) {
                Spacer(Modifier.height(8.dp)); Text("Trả lời vài câu để cùng nhìn lại thói quen tập trung của bạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(4.dp)); Text("Một góc nhìn nhẹ nhàng về thói quen của bạn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.scores.forEach { score ->
                    Spacer(Modifier.height(12.dp)); Text(score.title, style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        val scoreColor = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.weight(1f).height(12.dp)) {
                            drawLine(trackColor, androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = size.height, cap = StrokeCap.Round)
                            drawLine(scoreColor, androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * (score.score / 5f).toFloat(), size.height / 2), strokeWidth = size.height, cap = StrokeCap.Round)
                        }
                        Text("  ${String.format(Locale.forLanguageTag("vi-VN"), "%.1f", score.score)} / 5", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(14.dp)); Text("Gợi ý ưu tiên quan sát", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                state.priorities.forEach { Text("• $it", modifier = Modifier.padding(top = 4.dp)) }
                Text("Đây không phải chẩn đoán y khoa", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp)); Button(onClick = onAssess, modifier = Modifier.fillMaxWidth()) { Text(if (state.assessment == null) "Làm Hồ sơ tập trung" else "Làm lại Hồ sơ tập trung") }
        }
    }
}

@Composable
fun AssessmentScreen(viewModel: ProfileViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val initialVersion = remember { state.saveVersion }
    LaunchedEffect(state.saveVersion) { if (state.saveVersion > initialVersion) onSaved() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Quay lại") }
        Text("Hồ sơ tập trung", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp)); Text("Trong 2 tuần gần đây, điều này xảy ra với bạn thường xuyên thế nào?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("1 · Hầu như không     5 · Gần như luôn", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelMedium)
        ProfileViewModel.groups.forEachIndexed { groupIndex, group ->
            Spacer(Modifier.height(16.dp)); Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(group, style = MaterialTheme.typography.titleSmall)
                    ProfileViewModel.questions.forEachIndexed { index, question -> if (question.group == groupIndex) {
                        Spacer(Modifier.height(12.dp)); Text(question.text)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { value -> FilterChip(selected = state.answers[index] == value, onClick = { viewModel.selectAnswer(index, value) }, label = { Text(value.toString()) }) }
                        }
                    } }
                }
            }
        }
        Spacer(Modifier.height(16.dp)); Button(onClick = viewModel::submitAssessment, enabled = state.canSubmit && !state.saving, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "Đang lưu…" else "Xem hồ sơ của mình") }
        Spacer(Modifier.height(20.dp))
    }
}

private fun formatMillis(millis: Long): String {
    val minutes = millis / 60_000
    val hours = minutes / 60
    return if (hours > 0) "$hours giờ ${minutes % 60} phút" else "$minutes phút"
}
