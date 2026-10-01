package com.example.popmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.popmind.navigation.PopMindNavigation
import com.example.popmind.ui.theme.PopMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PopMindTheme {
                PopMindNavigation()
            }
        }
    }
}
