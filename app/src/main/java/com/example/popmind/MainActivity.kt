package com.example.popmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.popmind.data.SessionRepository
import com.example.popmind.data.local.PopMindDatabase
import com.example.popmind.navigation.PopMindNavigation
import com.example.popmind.ui.progress.ProgressViewModel
import com.example.popmind.ui.progress.ProgressViewModelFactory
import com.example.popmind.ui.theme.PopMindTheme
import com.example.popmind.ui.profile.ProfileViewModel
import com.example.popmind.ui.roadmap.RoadmapViewModel
import com.example.popmind.session.FocusSessionRepository
import com.example.popmind.session.FocusSettingsViewModel
import com.example.popmind.ui.profile.PersonalizationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SessionRepository(PopMindDatabase.getInstance(applicationContext))
        val progressViewModel = ViewModelProvider(this, ProgressViewModelFactory(repository))[ProgressViewModel::class.java]
        val profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        val roadmapViewModel = ViewModelProvider(this)[RoadmapViewModel::class.java]
        val focusSettingsViewModel = ViewModelProvider(this)[FocusSettingsViewModel::class.java]
        val personalizationViewModel = ViewModelProvider(this)[PersonalizationViewModel::class.java]
        FocusSessionRepository.initialize(applicationContext)
        setContent {
            PopMindTheme {
                PopMindNavigation(progressViewModel, profileViewModel, roadmapViewModel, focusSettingsViewModel, personalizationViewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == com.example.popmind.service.FocusSessionService.ACTION_OPEN_SESSION) FocusSessionRepository.requestOpenSession()
    }
}
