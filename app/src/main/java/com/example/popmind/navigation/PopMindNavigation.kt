package com.example.popmind.navigation

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
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

private data class Tab(val route: String, val title: String, val icon: ImageVector)
private data class SessionConfig(val task: String, val pomodoro: Boolean, val music: Boolean)

private val tabs = listOf(
    Tab("focus", "Tập trung", Icons.Rounded.Bolt),
    Tab("progress", "Tiến độ", Icons.Rounded.Insights),
    Tab("roadmap", "Lộ trình", Icons.Rounded.AutoStories),
    Tab("profile", "Hồ sơ", Icons.Rounded.AccountCircle)
)

@Composable
fun PopMindNavigation(progressViewModel: ProgressViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStack = navController.currentBackStackEntryAsState()
    var pendingSession by remember { mutableStateOf<SessionConfig?>(null) }
    var showDndDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    fun startSession(config: SessionConfig, tryDnd: Boolean) {
        val intent = Intent(context, FocusSessionService::class.java)
            .setAction(FocusSessionService.ACTION_START)
            .putExtra(FocusSessionService.EXTRA_TASK, config.task)
            .putExtra(FocusSessionService.EXTRA_POMODORO, config.pomodoro)
            .putExtra(FocusSessionService.EXTRA_MUSIC, config.music)
            .putExtra(FocusSessionService.EXTRA_TRY_DND, tryDnd)
        ContextCompat.startForegroundService(context, intent)
        navController.navigate("session") { launchSingleTop = true }
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

    fun requestToStart(task: String, pomodoro: Boolean, music: Boolean) {
        val config = SessionConfig(task, pomodoro, music)
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

    val currentRoute = backStack.value?.destination?.route
    val showTabs = tabs.any { it.route == currentRoute }
    Scaffold(
        bottomBar = {
            if (showTabs) NavigationBar {
                tabs.forEach { tab ->
                    val selected = backStack.value?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigate(tab.route) { launchSingleTop = true; restoreState = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "focus", modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable("focus") { FocusScreen(::requestToStart) }
            composable("progress") { ProgressScreen(progressViewModel) }
            composable("roadmap") { RoadmapScreen() }
            composable("profile") {
                ProfileScreen(
                    onLoadSample = progressViewModel::loadDemoWeek,
                    onDeleteAll = { progressViewModel.deleteAll() }
                )
            }
            composable("session") {
                SessionScreen(
                    onFinished = { duration, interruptions ->
                        navController.navigate("summary/$duration/$interruptions") {
                            popUpTo("session") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAbandoned = {
                        navController.popBackStack("focus", inclusive = false)
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
