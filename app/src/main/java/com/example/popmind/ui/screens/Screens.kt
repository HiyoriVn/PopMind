package com.example.popmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.popmind.ui.theme.Orange
import com.example.popmind.ui.profile.ProfileViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.popmind.session.FocusSettings
import com.example.popmind.session.FocusSessionState
import com.example.popmind.data.PersonalProfile
import java.time.LocalTime

@Composable
fun FocusScreen(
    progressState: com.example.popmind.ui.progress.ProgressUiState,
    personalization: PersonalProfile,
    settings: FocusSettings,
    onFocusMinutes: (Int) -> Unit,
    onBreakMinutes: (Int) -> Unit,
    onSound: (String) -> Unit,
    onVolume: (Float) -> Unit,
    onStartSession: (String, Boolean, Int, Int, String, Float) -> Unit,
    onOpenBreathing: () -> Unit
) {
    var task by remember { mutableStateOf("") }
    var pomodoro by remember { mutableStateOf(true) }
    var customDialog by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("POP-MIND", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                val hour = LocalTime.now().hour
                val greeting = when (hour) { in 5..11 -> "Chào buổi sáng"; in 12..17 -> "Chào buổi chiều"; in 18..21 -> "Chào buổi tối"; else -> "Chào khuya" }
                Text("$greeting, ${personalization.name.ifBlank { "bạn" }}!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Sẵn sàng ngồi\nvào bàn chưa?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
            }
            Box(Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SelfImprovement, null, tint = Orange, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Dành một chút thời gian cho điều quan trọng nhé.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        AssistChip(onClick = {}, modifier = Modifier.height(36.dp), label = { Text("🔥 ${progressState.streakDays} ngày giữ nhịp") }, leadingIcon = { Icon(Icons.Rounded.LocalFireDepartment, null) })
        Spacer(Modifier.height(28.dp))
        Text("Hôm nay bạn muốn làm gì?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = task, onValueChange = { task = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ví dụ: Ôn Toán chương 2") }, shape = RoundedCornerShape(20.dp), singleLine = true)
        val taskSuggestions = (progressState.recentTasks + personalization.subjects).distinct().take(8)
        if (taskSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); Text("Gợi ý của bạn", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                taskSuggestions.forEach { suggestion -> FilterChip(modifier = Modifier.height(36.dp), selected = task == suggestion, onClick = { task = suggestion }, label = { Text(suggestion) }) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("${settings.focusMinutes}:00", style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.primary)
                Text("Tập trung theo nhịp của bạn", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OptionCard(title = "Pomodoro ${settings.focusMinutes}/${settings.breakMinutes}", subtitle = "Tập trung và nghỉ theo nhịp đã chọn", checked = pomodoro, onCheckedChange = { pomodoro = it }, icon = "⏱️")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Thời lượng tập trung", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15, 25, 45, 60).forEach { minutes -> FilterChip(modifier = Modifier.height(36.dp), selected = settings.focusMinutes == minutes, onClick = { onFocusMinutes(minutes) }, label = { Text("$minutes phút") }) }
            FilterChip(modifier = Modifier.height(36.dp), selected = settings.focusMinutes !in listOf(15, 25, 45, 60), onClick = { customMinutes = settings.focusMinutes.toString(); customDialog = true }, label = { Text("Tự chọn") })
        }
        Text("Nghỉ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10).forEach { minutes -> FilterChip(modifier = Modifier.height(36.dp), selected = settings.breakMinutes == minutes, onClick = { onBreakMinutes(minutes) }, label = { Text("$minutes phút") }) }
        }
        Spacer(Modifier.height(12.dp))
        OptionCard(title = "Nhạc nền", subtitle = "Chọn âm thanh dịu nhẹ hoặc tắt", checked = settings.sound != FocusSessionState.SOUND_OFF, onCheckedChange = { onSound(if (it) FocusSessionState.SOUND_RAIN else FocusSessionState.SOUND_OFF) }, icon = "🎧")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(FocusSessionState.SOUND_RAIN to "Mưa", FocusSessionState.SOUND_LOFI to "Lo-fi", FocusSessionState.SOUND_WHITE to "Ồn trắng", FocusSessionState.SOUND_OFF to "Tắt").forEach { (sound, label) ->
                FilterChip(modifier = Modifier.height(36.dp), selected = settings.sound == sound, onClick = { onSound(sound) }, label = { Text(label) })
            }
        }
        if (settings.sound != FocusSessionState.SOUND_OFF) {
            var volume by remember(settings.volume) { mutableFloatStateOf(settings.volume) }
            Text("Âm lượng nhạc · ${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = volume, onValueChange = { volume = it }, onValueChangeFinished = { onVolume(volume) })
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onStartSession(task, pomodoro, settings.focusMinutes, settings.breakMinutes, settings.sound, settings.volume) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
            Text("Bắt đầu tập trung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp)); Text("→", fontSize = 22.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenBreathing, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CircleShape, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
            Icon(Icons.Rounded.SelfImprovement, null); Spacer(Modifier.width(8.dp)); Text("Cứu nguy lướt video")
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Hôm nay", "${progressState.todayMinutes} phút", Modifier.weight(1f))
            StatCard("Phiên hoàn thành", "${progressState.completedCount}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text("Chậm lại một chút, bạn sẽ đi xa hơn 🌱", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
    if (customDialog) AlertDialog(
        onDismissRequest = { customDialog = false }, title = { Text("Chọn số phút tập trung") },
        text = { Column { Text("Nhập số từ 5 đến 120 phút."); OutlinedTextField(value = customMinutes, onValueChange = { customMinutes = it.filter(Char::isDigit).take(3) }, singleLine = true, label = { Text("Phút") }) } },
        confirmButton = { TextButton(onClick = { customMinutes.toIntOrNull()?.takeIf { it in 5..120 }?.let(onFocusMinutes); customDialog = false }) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = { customDialog = false }) { Text("Hủy") } }
    )
}

@Composable
private fun OptionCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text(icon, fontSize = 21.sp) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    progressState: com.example.popmind.ui.progress.ProgressUiState,
    personalization: PersonalProfile,
    favoriteAppLabels: List<String>,
    onLoadSample: () -> Unit,
    onDeleteAll: () -> Unit,
    onOpenAssessment: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onOpenPlus: () -> Unit,
    onEditPersonalization: () -> Unit
) {
    val profileState by profileViewModel.state.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        PageHeading("Hồ sơ", "Góc nhỏ ghi lại hành trình của bạn.")
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("${personalization.name.ifBlank { "Bạn" }} · Lớp ${personalization.grade}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Mình đang hướng tới: ${personalization.subjects.joinToString().ifBlank { "chưa chọn môn học" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Khung giờ cần để ý: ${personalization.distractionWindow}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (favoriteAppLabels.isNotEmpty()) Text("Ứng dụng bạn đã chọn: ${favoriteAppLabels.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onEditPersonalization) { Text("Sửa thông tin và mục tiêu") }
            }
        }
        Spacer(Modifier.height(14.dp))
        UsageStatsCard(profileState.usage, onRequestUsageAccess, profileViewModel::refreshUsage)
        Spacer(Modifier.height(14.dp))
        AssessmentProfileCard(profileState, onOpenAssessment)
        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(26.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.width(16.dp)); Column { Text("Hành trình của bạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Một bước nhỏ mỗi ngày", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Spacer(Modifier.height(20.dp)); Text("Tổng quan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp)); StatCard("Chuỗi ngày tập trung", "${progressState.streakDays} ngày", Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp)); StatCard("Thời gian tập trung · 7 ngày", "${progressState.weekMinutes} phút", Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPlus), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(12.dp))
                Column { Text("Gói Plus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text("Bản demo · không thu phí", style = MaterialTheme.typography.bodySmall) }
            }
        }
        Spacer(Modifier.height(22.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Dữ liệu demo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Dùng để xem thử màn Tiến độ với dữ liệu 7 ngày gần nhất.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onLoadSample, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Nạp dữ liệu mẫu 7 ngày")
                }
                TextButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Xóa dữ liệu", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Xóa toàn bộ dữ liệu?") },
            text = { Text("Các phiên tập trung và dữ liệu demo sẽ bị xóa khỏi thiết bị.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onDeleteAll() }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(6.dp)); Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DayDot(day: String, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(if (done) Orange else MaterialTheme.colorScheme.onPrimary.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Text(if (done) "✓" else "·", color = if (done) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(5.dp)); Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(17.dp)) { Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(6.dp)); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun BadgeCard(title: String, subtitle: String, emoji: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(emoji, fontSize = 27.sp); Spacer(Modifier.height(7.dp)); Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}
