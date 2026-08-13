package com.example.service

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ai.MarcoAiEngine
import com.example.tools.MarcoToolRegistry
import com.example.voice.SpeechToTextManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MarcoVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val sessionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val speechToText = SpeechToTextManager(context)
    private val textToSpeech = TextToSpeechManager(context)
    private val aiEngine = MarcoAiEngine()
    private val toolRegistry = MarcoToolRegistry(context)

    private var statusTextView: TextView? = null
    private var isProcessing = false

    override fun onCreateContentView(): View {
        val rootLayout = FrameLayout(context).apply {
            setBackgroundColor(0xEE000000.toInt()) // Translucent black assistant overlay
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        statusTextView = TextView(context).apply {
            text = "MARCO Listening..."
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        container.addView(statusTextView)
        rootLayout.addView(
            container,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        return rootLayout
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        isProcessing = false
        statusTextView?.text = "MARCO Listening..."

        sessionScope.launch {
            speechToText.finalText.collectLatest { speech ->
                if (!speech.isNullOrBlank() && !isProcessing) {
                    isProcessing = true
                    processUserSpeech(speech)
                }
            }
        }

        speechToText.startListening()
    }

    private fun processUserSpeech(userSpeech: String) {
        statusTextView?.text = "MARCO Thinking..."
        sessionScope.launch(Dispatchers.IO) {
            val parsedIntent = aiEngine.processUserSpeech(userSpeech)

            sessionScope.launch(Dispatchers.Main) {
                statusTextView?.text = "Executing: ${parsedIntent.intent}"
                val result = toolRegistry.executeTool(parsedIntent)

                val responseText = if (result.success) {
                    parsedIntent.spokenResponse
                } else {
                    result.message
                }

                statusTextView?.text = responseText
                textToSpeech.speak(responseText) {
                    // Hide session after speech finishes
                    hide()
                }
            }
        }
    }

    override fun onHide() {
        super.onHide()
        speechToText.stopListening()
        textToSpeech.stop()
        isProcessing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionScope.cancel()
    }
}
