package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MarcoAiEngine
import com.example.data.AssistantState
import com.example.data.Language
import com.example.data.ParsedIntent
import com.example.data.ToolResult
import com.example.data.db.ConversationEntity
import com.example.data.db.MarcoDatabase
import com.example.data.db.ReminderEntity
import com.example.service.MarcoForegroundService
import com.example.tools.MarcoToolRegistry
import com.example.voice.SpeechToTextManager
import com.example.voice.TextToSpeechManager
import com.example.voice.WakeWordDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarcoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MarcoDatabase.getDatabase(application)
    private val conversationDao = db.conversationDao()
    private val reminderDao = db.reminderDao()

    val speechToText = SpeechToTextManager(application)
    val textToSpeech = TextToSpeechManager(application)
    val toolRegistry = MarcoToolRegistry(application)
    val aiEngine = MarcoAiEngine()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _preferredLanguage = MutableStateFlow(Language.AUTO)
    val preferredLanguage: StateFlow<Language> = _preferredLanguage.asStateFlow()

    private val _currentPrompt = MutableStateFlow("")
    val currentPrompt: StateFlow<String> = _currentPrompt.asStateFlow()

    private val _lastParsedIntent = MutableStateFlow<ParsedIntent?>(null)
    val lastParsedIntent: StateFlow<ParsedIntent?> = _lastParsedIntent.asStateFlow()

    private val _lastToolResult = MutableStateFlow<ToolResult?>(null)
    val lastToolResult: StateFlow<ToolResult?> = _lastToolResult.asStateFlow()

    private val _pendingConfirmationIntent = MutableStateFlow<ParsedIntent?>(null)
    val pendingConfirmationIntent: StateFlow<ParsedIntent?> = _pendingConfirmationIntent.asStateFlow()

    private val _isBackgroundActive = MutableStateFlow(false)
    val isBackgroundActive: StateFlow<Boolean> = _isBackgroundActive.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = conversationDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = reminderDao.getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isContinuousWakeWordActive: StateFlow<Boolean> = speechToText.isContinuousWakeWordActive
    val voiceGender: StateFlow<com.example.voice.VoiceGender> = textToSpeech.voiceGender

    init {
        // Observe final speech recognition results with WakeWord processing
        viewModelScope.launch {
            speechToText.finalText.collectLatest { text ->
                if (!text.isNull_Blank()) {
                    val rawText = text!!
                    if (WakeWordDetector.containsWakeWord(rawText)) {
                        val command = WakeWordDetector.extractCommandAfterWakeWord(rawText)
                        val effectivePrompt = if (command.isNotBlank()) command else rawText
                        _currentPrompt.value = effectivePrompt
                        processSpeechInput(effectivePrompt)
                    } else {
                        _currentPrompt.value = rawText
                        processSpeechInput(rawText)
                    }
                }
            }
        }

        // Sync speech recognition state with assistant state
        viewModelScope.launch {
            speechToText.isListening.collectLatest { listening ->
                if (listening && _assistantState.value != AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.LISTENING
                } else if (!listening && _assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.PROCESSING
                }
            }
        }
    }

    fun setContinuousWakeWord(enabled: Boolean) {
        speechToText.setContinuousWakeWord(enabled)
    }

    fun setVoiceGender(gender: com.example.voice.VoiceGender) {
        textToSpeech.setVoiceGender(gender)
    }

    fun setPreferredLanguage(language: Language) {
        _preferredLanguage.value = language
    }

    fun startListening() {
        speechToText.clearResult()
        _assistantState.value = AssistantState.LISTENING
        speechToText.startListening(_preferredLanguage.value)
    }

    fun stopListening() {
        speechToText.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun processTextInput(text: String) {
        if (text.isBlank()) return
        _currentPrompt.value = text
        processSpeechInput(text)
    }

    private fun processSpeechInput(userInput: String) {
        viewModelScope.launch {
            _assistantState.value = AssistantState.PROCESSING
            val parsedIntent = aiEngine.processUserSpeech(userInput, _preferredLanguage.value)
            _lastParsedIntent.value = parsedIntent

            if (parsedIntent.requiresConfirmation) {
                _pendingConfirmationIntent.value = parsedIntent
                _assistantState.value = AssistantState.WAITING_CONFIRMATION
                speakResponse(parsedIntent.spokenResponse, parsedIntent.detectedLanguage)
            } else {
                executeParsedIntent(parsedIntent)
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmationIntent.value ?: return
        _pendingConfirmationIntent.value = null
        executeParsedIntent(pending)
    }

    fun cancelPendingAction() {
        _pendingConfirmationIntent.value = null
        _assistantState.value = AssistantState.IDLE
        val cancelMsg = when (_preferredLanguage.value) {
            Language.TAMIL -> "செயல் ரத்து செய்யப்பட்டது."
            Language.HINDI -> "कार्रवाई रद्द कर दी गई।"
            else -> "Action cancelled."
        }
        textToSpeech.speak(cancelMsg, _preferredLanguage.value)
    }

    private fun executeParsedIntent(parsedIntent: ParsedIntent) {
        viewModelScope.launch {
            _assistantState.value = AssistantState.EXECUTING
            val toolResult = toolRegistry.executeTool(parsedIntent)
            _lastToolResult.value = toolResult

            // Save to database
            conversationDao.insertConversation(
                ConversationEntity(
                    userPrompt = _currentPrompt.value,
                    marcoResponse = parsedIntent.spokenResponse,
                    language = parsedIntent.detectedLanguage.name,
                    intent = parsedIntent.intent.name,
                    executedTool = toolResult.actionExecuted,
                    toolSuccess = toolResult.success
                )
            )

            if (parsedIntent.intent == com.example.data.ActionIntent.CREATE_REMINDER && toolResult.success) {
                reminderDao.insertReminder(
                    ReminderEntity(
                        title = parsedIntent.messageText ?: "MARCO Reminder",
                        timeString = parsedIntent.timeStr ?: "7:00 AM"
                    )
                )
            }

            _assistantState.value = AssistantState.SPEAKING
            speakResponse(parsedIntent.spokenResponse, parsedIntent.detectedLanguage) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    private fun speakResponse(text: String, language: Language, onDone: (() -> Unit)? = null) {
        textToSpeech.speak(text, language) {
            _assistantState.value = AssistantState.IDLE
            onDone?.invoke()
        }
    }

    fun toggleBackgroundService() {
        val newState = !_isBackgroundActive.value
        _isBackgroundActive.value = newState
        if (newState) {
            MarcoForegroundService.start(getApplication())
        } else {
            MarcoForegroundService.stop(getApplication())
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationDao.clearAll()
        }
    }

    fun stopSpeaking() {
        textToSpeech.stop()
        _assistantState.value = AssistantState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        speechToText.stopListening()
        textToSpeech.shutdown()
    }

    private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
}
