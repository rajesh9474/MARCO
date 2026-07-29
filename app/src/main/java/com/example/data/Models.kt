package com.example.data

enum class Language(val code: String, val displayName: String, val localeTag: String) {
    TAMIL("ta", "தமிழ்", "ta-IN"),
    ENGLISH("en", "English", "en-US"),
    HINDI("hi", "हिन्दी", "hi-IN"),
    AUTO("auto", "Auto Detect", "auto")
}

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    EXECUTING,
    WAITING_CONFIRMATION
}

enum class ActionIntent {
    PLAY_MEDIA,
    OPEN_APP,
    SEND_MESSAGE,
    MAKE_CALL,
    CREATE_REMINDER,
    NAVIGATE_MAPS,
    SEARCH_WEB,
    DEVICE_SETTING,
    CAMERA,
    WEATHER_QUERY,
    CALCULATE,
    TRANSLATE,
    DEVICE_INFO,
    FLASHLIGHT,
    CHAT
}

data class ParsedIntent(
    val intent: ActionIntent,
    val detectedLanguage: Language,
    val spokenResponse: String,
    val application: String? = null,
    val searchQuery: String? = null,
    val contactName: String? = null,
    val messageText: String? = null,
    val timeStr: String? = null,
    val destination: String? = null,
    val settingName: String? = null,
    val settingValue: String? = null,
    val requiresConfirmation: Boolean = false,
    val toolName: String = "",
    val toolParams: Map<String, String> = emptyMap()
)

data class ToolResult(
    val success: Boolean,
    val message: String,
    val actionExecuted: String,
    val details: Map<String, String> = emptyMap()
)

data class ToolInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val requiresPermission: List<String> = emptyList(),
    val exampleUtterances: List<String> = emptyList()
)
