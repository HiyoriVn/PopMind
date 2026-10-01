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
import com.example.popmind.data.local.RoadmapGoalEntity
import com.example.popmind.ui.roadmap.DayBalance
import com.example.popmind.ui.roadmap.RoadmapViewModel

@Composable
fun PersonalizedRoadmapScreen(viewModel: RoadmapViewModel, onOpenUsageSettings: () -> Unit, onOpenPlus: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var balance by remember { mutableStateOf(DayBalance()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("Lộ trình", style = MaterialTheme.typography.headlineLarge)
        Text("${state.profile.name.ifBlank { "Bạn" }} · lớp ${state.profile.grade} — sống chậm, sống sâu theo nhịp của mình.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightCard("Giờ vàng", state.goldenHour, "Phiên hoàn thành nhiều nhất", Modifier.weight(1f))
            InsightCard("Giờ dễ xao nhãng", state.distractionHour, "Nội dung ngắn trong 7 ngày", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Nhịp phiên gợi ý · ${state.recommendedMinutes} phút", style = MaterialTheme.typography.titleMedium)
                Text(state.adjustmentReason, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Nhìn lại tuần này", style = MaterialTheme.typography.titleMedium)
                Text("Thời gian tập trung: ${signed(state.focusWeekChangeMinutes)} phút so với tuần trước", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (state.usage.hasAccess) "Nội dung ngắn: ${formatHourChange(state.shortWeekChangeMillis)} so với tuần trước" else "Bật quyền Thời gian sử dụng để so sánh nội dung ngắn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Gián đoạn trung bình: ${signedDecimal(state.interruptionWeekChange)} lần/phiên", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Mục tiêu tuần này", style = MaterialTheme.typography.titleLarge)
        if (state.goals.isEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) { Column(Modifier.padding(16.dp)) { Text("Đang gợi ý mục tiêu từ nhịp sinh hoạt gần đây…") } }
        } else state.goals.forEachIndexed { index, goal ->
            GoalCard(goal, index + 1) { checked -> viewModel.setGoalCompleted(goal.id, checked) }
            Spacer(Modifier.height(10.dp))
        }
        if (!state.usage.hasAccess && !state.usage.loading) {
            TextButton(onClick = onOpenUsageSettings) { Text("Bật Thời gian sử dụng để cá nhân hóa thêm") }
        }
        Spacer(Modifier.height(18.dp)); BalanceCard(balance) { balance = it }
        Spacer(Modifier.height(18.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Muốn có thêm lựa chọn?", style = MaterialTheme.typography.titleMedium)
                Text("Xem thử Gói Plus", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenPlus) { Text("Khám phá Gói Plus →") }
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp)); Text(value, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun signed(value: Int) = (if (value > 0) "+" else "") + value
private fun signedDecimal(value: Double) = (if (value > 0) "+" else "") + "%.1f".format(java.util.Locale.getDefault(), value)
private fun formatHourChange(millis: Long): String {
    val minutes = kotlin.math.abs(millis) / 60_000
    val formatted = if (minutes >= 60) "${minutes / 60} giờ ${minutes % 60} phút" else "$minutes phút"
    return (if (millis > 0) "+" else if (millis < 0) "−" else "") + formatted
}

@Composable
private fun GoalCard(goal: RoadmapGoalEntity, number: Int, onChecked: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MỤC TIÊU 0$number", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Checkbox(checked = goal.completed, onCheckedChange = onChecked)
            }
            Text(goal.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp)); Text("Vì sao: ${goal.reason}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp)); Text("Thử thay bằng: ${goal.activity}", color = MaterialTheme.colorScheme.secondary)
            if (goal.completed) Text("Đã hoàn thành tuần này ✓", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun BalanceCard(balance: DayBalance, onChange: (DayBalance) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val orange = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Cán cân một ngày", style = MaterialTheme.typography.titleLarge)
            Text("Ước lượng nhanh số giờ trong một ngày của bạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            BalanceSlider("Ngủ", balance.sleep, 4f..12f) { onChange(balance.copy(sleep = it)) }
            BalanceSlider("Học", balance.study, 0f..12f) { onChange(balance.copy(study = it)) }
            BalanceSlider("Vận động", balance.movement, 0f..6f) { onChange(balance.copy(movement = it)) }
            BalanceSlider("Màn hình giải trí", balance.screen, 0f..10f) { onChange(balance.copy(screen = it)) }
            Spacer(Modifier.height(6.dp))
            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val vals = listOf(balance.sleep / 12f, balance.study / 12f, balance.movement / 12f, balance.screen / 12f)
                val colors = listOf(primary, orange, tertiary, error)
                val gap = 5f
                val width = (size.width - gap * 3) / 4
                vals.forEachIndexed { i, value ->
                    drawLine(colors[i].copy(alpha = .2f), androidx.compose.ui.geometry.Offset(i * (width + gap), size.height / 2), androidx.compose.ui.geometry.Offset(i * (width + gap) + width, size.height / 2), 12f, StrokeCap.Round)
                    drawLine(colors[i], androidx.compose.ui.geometry.Offset(i * (width + gap), size.height / 2), androidx.compose.ui.geometry.Offset(i * (width + gap) + width * value.coerceIn(0f, 1f), size.height / 2), 12f, StrokeCap.Round)
                }
            }
            Text("Mỗi vạch biểu thị một khoảng trong ngày — hãy chọn nhịp phù hợp với bạn.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BalanceSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Text("$label · ${value.toInt()} giờ", style = MaterialTheme.typography.labelLarge)
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = (range.endInclusive - range.start).toInt() - 1)
}

@Composable
fun PlusScreen(onBack: () -> Unit) {
    var demoMessage by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        TextButton(onClick = onBack) { Text("← Quay lại") }
        Text("Gói Plus", style = MaterialTheme.typography.headlineLarge)
        Text("Thêm lựa chọn để đồng hành cùng hành trình tập trung.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        PlusCompareCard("Miễn phí", listOf("Phiên tập trung và bộ đếm", "Thống kê cơ bản", "Gợi ý lộ trình tuần"), highlighted = false)
        Spacer(Modifier.height(12.dp)); PlusCompareCard("Plus", listOf("Lộ trình nâng cao", "Báo cáo dành cho phụ huynh", "Nhiều âm thanh nền"), highlighted = true)
        Spacer(Modifier.height(16.dp)); Button(onClick = { demoMessage = true }, modifier = Modifier.fillMaxWidth()) { Text("Nâng cấp") }
        Text("Đây là bản demo, không có thanh toán thật.", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
    if (demoMessage) AlertDialog(onDismissRequest = { demoMessage = false }, title = { Text("Bản demo") }, text = { Text("Đây là bản demo, chưa có thanh toán thật.") }, confirmButton = { TextButton(onClick = { demoMessage = false }) { Text("Đã hiểu") } })
}

@Composable
private fun PlusCompareCard(title: String, features: List<String>, highlighted: Boolean) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
            features.forEach { Text("✓  $it", modifier = Modifier.padding(top = 10.dp), color = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp), verticalArrangement = Arrangement.Center) {
        Text("POP-MIND", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp)); Text("Chào bạn!", style = MaterialTheme.typography.headlineLarge)
        Text("Mình sẽ giúp bạn tạo những khoảng tập trung vừa sức.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp)); Text("Ba quyền có thể được hỏi khi bạn dùng tính năng liên quan:", style = MaterialTheme.typography.titleMedium)
        listOf(
            "Thông báo — để xem đồng hồ phiên đang chạy.",
            "Không làm phiền — để tạm yên lặng trong phiên và khôi phục trạng thái cũ sau đó.",
            "Thời gian sử dụng — để tổng hợp thời lượng ứng dụng trong 7 ngày."
        ).forEach { Text("•  $it", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(22.dp)); Text("Bạn có thể từ chối quyền và vẫn dùng các phần còn lại của ứng dụng.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp)); Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Bắt đầu") }
    }
}
