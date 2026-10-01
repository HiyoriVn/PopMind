package com.example.popmind.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.popmind.data.PersonalProfile
import com.example.popmind.ui.profile.PersonalizationViewModel

@Composable
fun PersonalizationScreen(viewModel: PersonalizationViewModel, editing: Boolean, onSaved: (PersonalProfile) -> Unit, onCancel: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(PersonalProfile()) }
    var step by remember { mutableIntStateOf(0) }
    var customSubject by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(state.loaded) {
        if (state.loaded && !initialized) { draft = state.profile; initialized = true }
    }
    if (!state.loaded) { Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }; return }

    val steps = listOf("Một chút về bạn", "Môn học của bạn", "Khung giờ dễ xao nhãng", "Ứng dụng bạn hay mở")
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp)) {
        if (editing) TextButton(onClick = onCancel) { Text("← Quay lại") }
        Text(if (editing) "Thông tin cá nhân" else "Chào bạn đến với POP-MIND", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp)); Text("Bước ${step + 1}/4 · ${steps[step]}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp)); LinearProgressIndicator(progress = (step + 1) / 4f, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> {
                    Text("Mình nên gọi bạn là gì?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp)); OutlinedTextField(draft.name, { draft = draft.copy(name = it.take(40)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Tên gọi") }, singleLine = true, shape = RoundedCornerShape(18.dp))
                    Spacer(Modifier.height(20.dp)); Text("Bạn đang học lớp nào?", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (10..12).forEach { grade -> FilterChip(modifier = Modifier.height(38.dp), selected = draft.grade == grade, onClick = { draft = draft.copy(grade = grade) }, label = { Text("Lớp $grade") }) } }
                }
                1 -> {
                    Text("Bạn thường học môn nào hoặc đang hướng tới mục tiêu gì?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    val subjects = listOf("Toán", "Ngữ văn", "Tiếng Anh", "Vật lý", "Hóa học", "Sinh học", "Lịch sử", "Địa lý", "Tin học", "Luyện đề", "Đọc sách")
                    subjects.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { subject -> FilterChip(modifier = Modifier.height(38.dp), selected = subject in draft.subjects, onClick = { draft = draft.copy(subjects = draft.subjects.toggle(subject)) }, label = { Text(subject) }) } } }
                    OutlinedTextField(customSubject, { customSubject = it.filterNot { char -> char == '|' }.take(32) }, modifier = Modifier.fillMaxWidth(), label = { Text("Tự thêm môn / mục tiêu") }, trailingIcon = { TextButton(onClick = { val value = customSubject.trim(); if (value.isNotEmpty()) draft = draft.copy(subjects = (draft.subjects + value).distinct()); customSubject = "" }) { Text("Thêm") } }, singleLine = true)
                    draft.subjects.filter { it !in subjects }.forEach { value -> InputChip(selected = true, onClick = { draft = draft.copy(subjects = draft.subjects - value) }, label = { Text("$value  ×") }) }
                }
                2 -> {
                    Text("Lúc nào bạn thường dễ bị xao nhãng nhất?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp)); listOf("Sáng", "Trưa", "Tối", "Khuya").forEach { window ->
                        FilterChip(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), selected = draft.distractionWindow == window, onClick = { draft = draft.copy(distractionWindow = window) }, label = { Text(window) })
                    }
                    Text("Mình sẽ nhắc bạn nhẹ nhàng trước khung giờ này.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Text("Bạn hay dùng ứng dụng giải trí nào nhất?", style = MaterialTheme.typography.titleLarge)
                    Text("Chọn ứng dụng đã cài trên máy. Lựa chọn này chỉ lưu trên thiết bị.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    if (state.installedApps.isEmpty()) Text("Chưa tìm thấy ứng dụng có thể mở trên thiết bị.", modifier = Modifier.padding(vertical = 16.dp))
                    else state.installedApps.forEach { app ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = app.packageName in draft.favoriteApps, onClick = { draft = draft.copy(favoriteApps = listOf(app.packageName)) })
                            Text(app.label, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).height(54.dp), shape = CircleShape) { Text("Quay lại") }
            Button(onClick = {
                if (step < 3) step++
                else viewModel.save(draft) { onSaved(draft) }
            }, enabled = (step != 0 || draft.name.isNotBlank()) && (step != 1 || draft.subjects.isNotEmpty()), modifier = Modifier.weight(1f).height(54.dp), shape = CircleShape) {
                Text(if (step == 3) "Lưu hồ sơ" else "Tiếp tục")
            }
        }
    }
}

private fun List<String>.toggle(value: String): List<String> = if (value in this) this - value else this + value
