package com.example.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WakeWordDetector {

    private val _isWakeWordListening = MutableStateFlow(true)
    val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

    private val wakeWords = listOf(
        "hey marco", "marco", "hello marco", "hi marco", "ok marco",
        "jarvis", "hey jarvis", "hello jarvis", "ok jarvis",
        "மார்கோ", "ஹே மார்கோ", "ஹலோ மார்கோ",
        "मार्को", "हे मार्को", "हेलो मार्को", "जार्विस"
    )

    fun setWakeWordListening(enabled: Boolean) {
        _isWakeWordListening.value = enabled
    }

    fun containsWakeWord(text: String): Boolean {
        val lower = text.lowercase().trim()
        return wakeWords.any { wakeWord -> lower.contains(wakeWord) }
    }

    fun extractCommandAfterWakeWord(text: String): String {
        val lower = text.lowercase().trim()
        for (wakeWord in wakeWords) {
            val index = lower.indexOf(wakeWord)
            if (index != -1) {
                val command = text.substring(index + wakeWord.length).trim()
                val cleaned = command.removePrefix(",").removePrefix(".").removePrefix(":").trim()
                if (cleaned.isNotEmpty()) {
                    return cleaned
                }
            }
        }
        return text.trim()
    }

    fun getWakeWordList(): List<String> = wakeWords
}
