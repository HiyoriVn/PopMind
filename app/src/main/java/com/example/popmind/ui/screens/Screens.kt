package com.example.popmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.popmind.ui.theme.Orange
import com.example.popmind.ui.theme.OrangePale
import com.example.popmind.ui.theme.TealPale

@Composable
fun FocusScreen(onStartSession: (String, Boolean, Boolean) -> Unit) {
    var task by remember { mutableStateOf("") }
    var pomodoro by remember { mutableStateOf(true) }
    var music by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("POP-MIND", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Sẵn sàng ngồi\nvào bàn chưa?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
            }
            Box(Modifier.size(76.dp).clip(CircleShape).background(OrangePale), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SelfImprovement, null, tint = Orange, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Dành một chút thời gian cho điều quan trọng nhé.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        Text("Hôm nay bạn muốn làm gì?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = task, onValueChange = { task = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ví dụ: Ôn Toán chương 2") }, shape = RoundedCornerShape(20.dp), singleLine = true)
        Spacer(Modifier.height(18.dp))
        OptionCard(title = "Pomodoro 25/5", subtitle = "Tập trung 25 phút, nghỉ 5 phút", checked = pomodoro, onCheckedChange = { pomodoro = it }, icon = "⏱️")
        Spacer(Modifier.height(12.dp))
        OptionCard(title = "Nhạc nền", subtitle = "Một chút âm thanh dịu nhẹ", checked = music, onCheckedChange = { music = it }, icon = "🎧")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onStartSession(task, pomodoro, music) }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp)) {
            Text("Bắt đầu tập trung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp)); Text("→", fontSize = 22.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text("Chậm lại một chút, bạn sẽ đi xa hơn 🌱", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(TealPale), contentAlignment = Alignment.Center) { Text(icon, fontSize = 21.sp) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun RoadmapScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        PageHeading("Lộ trình", "Sống chậm, sống sâu — theo nhịp của bạn.")
        Spacer(Modifier.height(22.dp))
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = TealPale)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp)); Column { Text("Tuần này", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Xây nhịp học nhẹ nhàng", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Spacer(Modifier.height(18.dp)); Text("Gợi ý dành cho bạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        SuggestionCard("01", "Bắt đầu từ 25 phút", "Chọn một việc nhỏ và dành trọn một phiên cho nó.", "Thử ngay hôm nay", Icons.Rounded.Lightbulb)
        Spacer(Modifier.height(12.dp))
        SuggestionCard("02", "Nghỉ ngắn, nghỉ thật", "Rời màn hình, vươn vai hoặc uống một cốc nước.", "Sau phiên tập trung", Icons.Rounded.SelfImprovement)
        Spacer(Modifier.height(12.dp))
        SuggestionCard("03", "Để điện thoại xa tầm tay", "Tạo một khoảng yên tĩnh để đầu óc dễ vào guồng.", "Mẹo nhỏ", Icons.Rounded.Star)
    }
}

@Composable
fun ProfileScreen(onLoadSample: () -> Unit, onDeleteAll: () -> Unit) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        PageHeading("Hồ sơ", "Góc nhỏ ghi lại hành trình của bạn.")
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(26.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(TealPale), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.width(16.dp)); Column { Text("Bạn học chăm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Thành viên từ hôm nay", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Spacer(Modifier.height(20.dp)); Text("Tổng quan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp)); StatCard("Mục tiêu tuần", "5 / 7 phiên", Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp)); StatCard("Thời gian tập trung", "3 giờ 20 phút", Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = OrangePale)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = Orange, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(12.dp))
                Column { Text("Gói Plus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text("Góc giới thiệu tính năng sẽ sớm có mặt.", style = MaterialTheme.typography.bodySmall) }
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

@Composable
private fun SuggestionCard(number: String, title: String, body: String, tag: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(OrangePale), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Orange) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("GỢI Ý $number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp)); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp)); Text("•  $tag", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
