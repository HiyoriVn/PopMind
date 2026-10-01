package com.example.popmind.navigation

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.popmind.service.FocusSessionService
import com.example.popmind.ui.screens.FocusScreen
import com.example.popmind.ui.screens.ProfileScreen
import com.example.popmind.ui.screens.ProgressScreen
import com.example.popmind.ui.screens.RoadmapScreen
import com.example.popmind.ui.screens.SessionScreen
import com.example.popmind.ui.screens.SessionSummaryScreen
import com.example.popmind.ui.progress.ProgressViewModel
import com.example.popmind.ui.profile.ProfileViewModel
import com.example.popmind.ui.screens.AssessmentScreen
import com.example.popmind.ui.roadmap.RoadmapViewModel
import com.example.popmind.ui.screens.PersonalizedRoadmapScreen
import com.example.popmind.ui.screens.PlusScreen
import com.example.popmind.ui.screens.WelcomeScreen
import com.example.popmind.ui.screens.BreathingScreen
import com.example.popmind.session.FocusSessionRepository
import com.example.popmind.session.FocusSettingsViewModel
import com.example.popmind.ui.theme.Mint
import kotlinx.coroutines.flow.collect

private data class Tab(val route: String, val title: String, val icon: ImageVector)
private data class SessionConfig(val task: String, val pomodoro: Boolean, val focusMinutes: Int, val breakMinutes: Int, val sound: String, val volume: Float)

private val tabs = listOf(
    Tab("focus", "Tập trung", Icons.Rounded.Bolt),
    Tab("progress", "Tiến độ", Icons.Rounded.Insights),
    Tab("roadmap", "Lộ trình", Icons.Rounded.AutoStories),
    Tab("profile", "Hồ sơ", Icons.Rounded.AccountCircle)
)

@Composable
fun PopMindNavigation(progressViewModel: ProgressViewModel, profileViewModel: ProfileViewModel, roadmapViewModel: RoadmapViewModel, focusSettingsViewModel: FocusSettingsViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStack = navController.currentBackStackEntryAsState()
    val sessionState by FocusSessionRepository.state.collectAsState()
    val sessionReady by FocusSessionRepository.ready.collectAsState()
    val focusSettings by focusSettingsViewModel.settings.collectAsState()
    var pendingSession by remember { mutableStateOf<SessionConfig?>(null) }
    var showDndDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingTryDnd by remember { mutableStateOf(false) }

    fun launchSession(config: SessionConfig, tryDnd: Boolean, pin: Boolean) {
        val intent = Intent(context, FocusSessionService::class.java)
            .setAction(FocusSessionService.ACTION_START)
            .putExtra(FocusSessionService.EXTRA_TASK, config.task)
            .putExtra(FocusSessionService.EXTRA_POMODORO, config.pomodoro)
            .putExtra(FocusSessionService.EXTRA_FOCUS_MINUTES, config.focusMinutes)
            .putExtra(FocusSessionService.EXTRA_BREAK_MINUTES, config.breakMinutes)
            .putExtra(FocusSessionService.EXTRA_SOUND, config.sound)
            .putExtra(FocusSessionService.EXTRA_VOLUME, config.volume)
            .putExtra(FocusSessionService.EXTRA_PIN, pin)
            .putExtra(FocusSessionService.EXTRA_TRY_DND, tryDnd)
        ContextCompat.startForegroundService(context, intent)
        navController.navigate("session") { launchSingleTop = true }
    }

    fun startSession(config: SessionConfig, tryDnd: Boolean) {
        pendingSession = config
        pendingTryDnd = tryDnd
        showPinDialog = true
    }

    LaunchedEffect(sessionReady) {
        if (sessionReady && backStack.value?.destination?.route == "bootstrap") {
            val prefs = context.getSharedPreferences("pop_mind", android.content.Context.MODE_PRIVATE)
            val destination = if (sessionState.isActive) "session" else if (prefs.getBoolean("welcome_done", false)) "focus" else "welcome"
            if (sessionState.isActive) ContextCompat.startForegroundService(context, Intent(context, FocusSessionService::class.java).setAction(FocusSessionService.ACTION_RESTORE))
            navController.navigate(destination) { popUpTo("bootstrap") { inclusive = true } }
        }
    }

    LaunchedEffect(navController) {
        FocusSessionRepository.openRequests.collect {
            navController.navigate("session") { launchSingleTop = true }
        }
    }

    val dndSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val config = pendingSession
        pendingSession = null
        if (config != null) {
            val manager = context.getSystemService(NotificationManager::class.java)
            startSession(config, manager.isNotificationPolicyAccessGranted)
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Từ chối thông báo không ngăn phiên tập trung chạy.
        showDndDialog = true
    }
    val usageAccessSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        profileViewModel.refreshUsage()
        roadmapViewModel.refreshUsage()
    }

    fun requestToStart(task: String, pomodoro: Boolean, focusMinutes: Int, breakMinutes: Int, sound: String, volume: Float) {
        val config = SessionConfig(task, pomodoro, focusMinutes, breakMinutes, sound, volume)
        pendingSession = config
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showNotificationDialog = true
        } else showDndDialog = true
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = {
                showNotificationDialog = false
                showDndDialog = true
            },
            title = { Text("Cho phép thông báo phiên nhé?") },
            text = { Text("Thông báo giúp bạn thấy đồng hồ phiên đang chạy khi chuyển sang ứng dụng khác hoặc tắt màn hình. Bạn vẫn có thể tập trung nếu không cấp quyền này.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationDialog = false
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Tiếp tục") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationDialog = false
                    showDndDialog = true
                }) { Text("Không cấp quyền") }
            }
        )
    }

    if (showDndDialog) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val hasDndAccess = Build.VERSION.SDK_INT < 23 || manager.isNotificationPolicyAccessGranted
        AlertDialog(
            onDismissRequest = { showDndDialog = false },
            title = { Text(if (hasDndAccess) "Bật chế độ yên lặng?" else "Cho phép bật Không làm phiền?") },
            text = { Text(if (hasDndAccess) "Khi phiên bắt đầu, Pop-Mind sẽ tạm bật Không làm phiền để bạn dễ tập trung. Trạng thái cũ sẽ được khôi phục khi kết thúc." else "Pop-Mind cần quyền truy cập chính sách thông báo để bật Không làm phiền trong phiên và khôi phục cài đặt cũ sau đó. Bạn vẫn có thể bắt đầu mà không cấp quyền này.") },
            confirmButton = {
                TextButton(onClick = {
                    showDndDialog = false
                    val config = pendingSession ?: return@TextButton
                    if (!hasDndAccess && Build.VERSION.SDK_INT >= 23) {
                        dndSettings.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    } else {
                        pendingSession = null
                        startSession(config, true)
                    }
                }) { Text(if (hasDndAccess) "Bắt đầu phiên" else "Mở cài đặt") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDndDialog = false
                    val config = pendingSession
                    pendingSession = null
                    if (config != null) startSession(config, false)
                }) { Text(if (hasDndAccess) "Để sau" else "Tiếp tục không bật") }
            }
        )
    }

    if (showPinDialog) AlertDialog(
        onDismissRequest = { showPinDialog = false; pendingSession = null },
        title = { Text("Ghim màn hình trong phiên?") },
        text = { Text("Ghim giúp bạn ở lại POP-MIND. Android sẽ hỏi xác nhận khi bật lần đầu; bạn có thể bỏ ghim bằng thao tác hệ thống bất cứ lúc nào.") },
        confirmButton = { TextButton(onClick = {
            showPinDialog = false
            pendingSession?.let { launchSession(it, pendingTryDnd, true) }
            pendingSession = null
        }) { Text("Ghim và bắt đầu") } },
        dismissButton = { TextButton(onClick = {
            showPinDialog = false
            pendingSession?.let { launchSession(it, pendingTryDnd, false) }
            pendingSession = null
        }) { Text("Không ghim") } }
    )

    val currentRoute = backStack.value?.destination?.route
    val showTabs = tabs.any { it.route == currentRoute }
    Scaffold(
        bottomBar = {
            if (showTabs) NavigationBar(
                modifier = Modifier.heightIn(min = 80.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { tab ->
                    val selected = backStack.value?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigate(tab.route) { launchSingleTop = true; restoreState = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelMedium) },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Mint,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "bootstrap", modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable("bootstrap") { androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() } }
            composable("welcome") { WelcomeScreen {
                context.getSharedPreferences("pop_mind", android.content.Context.MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply()
                navController.navigate("focus") { popUpTo("welcome") { inclusive = true } }
            } }
            composable("focus") {
                FocusScreen(
                    progressState = progressViewModel.state.collectAsState().value,
                    settings = focusSettings,
                    onFocusMinutes = focusSettingsViewModel::saveFocus,
                    onBreakMinutes = focusSettingsViewModel::saveBreak,
                    onSound = focusSettingsViewModel::saveSound,
                    onVolume = focusSettingsViewModel::saveVolume,
                    onStartSession = ::requestToStart,
                    onOpenBreathing = { navController.navigate("breathing") }
                )
            }
            composable("progress") { ProgressScreen(progressViewModel, profileViewModel, onRequestUsageAccess = { usageAccessSettings.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) }
            composable("roadmap") {
                PersonalizedRoadmapScreen(
                    roadmapViewModel,
                    onOpenUsageSettings = { usageAccessSettings.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    onOpenPlus = { navController.navigate("plus") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    progressState = progressViewModel.state.collectAsState().value,
                    onLoadSample = progressViewModel::loadDemoWeek,
                    onDeleteAll = { progressViewModel.deleteAll() },
                    onOpenAssessment = { navController.navigate("assessment") },
                    onRequestUsageAccess = { usageAccessSettings.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    onOpenPlus = { navController.navigate("plus") }
                )
            }
            composable("assessment") {
                AssessmentScreen(profileViewModel, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
            }
            composable("plus") { PlusScreen(onBack = { navController.popBackStack() }) }
            composable("breathing") { BreathingScreen(onExit = { navController.popBackStack() }) }
            composable("session") {
                SessionScreen(
                    onVolumeChanged = focusSettingsViewModel::saveVolume,
                    onFinished = { duration, interruptions, completed ->
                        if (completed) navController.navigate("summary/$duration/$interruptions") {
                            popUpTo("session") { inclusive = true }; launchSingleTop = true
                        } else navController.navigate("focus") { popUpTo("session") { inclusive = true } }
                    }
                )
            }
            composable("summary/{duration}/{interruptions}") { entry ->
                val duration = entry.arguments?.getString("duration")?.toLongOrNull() ?: 0L
                val interruptions = entry.arguments?.getString("interruptions")?.toIntOrNull() ?: 0
                SessionSummaryScreen(duration, interruptions) {
                    navController.navigate("focus") { popUpTo("summary/{duration}/{interruptions}") { inclusive = true } }
                }
            }
        }
    }
}
