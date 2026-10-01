package com.example.popmind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.popmind.ui.progress.ProgressBadge
import com.example.popmind.ui.progress.ProgressViewModel
import com.example.popmind.ui.progress.shortDayLabel
import java.util.Locale

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("Tiến độ", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("Mỗi ngày một chút, bạn đang làm rất tốt.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Chuỗi tập trung", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                    Text("${state.streakDays} ngày liên tiếp", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressStat("Hôm nay", "${state.todayMinutes} phút", Modifier.weight(1f))
            ProgressStat("7 ngày", "${state.weekMinutes} phút", Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        Text("Tập trung trong 7 ngày", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                SessionChart(state.lastSevenDays.map { it.minutes })
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    state.lastSevenDays.forEach { day ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.date.shortDayLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${day.minutes}′", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Gián đoạn trung bình", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.getDefault(), "%.1f lần / phiên", state.averageInterruptions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("🌿", fontSize = 28.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Thử thách 21 ngày Focus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${state.challengeDays.count { it }} / 21 ngày có phiên hoàn thành", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("🌱", fontSize = 25.sp)
        }
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.challengeDays.chunked(7).forEachIndexed { weekIndex, week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        week.forEachIndexed { index, completed ->
                            val dayNumber = weekIndex * 7 + index + 1
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$dayNumber", color = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("Huy hiệu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        state.badges.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { badge -> BadgeTile(badge, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SessionChart(minutes: List<Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(132.dp)) {
        val gridColor = muted.copy(alpha = .7f)
        val baseline = size.height - 4.dp.toPx()
        for (line in 0..3) {
            val y = baseline - (baseline - 8.dp.toPx()) * line / 3f
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val maxMinutes = (minutes.maxOrNull() ?: 0).coerceAtLeast(25)
        val slotWidth = size.width / 7f
        val barWidth = slotWidth * .46f
        minutes.forEachIndexed { index, value ->
            val barHeight = ((baseline - 10.dp.toPx()) * value / maxMinutes).coerceAtLeast(if (value > 0) 8.dp.toPx() else 0f)
            val left = slotWidth * index + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = if (value > 0) primary else muted,
                topLeft = androidx.compose.ui.geometry.Offset(left, baseline - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx())
            )
        }
    }
}

@Composable
private fun ProgressStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BadgeTile(badge: ProgressBadge, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (badge.unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (badge.unlocked) MaterialTheme.colorScheme.secondary.copy(alpha = .22f) else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(if (badge.unlocked) badge.emoji else "🔒", fontSize = 22.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(badge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(badge.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
