package com.example.popmind.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.popmind.ui.screens.FocusScreen
import com.example.popmind.ui.screens.ProfileScreen
import com.example.popmind.ui.screens.ProgressScreen
import com.example.popmind.ui.screens.RoadmapScreen

private data class Tab(val route: String, val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("focus", "Tập trung", Icons.Rounded.Bolt),
    Tab("progress", "Tiến độ", Icons.Rounded.Insights),
    Tab("roadmap", "Lộ trình", Icons.Rounded.AutoStories),
    Tab("profile", "Hồ sơ", Icons.Rounded.AccountCircle)
)

@Composable
fun PopMindNavigation() {
    val navController = rememberNavController()
    val backStack = navController.currentBackStackEntryAsState()
    Scaffold(
        bottomBar = {
            NavigationBar {
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
            composable("focus") { FocusScreen() }
            composable("progress") { ProgressScreen() }
            composable("roadmap") { RoadmapScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
}
