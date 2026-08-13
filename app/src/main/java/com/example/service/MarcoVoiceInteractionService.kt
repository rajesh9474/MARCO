package com.example.service

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession

class MarcoVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val args = intent.extras ?: Bundle()
            try {
                showSession(args, VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return START_STICKY
    }
}
