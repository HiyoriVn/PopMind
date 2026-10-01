package com.example.popmind.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.popmind.service.FocusSessionService
import com.example.popmind.session.FocusSessionRepository
import com.example.popmind.session.FocusSessionState
import com.example.popmind.ui.theme.Orange

@Composable
fun SessionScreen(onVolumeChanged: (Float) -> Unit, onFinished: (Long, Int, Boolean) -> Unit) {
    val context = LocalContext.current
    val state by FocusSessionRepository.state.collectAsStateWithLifecycle()
    var encouragement by remember { mutableStateOf("Bạn đang làm rất tốt. Cứ quay lại với việc của mình nhé!") }
    var confirmEnd by remember { mutableStateOf(false) }

    LaunchedEffect(state.phase, state.pinRequested, state.isActive) {
        if (!state.isActive || state.phase == FocusSessionState.PHASE_BREAK) runCatching { (context as? Activity)?.stopLockTask() }
        else if (state.pinRequested) runCatching {
            val activity = context as? Activity
            activity?.startLockTask()
        }
    }
    LaunchedEffect(state.ended) {
        if (state.ended) onFinished(state.elapsedSeconds(), state.interruptions, state.completedResult)
    }
    BackHandler(enabled = state.isActive) { confirmEnd = true }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SelfImprovement, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(if (state.phase == FocusSessionState.PHASE_FOCUS) "Đang tập trung" else "Giờ nghỉ ngắn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text(state.task.ifBlank { "Đang khôi phục phiên…" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatSessionTime(state.secondsRemaining()), style = TextStyle(fontFamily = com.example.popmind.ui.theme.PlusJakartaSans, fontSize = 76.sp, lineHeight = 86.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.primary)
            Text(if (state.pomodoro) "Còn lại trong giai đoạn này" else "Thời gian đã tập trung", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(if (state.phase == FocusSessionState.PHASE_FOCUS) "Chu kỳ đã xong: ${state.completedCycles}" else "Thả lỏng một chút rồi mình tiếp tục nhé", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Đã gián đoạn ${state.interruptions} lần", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp)); Text(encouragement, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {
                encouragement = listOf("Không sao, mình quay lại từ đây nhé 🌱", "Bạn đã nhận ra điều đó — giỏi lắm!", "Từng phút tập trung đều đáng quý.").random()
                context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_INTERRUPT))
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Mình vừa muốn xem điện thoại") }
            Spacer(Modifier.height(4.dp))
            if (state.sound == FocusSessionState.SOUND_OFF) Text("Nhạc nền đang tắt", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else TextButton(onClick = { context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_TOGGLE_MUSIC)) }) {
                Icon(if (state.soundEnabled) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff, null)
                Spacer(Modifier.width(8.dp)); Text(if (state.soundEnabled) "Tắt tiếng nhạc · ${soundName(state.sound)}" else "Bật tiếng nhạc · ${soundName(state.sound)}")
            }
            if (state.sound != FocusSessionState.SOUND_OFF) {
                var volume by remember(state.volume) { mutableFloatStateOf(state.volume) }
                Text("Âm lượng · ${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(value = volume, onValueChange = { volume = it }, onValueChangeFinished = {
                    onVolumeChanged(volume)
                    context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_SET_VOLUME).putExtra(FocusSessionService.EXTRA_VOLUME, volume))
                })
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = { confirmEnd = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = MaterialTheme.colorScheme.onSecondary)) {
                Text("Kết thúc", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (confirmEnd) AlertDialog(
        onDismissRequest = { confirmEnd = false },
        title = { Text("Bỏ cuộc giữa chừng?") },
        text = { Text("Phiên sẽ kết thúc ngay bây giờ. Bạn vẫn có thể bắt đầu lại bất cứ lúc nào.") },
        confirmButton = { TextButton(onClick = {
            confirmEnd = false
            context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_END))
        }) { Text("Kết thúc phiên") } },
        dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Tiếp tục tập trung") } }
    )
}

private fun soundName(sound: String) = when (sound) {
    FocusSessionState.SOUND_RAIN -> "Mưa"
    FocusSessionState.SOUND_LOFI -> "Lo-fi"
    FocusSessionState.SOUND_WHITE -> "Ồn trắng"
    else -> "Tắt"
}

@Composable
fun SessionSummaryScreen(durationSeconds: Long, interruptions: Int, onBackToFocus: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(94.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text("🌟", fontSize = 48.sp) }
        Spacer(Modifier.height(22.dp)); Text("Bạn làm tốt lắm!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp)); Text("Một khoảng thời gian dành trọn cho bản thân — thật đáng tự hào.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Thời lượng phiên", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp)); Text(formatDuration(durationSeconds), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))
                Text("Số lần gián đoạn", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp)); Text("$interruptions lần", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(28.dp)); Button(onClick = onBackToFocus, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Text("Quay lại", fontWeight = FontWeight.Bold) }
    }
}

private fun formatSessionTime(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "$mins phút $secs giây" else "$secs giây"
}
