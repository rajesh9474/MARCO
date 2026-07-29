package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechToTextManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isContinuousWakeWordActive = MutableStateFlow(true)
    val isContinuousWakeWordActive: StateFlow<Boolean> = _isContinuousWakeWordActive.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow<String?>(null)
    val finalText: StateFlow<String?> = _finalText.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastLanguagePreference: Language = Language.AUTO

    fun setContinuousWakeWord(enabled: Boolean) {
        _isContinuousWakeWordActive.value = enabled
        WakeWordDetector.setWakeWordListening(enabled)
    }

    fun startListening(languagePreference: Language = Language.AUTO) {
        lastLanguagePreference = languagePreference
        stopListening()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Speech recognition is not available on this device."
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _errorMessage.value = null
                    _partialText.value = ""
                    _finalText.value = null
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsDb.value = rmsdB.coerceAtLeast(0f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorText = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                        SpeechRecognizer.ERROR_SERVER -> "Speech server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
                        else -> "Speech recognition error ($error)"
                    }
                    _errorMessage.value = errorText
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = matches?.firstOrNull()
                    if (result != null && result.trim().isNotEmpty()) {
                        _finalText.value = result
                    } else {
                        _errorMessage.value = "Could not understand speech."
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = matches?.firstOrNull()
                    if (result != null && result.trim().isNotEmpty()) {
                        _partialText.value = result
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            val localeTag = when (languagePreference) {
                Language.TAMIL -> "ta-IN"
                Language.HINDI -> "hi-IN"
                Language.ENGLISH -> "en-IN"
                Language.AUTO -> Locale.getDefault().toLanguageTag()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            // Add extra supported languages for multi-language detection
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("ta-IN", "hi-IN", "en-IN", "en-US"))
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage ?: "Failed to start listening"
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore cleanup errors
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }

    fun clearResult() {
        _finalText.value = null
        _partialText.value = ""
        _errorMessage.value = null
    }

    private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
}
