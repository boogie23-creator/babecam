package com.babecam.carenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.babecam.carenote.ui.CareTheme
import com.babecam.carenote.ui.MainScreen
import com.babecam.carenote.ui.MainViewModel
import com.babecam.carenote.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CareTheme {
                val vm: MainViewModel = viewModel()
                var screen by rememberSaveable { mutableStateOf("main") }
                if (screen == "settings") {
                    SettingsScreen(vm, onBack = { screen = "main" })
                } else {
                    MainScreen(vm, onSettings = { screen = "settings" })
                }
            }
        }
    }
}
