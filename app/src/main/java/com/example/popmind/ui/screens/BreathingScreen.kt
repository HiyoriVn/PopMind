package com.example.popmind.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import android.os.SystemClock

@Composable
fun BreathingScreen(onExit: () -> Unit) {
    var elapsed by remember { mutableLongStateOf(0L) }
    val startedAt = remember { SystemClock.elapsedRealtime() }
    LaunchedEffect(startedAt) {
        while (elapsed < 180_000L) {
            elapsed = SystemClock.elapsedRealtime() - startedAt
            delay(100)
        }
        elapsed = 180_000L
    }
    BackHandler(onBack = onExit)
    val finished = elapsed >= 180_000L
    val phaseMillis = (elapsed % 14_000L).toInt()
    val phase = when {
        finished -> "Bạn vừa dành 3 phút cho chính mình. Cảm ơn bạn đã chậm lại."
        phaseMillis < 4_000 -> "Hít vào thật nhẹ"
        phaseMillis < 8_000 -> "Giữ hơi một chút"
        else -> "Thở ra từ từ"
    }
    val targetScale = if (finished || phaseMillis < 8_000) 1.12f else 0.78f
    val duration = when { phaseMillis < 4_000 -> 4_000; phaseMillis < 8_000 -> 100; else -> 6_000 }
    val remainingSeconds = ((180_000L - elapsed).coerceAtLeast(0) / 1000).toInt()
    val remainingLabel = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
    val scale by animateFloatAsState(targetScale, tween(duration, easing = LinearEasing), label = "breath")
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Cứu nguy lướt video", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp)); Text("Mình cùng thở chậm trong 3 phút nhé.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(220.dp).scale(scale).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SelfImprovement, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(62.dp))
            }
            Spacer(Modifier.height(30.dp)); Text(phase, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp)); Text(if (finished) "" else remainingLabel, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) { Text(if (finished) "Quay lại" else "Thoát") }
    }
}
