package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.service.MarcoForegroundService
import com.example.ui.MarcoApp

class MainActivity : ComponentActivity() {

    private val startVoiceListeningState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startVoiceListeningState.value = intent?.getBooleanExtra(
            MarcoForegroundService.EXTRA_START_VOICE_LISTENING,
            false
        ) == true

        setContent {
            MarcoApp(
                autoStartVoiceListening = startVoiceListeningState.value,
                onVoiceListeningHandled = { startVoiceListeningState.value = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(MarcoForegroundService.EXTRA_START_VOICE_LISTENING, false)) {
            startVoiceListeningState.value = true
        }
    }
}

