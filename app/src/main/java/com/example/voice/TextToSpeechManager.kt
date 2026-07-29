package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceGender(val displayName: String) {
    DEFAULT("Default Engine Voice"),
    MALE("Male Voice Model (JARVIS Deep-Tone)"),
    FEMALE("Female Voice Model (Smooth Tone)")
}

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _voiceGender = MutableStateFlow(VoiceGender.MALE)
    val voiceGender: StateFlow<VoiceGender> = _voiceGender.asStateFlow()

    var speechRate: Float = 1.0f
    var pitch: Float = 0.85f

    init {
        tts = TextToSpeech(context, this)
    }

    fun setVoiceGender(gender: VoiceGender) {
        _voiceGender.value = gender
        when (gender) {
            VoiceGender.MALE -> {
                pitch = 0.82f
                speechRate = 1.0f
            }
            VoiceGender.FEMALE -> {
                pitch = 1.18f
                speechRate = 1.0f
            }
            VoiceGender.DEFAULT -> {
                pitch = 1.0f
                speechRate = 1.0f
            }
        }
        applyVoiceProfile()
    }

    private fun applyVoiceProfile() {
        if (!isInitialized || tts == null) return
        val currentGender = _voiceGender.value
        try {
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matchedVoice = availableVoices.firstOrNull { voice ->
                    val voiceName = voice.name.lowercase()
                    when (currentGender) {
                        VoiceGender.MALE -> voiceName.contains("male") || voiceName.contains("masculine") || voiceName.contains("m-") || voiceName.contains("en-us-x-sfg")
                        VoiceGender.FEMALE -> voiceName.contains("female") || voiceName.contains("feminine") || voiceName.contains("f-") || voiceName.contains("en-us-x-tpf")
                        VoiceGender.DEFAULT -> false
                    }
                }
                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (e: Exception) {
            // Fallback to pitch modulation
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyVoiceProfile()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun speak(text: String, language: Language = Language.AUTO, onDone: (() -> Unit)? = null) {
        if (!isInitialized || text.isBlank()) {
            onDone?.invoke()
            return
        }

        stop()

        val locale = when (language) {
            Language.TAMIL -> Locale("ta", "IN")
            Language.HINDI -> Locale("hi", "IN")
            Language.ENGLISH -> Locale("en", "US")
            Language.AUTO -> detectLocaleFromText(text)
        }

        tts?.let { engine ->
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.ENGLISH
            }
            engine.setSpeechRate(speechRate)
            engine.setPitch(pitch)

            val utteranceId = "MARCO_SPEECH_${System.currentTimeMillis()}"
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun detectLocaleFromText(text: String): Locale {
        val tamilCount = text.count { it in '\u0B80'..'\u0BFF' }
        val hindiCount = text.count { it in '\u0900'..'\u097F' }

        return when {
            tamilCount > 0 -> Locale("ta", "IN")
            hindiCount > 0 -> Locale("hi", "IN")
            else -> Locale("en", "US")
        }
    }
}
