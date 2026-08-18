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

import com.example.data.ApiKeyManager
import com.example.data.FirebaseAuthManager
import com.example.data.FirestoreManager
import com.example.ui.screens.CHAT_ROLES
import com.example.ui.screens.ChatRoleItem

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = ""
)

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    DARK("Dark Theme"),
    LIGHT("Light Theme")
}

class MarcoViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("marco_theme_prefs", android.content.Context.MODE_PRIVATE)

    val isApiKeyConfigured: StateFlow<Boolean> = ApiKeyManager.isConfigured
    val apiKeySource: StateFlow<String> = ApiKeyManager.apiKeySource

    init {
        FirebaseAuthManager.instance.init(application)
        ApiKeyManager.init(application)
    }

    fun saveCustomApiKey(key: String) {
        ApiKeyManager.saveCustomApiKey(key)
    }

    fun clearCustomApiKey() {
        ApiKeyManager.clearCustomApiKey()
    }

    fun getCustomApiKey(): String {
        return ApiKeyManager.getCustomApiKey()
    }

    suspend fun testApiKeyConnection(key: String? = null): Pair<Boolean, String> {
        return aiEngine.testApiKey(key)
    }

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme_mode", ThemeMode.SYSTEM.name) } ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
        val userId = FirebaseAuthManager.instance.currentUser.value?.uid ?: "anonymous"
        viewModelScope.launch {
            FirestoreManager.instance.saveUserPreference(userId, "theme_mode", mode.name)
        }
    }

    // --- Gemini Multi-turn Chatbot State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "gemini",
                text = "Hello! I am your Gemini multi-turn AI chatbot. Select a role or model and ask me anything.",
                modelUsed = "gemini-3.5-flash"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _selectedChatRole = MutableStateFlow(CHAT_ROLES[0])
    val selectedChatRole: StateFlow<ChatRoleItem> = _selectedChatRole.asStateFlow()

    private val _selectedGeminiModel = MutableStateFlow("gemini-3.5-flash")
    val selectedGeminiModel: StateFlow<String> = _selectedGeminiModel.asStateFlow()

    private val _isHighThinkingEnabled = MutableStateFlow(false)
    val isHighThinkingEnabled: StateFlow<Boolean> = _isHighThinkingEnabled.asStateFlow()

    fun setSelectedChatRole(role: ChatRoleItem) {
        _selectedChatRole.value = role
        _selectedGeminiModel.value = role.defaultModel
    }

    fun setSelectedGeminiModel(model: String) {
        _selectedGeminiModel.value = model
    }

    fun setHighThinkingEnabled(enabled: Boolean) {
        _isHighThinkingEnabled.value = enabled
        if (enabled) {
            _selectedGeminiModel.value = "gemini-3.1-pro-preview"
        }
    }

    fun updateRoleSystemInstruction(newInstruction: String) {
        val current = _selectedChatRole.value
        _selectedChatRole.value = current.copy(systemInstruction = newInstruction)
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(sender = "user", text = userText)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatGenerating.value = true

        val userId = FirebaseAuthManager.instance.currentUser.value?.uid ?: "guest_user"
        val currentRole = _selectedChatRole.value
        val modelToUse = if (_isHighThinkingEnabled.value) "gemini-3.1-pro-preview" else _selectedGeminiModel.value

        viewModelScope.launch {
            // Save user message to Firestore
            FirestoreManager.instance.saveChatMessage(userId, "user", userText, modelToUse)

            // Prepare multi-turn history
            val history = _chatMessages.value.map { Pair(it.sender, it.text) }

            val responseText = aiEngine.sendMultiTurnChatMessage(
                history = history,
                systemInstructionText = currentRole.systemInstruction,
                model = modelToUse,
                enableHighThinking = _isHighThinkingEnabled.value
            )

            val displayModelLabel = if (_isHighThinkingEnabled.value) "$modelToUse [HIGH THINKING]" else modelToUse
            val geminiMsg = ChatMessage(sender = "gemini", text = responseText, modelUsed = displayModelLabel)
            _chatMessages.value = _chatMessages.value + geminiMsg
            _isChatGenerating.value = false

            // Save Gemini response to Firestore
            FirestoreManager.instance.saveChatMessage(userId, "gemini", responseText, displayModelLabel)
        }
    }

    private val db = MarcoDatabase.getDatabase(application)
    private val conversationDao = db.conversationDao()
    private val reminderDao = db.reminderDao()

    val speechToText = SpeechToTextManager(application)
    val textToSpeech = TextToSpeechManager(application)
    val toolRegistry = MarcoToolRegistry(application)
    val networkMonitor = com.example.tools.NetworkMonitor(application)
    val localKeywordSpotter = com.example.voice.LocalKeywordSpotter(application)
    val aiEngine = MarcoAiEngine()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val isKwsActive: StateFlow<Boolean> = localKeywordSpotter.isKwsActive

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

    private val _aiGeneratedContent = MutableStateFlow<String?>(null)
    val aiGeneratedContent: StateFlow<String?> = _aiGeneratedContent.asStateFlow()

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
                    speechToText.clearResult()
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
        // Connect local keyword spotter listener for 'Hey Marco'
        localKeywordSpotter.onKeywordDetectedListener = { keyword ->
            if (_assistantState.value != AssistantState.SPEAKING && _assistantState.value != AssistantState.LISTENING) {
                startListening()
            }
        }

        if (isContinuousWakeWordActive.value) {
            localKeywordSpotter.startSpotting()
        }
    }

    val voiceMatchManager = com.example.voice.SpeakerVoiceMatchManager.getInstance(application)
    val isVoiceMatchEnabled: StateFlow<Boolean> = voiceMatchManager.isVoiceMatchEnabled
    val isVoiceProfileEnrolled: StateFlow<Boolean> = voiceMatchManager.isProfileEnrolled
    val enrolledPitchHz: StateFlow<Float> = voiceMatchManager.enrolledPitchHz

    fun setContinuousWakeWord(enabled: Boolean) {
        speechToText.setContinuousWakeWord(enabled)
        if (enabled) {
            localKeywordSpotter.startSpotting()
            toggleBackgroundService(true)
        } else {
            localKeywordSpotter.stopSpotting()
            toggleBackgroundService(false)
        }
    }

    fun toggleBackgroundService(enabled: Boolean) {
        _isBackgroundActive.value = enabled
        if (enabled) {
            MarcoForegroundService.start(getApplication())
        } else {
            MarcoForegroundService.stop(getApplication())
        }
    }

    fun setVoiceMatchEnabled(enabled: Boolean) {
        voiceMatchManager.setVoiceMatchEnabled(enabled)
    }

    fun enrollVoiceSample(pcmBuffer: ShortArray): Boolean {
        return voiceMatchManager.enrollVoiceProfile(pcmBuffer)
    }

    fun resetVoiceProfile() {
        voiceMatchManager.resetVoiceProfile()
    }

    fun setKwsSensitivity(sensitivity: Float) {
        localKeywordSpotter.setSensitivity(sensitivity)
        WakeWordDetector.setSensitivity(sensitivity)
    }

    fun setVoiceGender(gender: com.example.voice.VoiceGender) {
        textToSpeech.setVoiceGender(gender)
    }

    fun setHighThinking(enabled: Boolean) {
        _isHighThinkingEnabled.value = enabled
    }

    fun generateImagePrompt(prompt: String) {
        if (prompt.isBlank()) return
        if (!isOnline.value) {
            val offlineMsg = "Offline Mode Active: Internet connection is required for AI image generation."
            _aiGeneratedContent.value = offlineMsg
            _assistantState.value = AssistantState.SPEAKING
            speakResponse("Device is offline. Connecting to network is required for image generation.", _preferredLanguage.value)
            return
        }
        viewModelScope.launch {
            _assistantState.value = AssistantState.PROCESSING
            val apiKey = try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            val res = aiEngine.generateImageWithGemini(prompt, apiKey)
            _aiGeneratedContent.value = res
            _assistantState.value = AssistantState.SPEAKING
            speakResponse(res ?: "Generated image artwork.", _preferredLanguage.value)
        }
    }

    fun generateMusicTrack(prompt: String) {
        if (prompt.isBlank()) return
        if (!isOnline.value) {
            val offlineMsg = "Offline Mode Active: Internet connection is required for Lyria music composition."
            _aiGeneratedContent.value = offlineMsg
            _assistantState.value = AssistantState.SPEAKING
            speakResponse("Device is offline. Connection required for music composition.", _preferredLanguage.value)
            return
        }
        viewModelScope.launch {
            _assistantState.value = AssistantState.PROCESSING
            val apiKey = try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            val res = aiEngine.generateMusicWithLyria(prompt, apiKey)
            _aiGeneratedContent.value = res
            _assistantState.value = AssistantState.SPEAKING
            speakResponse(res ?: "Composed 30-second audio track with Lyria.", _preferredLanguage.value)
        }
    }

    fun analyzeImagePhoto(prompt: String, base64Jpeg: String) {
        if (!isOnline.value) {
            val offlineMsg = "Offline Mode Active: Internet connection is required for Gemini Vision analysis."
            _aiGeneratedContent.value = offlineMsg
            _assistantState.value = AssistantState.SPEAKING
            speakResponse("Device is offline. Connection required for vision analysis.", _preferredLanguage.value)
            return
        }
        viewModelScope.launch {
            _assistantState.value = AssistantState.PROCESSING
            val apiKey = try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            val res = aiEngine.analyzeImageWithGemini(prompt, base64Jpeg, apiKey)
            _aiGeneratedContent.value = res
            _assistantState.value = AssistantState.SPEAKING
            speakResponse(res, _preferredLanguage.value)
        }
    }

    fun setPreferredLanguage(language: Language) {
        _preferredLanguage.value = language
    }

    fun clearConversationHistory() {
        viewModelScope.launch {
            conversationDao.clearAll()
        }
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
            val parsedIntent = aiEngine.processUserSpeech(userInput, _preferredLanguage.value, isOnline = isOnline.value)
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

            // Save to database and prune to maintain last 50 exchanges
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
            conversationDao.pruneOldConversations()

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
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            _isBackgroundActive.value = false
            return
        }

        val newState = !_isBackgroundActive.value
        _isBackgroundActive.value = newState
        if (newState) {
            try {
                MarcoForegroundService.start(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
                _isBackgroundActive.value = false
            }
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
        localKeywordSpotter.stopSpotting()
        textToSpeech.shutdown()
        networkMonitor.unregister()
    }

    private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
}
