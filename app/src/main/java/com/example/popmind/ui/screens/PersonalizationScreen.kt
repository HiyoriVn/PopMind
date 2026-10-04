package com.example.popmind.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.popmind.data.InstalledAppChoice
import com.example.popmind.data.PersonalProfile
import com.example.popmind.ui.profile.PersonalizationViewModel

private val suggestedAppPackages = setOf(
    "com.zhiliaoapp.musically",       // TikTok
    "com.ss.android.ugc.trill",       // TikTok Lite
    "com.google.android.youtube",      // YouTube Shorts
    "com.facebook.katana",             // Facebook Reels
    "com.instagram.android"            // Instagram Reels
)

private val personalProfileSaver = listSaver<PersonalProfile, Any>(
    save = { profile ->
        listOf(
            profile.name,
            profile.grade,
            profile.subjects.joinToString("|"),
            profile.distractionWindow,
            profile.linkedApps.sorted().joinToString("|"),
            profile.completed
        )
    },
    restore = { saved ->
        PersonalProfile(
            name = saved[0] as? String ?: "",
            grade = saved[1] as? Int ?: 10,
            subjects = (saved[2] as? String).orEmpty().split('|').filter(String::isNotBlank),
            distractionWindow = saved[3] as? String ?: "Tối",
            linkedApps = (saved[4] as? String).orEmpty().split('|').filter(String::isNotBlank).toSet(),
            completed = saved[5] as? Boolean ?: false
        )
    }
)

@Composable
fun PersonalizationScreen(
    viewModel: PersonalizationViewModel,
    editing: Boolean,
    onSaved: (PersonalProfile) -> Unit,
    onCancel: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by rememberSaveable(stateSaver = personalProfileSaver) { mutableStateOf(PersonalProfile()) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var customSubject by rememberSaveable { mutableStateOf("") }
    var selectedApps by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var search by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.loaded) {
        if (state.loaded && !initialized) {
            draft = state.profile
            selectedApps = state.profile.linkedApps.toList()
            initialized = true
        }
    }
    LaunchedEffect(step) {
        if (step == 3) viewModel.refreshInstalledApps()
    }
    LaunchedEffect(state.appsLoading, state.appsLoadFailed, state.installedApps) {
        if (initialized && !state.appsLoading && !state.appsLoadFailed) {
            val installed = state.installedApps.mapTo(mutableSetOf()) { it.packageName }
            selectedApps = selectedApps.filter { it in installed }
        }
    }
    if (!state.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val steps = listOf("Một chút về bạn", "Môn học của bạn", "Khung giờ dễ xao nhãng", "Ứng dụng liên kết")
    val suggestions = state.installedApps.filter { it.packageName in suggestedAppPackages }
    val selectedSet = selectedApps.toSet()

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp)) {
        if (editing) TextButton(onClick = onCancel) { Text("← Quay lại") }
        Text(if (editing) "Thông tin cá nhân" else "Chào bạn đến với POP-MIND", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("Bước ${step + 1}/4 · ${steps[step]}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = (step + 1) / 4f, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        if (step == 3) {
            LinkedAppsPicker(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                apps = state.installedApps,
                suggestions = suggestions,
                selectedPackages = selectedSet,
                search = search,
                onSearchChanged = { search = it },
                loading = state.appsLoading,
                loadFailed = state.appsLoadFailed,
                onToggle = { packageName ->
                    selectedApps = if (packageName in selectedSet) selectedApps - packageName else selectedApps + packageName
                },
                onSelectSuggestions = {
                    selectedApps = (selectedApps + suggestions.map { it.packageName }).distinct()
                }
            )
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                when (step) {
                    0 -> {
                        Text("Mình nên gọi bạn là gì?", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(draft.name, { draft = draft.copy(name = it.take(40)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Tên gọi") }, singleLine = true, shape = RoundedCornerShape(18.dp))
                        Spacer(Modifier.height(20.dp))
                        Text("Bạn đang học lớp nào?", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (10..12).forEach { grade -> FilterChip(modifier = Modifier.height(38.dp), selected = draft.grade == grade, onClick = { draft = draft.copy(grade = grade) }, label = { Text("Lớp $grade") }) }
                        }
                    }
                    1 -> {
                        Text("Bạn thường học môn nào hoặc đang hướng tới mục tiêu gì?", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        val subjects = listOf("Toán", "Ngữ văn", "Tiếng Anh", "Vật lý", "Hóa học", "Sinh học", "Lịch sử", "Địa lý", "Tin học", "Luyện đề", "Đọc sách")
                        subjects.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { subject -> FilterChip(modifier = Modifier.height(38.dp), selected = subject in draft.subjects, onClick = { draft = draft.copy(subjects = draft.subjects.toggle(subject)) }, label = { Text(subject) }) }
                            }
                        }
                        OutlinedTextField(customSubject, { customSubject = it.filterNot { char -> char == '|' }.take(32) }, modifier = Modifier.fillMaxWidth(), label = { Text("Tự thêm môn / mục tiêu") }, trailingIcon = { TextButton(onClick = { val value = customSubject.trim(); if (value.isNotEmpty()) draft = draft.copy(subjects = (draft.subjects + value).distinct()); customSubject = "" }) { Text("Thêm") } }, singleLine = true)
                        draft.subjects.filter { it !in subjects }.forEach { value -> InputChip(selected = true, onClick = { draft = draft.copy(subjects = draft.subjects - value) }, label = { Text("$value  ×") }) }
                    }
                    2 -> {
                        Text("Lúc nào bạn thường dễ bị xao nhãng nhất?", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        listOf("Sáng", "Trưa", "Tối", "Khuya").forEach { window ->
                            FilterChip(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), selected = draft.distractionWindow == window, onClick = { draft = draft.copy(distractionWindow = window) }, label = { Text(window) })
                        }
                        Text("Mình sẽ nhắc bạn nhẹ nhàng trước khung giờ này.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (step == 3) {
            Text("Đã chọn ${selectedApps.size} ứng dụng", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).height(54.dp), shape = CircleShape) { Text("Quay lại") }
            Button(
                onClick = {
                    if (step < 3) step++
                    else {
                        val savedProfile = draft.copy(linkedApps = selectedApps.toSet())
                        viewModel.save(savedProfile) { onSaved(savedProfile) }
                    }
                },
                enabled = if (step == 3) !state.appsLoading else (step != 0 || draft.name.isNotBlank()) && (step != 1 || draft.subjects.isNotEmpty()),
                modifier = Modifier.weight(1f).height(54.dp), shape = CircleShape
            ) { Text(if (step == 3) "Xong" else "Tiếp tục") }
        }
    }
}

@Composable
private fun LinkedAppsPicker(
    modifier: Modifier,
    apps: List<InstalledAppChoice>,
    suggestions: List<InstalledAppChoice>,
    selectedPackages: Set<String>,
    search: String,
    onSearchChanged: (String) -> Unit,
    loading: Boolean,
    loadFailed: Boolean,
    onToggle: (String) -> Unit,
    onSelectSuggestions: () -> Unit
) {
    Column(modifier) {
        Text("Chọn các ứng dụng bạn muốn liên kết", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("Các ứng dụng đã chọn được dùng để tổng hợp thời gian sử dụng.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm ứng dụng") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(8.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            loadFailed -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Chưa tải được danh sách ứng dụng. Bạn thử mở lại mục này nhé.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            apps.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Chưa tìm thấy ứng dụng nào có thể mở trên thiết bị.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else -> {
                val filteredSuggestions = suggestions.filter { it.label.contains(search, ignoreCase = true) }
                val filteredOtherApps = apps.filter { it.packageName !in suggestedAppPackages && it.label.contains(search, ignoreCase = true) }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                    if (filteredSuggestions.isNotEmpty()) {
                        item(key = "suggestions_heading") { Text("Gợi ý", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }
                        item(key = "select_all_suggestions") {
                            TextButton(onClick = onSelectSuggestions, modifier = Modifier.fillMaxWidth()) { Text("Chọn tất cả gợi ý") }
                        }
                        items(filteredSuggestions, key = { it.packageName }) { app ->
                            LinkedAppRow(app, selectedPackages.contains(app.packageName)) { onToggle(app.packageName) }
                        }
                    }
                    if (filteredOtherApps.isNotEmpty()) {
                        item(key = "all_apps_heading") { Text(if (filteredSuggestions.isEmpty()) "Ứng dụng" else "Tất cả ứng dụng", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) }
                        items(filteredOtherApps, key = { it.packageName }) { app ->
                            LinkedAppRow(app, selectedPackages.contains(app.packageName)) { onToggle(app.packageName) }
                        }
                    }
                    if (filteredSuggestions.isEmpty() && filteredOtherApps.isEmpty()) {
                        item(key = "no_search_results") { Text("Không tìm thấy ứng dụng khớp với “$search”.", modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedAppRow(app: InstalledAppChoice, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onToggle).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val packageManager = LocalContext.current.packageManager
        val icon = remember(app.packageName) {
            runCatching { packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit)
        else Text("📱", modifier = Modifier.width(40.dp))
        Text(app.label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.bodyLarge)
        Checkbox(checked = selected, onCheckedChange = null)
    }
}

private fun List<String>.toggle(value: String): List<String> = if (value in this) this - value else this + value
