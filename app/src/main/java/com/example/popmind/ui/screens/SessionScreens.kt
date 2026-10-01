package com.example.popmind.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.popmind.service.FocusSessionService
import com.example.popmind.ui.theme.Orange
import com.example.popmind.ui.theme.OrangePale
import com.example.popmind.ui.theme.TealPale

@Composable
fun SessionScreen(onFinished: (Long, Int) -> Unit) {
    val context = LocalContext.current
    var task by remember { mutableStateOf("Đang chuẩn bị phiên…") }
    var phase by remember { mutableStateOf(FocusSessionService.PHASE_FOCUS) }
    var seconds by remember { mutableLongStateOf(25 * 60L) }
    var interruptions by remember { mutableIntStateOf(0) }
    var music by remember { mutableStateOf(false) }
    var encouragement by remember { mutableStateOf("Bạn đang làm rất tốt. Cứ quay lại với việc của mình nhé!") }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    FocusSessionService.ACTION_UPDATE -> {
                        task = intent.getStringExtra(FocusSessionService.EXTRA_TASK).orEmpty()
                        phase = intent.getStringExtra(FocusSessionService.EXTRA_PHASE) ?: FocusSessionService.PHASE_FOCUS
                        seconds = intent.getLongExtra(FocusSessionService.EXTRA_REMAINING, 0L)
                        interruptions = intent.getIntExtra(FocusSessionService.EXTRA_INTERRUPTS, 0)
                        music = intent.getBooleanExtra(FocusSessionService.EXTRA_MUSIC, false)
                    }
                    FocusSessionService.ACTION_ENDED -> onFinished(
                        intent.getLongExtra(FocusSessionService.EXTRA_ELAPSED, 0L),
                        intent.getIntExtra(FocusSessionService.EXTRA_INTERRUPTS, 0)
                    )
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(FocusSessionService.ACTION_UPDATE)
            addAction(FocusSessionService.ACTION_ENDED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_REFRESH))
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(TealPale), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SelfImprovement, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(if (phase == FocusSessionService.PHASE_FOCUS) "Đang tập trung" else "Giờ nghỉ ngắn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text(task, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatSessionTime(seconds), fontSize = 76.sp, lineHeight = 82.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(if (phase == FocusSessionService.PHASE_FOCUS) "Cứ tập trung vào một việc thôi" else "Thả lỏng một chút rồi mình tiếp tục nhé", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Đã gián đoạn $interruptions lần", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(encouragement, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = {
                    encouragement = listOf("Không sao, mình quay lại từ đây nhé 🌱", "Bạn đã nhận ra điều đó — giỏi lắm!", "Từng phút tập trung đều đáng quý.").random()
                    context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_INTERRUPT))
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)
            ) { Text("Mình vừa muốn xem điện thoại") }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_TOGGLE_MUSIC)) }) {
                Icon(if (music) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff, null)
                Spacer(Modifier.width(8.dp)); Text(if (music) "Tắt nhạc nền" else "Bật nhạc nền")
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { context.startService(Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_END)) },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) { Text("Kết thúc", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun SessionSummaryScreen(durationSeconds: Long, interruptions: Int, onBackToFocus: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(94.dp).clip(CircleShape).background(OrangePale), contentAlignment = Alignment.Center) { Text("🌟", fontSize = 48.sp) }
        Spacer(Modifier.height(22.dp))
        Text("Bạn làm tốt lắm!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp)); Text("Một khoảng thời gian dành trọn cho bản thân — thật đáng tự hào.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Thời lượng phiên", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp)); Text(formatDuration(durationSeconds), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp)); HorizontalDivider()
                Spacer(Modifier.height(16.dp)); Text("Số lần gián đoạn", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp)); Text("$interruptions lần", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onBackToFocus, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Text("Quay lại", fontWeight = FontWeight.Bold) }
    }
}

private fun formatSessionTime(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "$mins phút $secs giây" else "$secs giây"
}
