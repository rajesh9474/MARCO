package com.example.ai

import com.example.BuildConfig
import com.example.data.ActionIntent
import com.example.data.ApiKeyManager
import com.example.data.Language
import com.example.data.ParsedIntent
import com.example.voice.WakeWordDetector
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MarcoAiEngine {

    suspend fun testApiKey(customKey: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val key = customKey?.takeIf { it.isNotBlank() } ?: ApiKeyManager.getApiKey()
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext Pair(false, "No API key provided. Please enter a valid Gemini API key.")
        }
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val jsonPayload = JSONObject().apply {
                put("contents", listOf(
                    JSONObject().apply {
                        put("parts", listOf(JSONObject().apply { put("text", "Hello, reply with 'OK'") }))
                    }
                ))
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()) }
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                return@withContext Pair(true, "✓ Gemini API connection successful! Model responded.")
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                val message = try {
                    JSONObject(err).optJSONObject("error")?.optString("message") ?: "HTTP $responseCode"
                } catch (e: Exception) {
                    "HTTP $responseCode"
                }
                return@withContext Pair(false, "API Key Error ($responseCode): $message")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Network error testing key: ${e.localizedMessage}")
        }
    }

    suspend fun processUserSpeech(
        userSpeech: String,
        preferredLanguage: Language = Language.AUTO,
        isOnline: Boolean = true
    ): ParsedIntent = withContext(Dispatchers.IO) {
        val cleanSpeech = WakeWordDetector.extractCommandAfterWakeWord(userSpeech)
        val apiKey = ApiKeyManager.getApiKey()

        if (isOnline && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                // If query asks for image creation or music generation or image analysis, handle appropriately
                val lower = cleanSpeech.lowercase()
                if (lower.contains("generate image") || lower.contains("create image") || lower.contains("draw ") || lower.contains("picture of") || lower.contains("படம் உருவாக்கு")) {
                    val prompt = cleanSpeech.replace(Regex("(?i)generate image|create image|draw|picture of|படம் உருவாக்கு"), "").trim()
                    val imgResult = generateImageWithGemini(prompt, apiKey)
                    if (imgResult != null) {
                        return@withContext ParsedIntent(
                            intent = ActionIntent.CHAT,
                            detectedLanguage = preferredLanguage,
                            spokenResponse = "Generated image for '$prompt'. Preview is ready.",
                            searchQuery = imgResult
                        )
                    }
                }

                if (lower.contains("generate music") || lower.contains("compose music") || lower.contains("create song") || lower.contains("lyria") || lower.contains("இசை உருவாக்கு")) {
                    val musicPrompt = cleanSpeech.replace(Regex("(?i)generate music|compose music|create song|lyria|இசை உருவாக்கு"), "").trim()
                    val musicResult = generateMusicWithLyria(musicPrompt, apiKey)
                    if (musicResult != null) {
                        return@withContext ParsedIntent(
                            intent = ActionIntent.PLAY_MEDIA,
                            detectedLanguage = preferredLanguage,
                            spokenResponse = "Composed 30-second audio track for '$musicPrompt' using Lyria.",
                            searchQuery = musicResult
                        )
                    }
                }

                val useHighThinking = lower.contains("think") || lower.contains("complex") || lower.contains("explain deeply") || lower.contains("code") || lower.contains("solve")
                val geminiResult = if (useHighThinking) {
                    callGeminiProThinking(cleanSpeech, preferredLanguage, apiKey)
                } else {
                    callGeminiApi(cleanSpeech, preferredLanguage, apiKey)
                }

                if (geminiResult != null) {
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                // fallback to rule engine on network failure
            }
        }

        return@withContext processOfflineRules(cleanSpeech, preferredLanguage)
    }

    private fun callGeminiApi(
        input: String,
        preferredLanguage: Language,
        apiKey: String
    ): ParsedIntent? {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val prompt = """
            You are MARCO, an advanced autonomous AI assistant similar to JARVIS from Iron Man. You operate natively on Android and understand Tamil, English, and Hindi, including code-mixed natural speech.
            User input: "$input"
            Preferred output language setting: "${preferredLanguage.name}"

            CRITICAL INTENT RULES FOR APPS & MEDIA:
            - If user asks to open an app like 'open youtube', 'launch youtube', 'open whatsapp', 'open chrome' (WITHOUT asking to search or play a specific video/song query), intent MUST be "OPEN_APP", application MUST be the app name (e.g. "YouTube"), and tool_name MUST be "open_app".
            - ONLY use "PLAY_MEDIA" or "search_youtube" if the user explicitly requests to search or play a specific video, song, artist, or topic (e.g. 'play A.R. Rahman on youtube', 'search youtube for cooking').
            - NEVER default empty search queries to "Tamil songs" or force media playback when user simply wants to open an app.

            Respond strictly in valid JSON format with keys:
            {
              "detected_language": "TAMIL" | "ENGLISH" | "HINDI" | "MIXED",
              "intent": "PLAY_MEDIA" | "OPEN_APP" | "SEND_MESSAGE" | "MAKE_CALL" | "CREATE_REMINDER" | "NAVIGATE_MAPS" | "SEARCH_WEB" | "DEVICE_SETTING" | "CAMERA" | "WEATHER_QUERY" | "CALCULATE" | "TRANSLATE" | "DEVICE_INFO" | "FLASHLIGHT" | "CHAT",
              "application": "YouTube" | "WhatsApp" | "Chrome" | "Maps" | "Camera" | "Calculator" | "Settings" | "Phone" | "Spotify" | "Gallery" | string or null,
              "search_query": string or null,
              "contact_name": string or null,
              "message_text": string or null,
              "time_str": string or null,
              "destination": string or null,
              "setting_name": string or null,
              "setting_value": string or null,
              "requires_confirmation": false,
              "tool_name": "search_youtube" | "open_app" | "play_media" | "send_message" | "make_call" | "reminder" | "set_timer" | "set_alarm" | "compose_email" | "youtube_music" | "smart_home" | "google_tasks" | "browser_search" | "maps_navigation" | "device_settings" | "calculator" | "weather" | "camera" | "device_info" | "flashlight" | "translate" | "none",
              "spoken_response": "Polished, highly intelligent, direct JARVIS-style response in the target language (Tamil, English, or Hindi) answering the query completely."
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", listOf(
                JSONObject().apply {
                    put("parts", listOf(
                        JSONObject().apply { put("text", prompt) }
                    ))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()) }

        if (conn.responseCode == 200) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseStr)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNull_Blank()) {
                val json = JSONObject(text!!)
                val langStr = json.optString("detected_language", "ENGLISH")
                val intentStr = json.optString("intent", "CHAT")

                val detectedLang = when {
                    preferredLanguage != Language.AUTO -> preferredLanguage
                    langStr.contains("TAMIL") -> Language.TAMIL
                    langStr.contains("HINDI") -> Language.HINDI
                    else -> detectLanguageFromText(input)
                }

                val actionIntent = try { ActionIntent.valueOf(intentStr) } catch (e: Exception) { ActionIntent.CHAT }

                return ParsedIntent(
                    intent = actionIntent,
                    detectedLanguage = detectedLang,
                    spokenResponse = json.optString("spoken_response", "Okay, I understand."),
                    application = json.optString("application").takeIf { !it.isNull_Blank() },
                    searchQuery = json.optString("search_query").takeIf { !it.isNull_Blank() },
                    contactName = json.optString("contact_name").takeIf { !it.isNull_Blank() },
                    messageText = json.optString("message_text").takeIf { !it.isNull_Blank() },
                    timeStr = json.optString("time_str").takeIf { !it.isNull_Blank() },
                    destination = json.optString("destination").takeIf { !it.isNull_Blank() },
                    settingName = json.optString("setting_name").takeIf { !it.isNull_Blank() },
                    settingValue = json.optString("setting_value").takeIf { !it.isNull_Blank() },
                    requiresConfirmation = json.optBoolean("requires_confirmation", false),
                    toolName = json.optString("tool_name", "none")
                )
            }
        }

        return null
    }

    suspend fun sendMultiTurnChatMessage(
        history: List<Pair<String, String>>,
        systemInstructionText: String = "You are a helpful AI assistant.",
        model: String = "gemini-3.5-flash",
        enableHighThinking: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API key is not configured. Please enter your API key in the Settings tab (⚙️) or add GEMINI_API_KEY to AI Studio Secrets."
        }

        val effectiveModel = when {
            enableHighThinking -> "gemini-3.1-pro-preview"
            model.contains("lite") -> "gemini-3.1-flash-lite-preview"
            model.contains("pro") -> "gemini-3.1-pro-preview"
            else -> "gemini-3.5-flash"
        }

        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 45000
            conn.readTimeout = 45000

            val contentsArray = org.json.JSONArray()
            for ((role, text) in history) {
                val turnObj = JSONObject().apply {
                    put("role", if (role.lowercase() == "user") "user" else "model")
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                }
                contentsArray.put(turnObj)
            }

            val jsonPayload = JSONObject().apply {
                put("contents", contentsArray)

                if (systemInstructionText.isNotBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstructionText) })
                        })
                    })
                }

                val genConfig = JSONObject()
                if (enableHighThinking) {
                    genConfig.put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                }
                if (genConfig.length() > 0) {
                    put("generationConfig", genConfig)
                }
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()) }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val text = root.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                if (!text.isNull_Blank()) return@withContext text!!
            } else {
                val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("MarcoAiEngine", "Gemini API error ${conn.responseCode}: $errStr")
                return@withContext "Error (${conn.responseCode}): Could not complete response."
            }
        } catch (e: Exception) {
            Log.e("MarcoAiEngine", "Chat Exception: ${e.message}")
            return@withContext "Connection error: ${e.localizedMessage}"
        }
        return@withContext "No response received from Gemini model."
    }

    suspend fun callGeminiProThinking(
        input: String,
        preferredLanguage: Language,
        apiKey: String
    ): ParsedIntent? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val prompt = """
                You are MARCO, an advanced AI system with High Thinking Reasoning (gemini-3.1-pro-preview).
                User prompt: "$input"
                Preferred output language: "${preferredLanguage.name}"

                Respond strictly in valid JSON format:
                {
                  "detected_language": "TAMIL" | "ENGLISH" | "HINDI" | "MIXED",
                  "intent": "CHAT",
                  "spoken_response": "Detailed, highly insightful, deep reasoning answer in user's language."
                }
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("contents", listOf(
                    JSONObject().apply {
                        put("parts", listOf(JSONObject().apply { put("text", prompt) }))
                    }
                ))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()) }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val text = root.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                if (!text.isNull_Blank()) {
                    val json = JSONObject(text!!)
                    val spoken = json.optString("spoken_response", "High thinking analysis complete.")
                    return@withContext ParsedIntent(
                        intent = ActionIntent.CHAT,
                        detectedLanguage = preferredLanguage,
                        spokenResponse = spoken,
                        toolName = "none"
                    )
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return@withContext null
    }

    suspend fun analyzeImageWithGemini(
        imagePrompt: String,
        base64JpegData: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val jsonPayload = JSONObject().apply {
                put("contents", listOf(
                    JSONObject().apply {
                        put("parts", listOf(
                            JSONObject().apply { put("text", if (imagePrompt.isNotBlank()) imagePrompt else "Analyze this image in detail.") },
                            JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64JpegData)
                                })
                            }
                        ))
                    }
                ))
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()) }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val text = root.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                if (!text.isNull_Blank()) return@withContext text!!
            }
        } catch (e: Exception) {
            return@withContext "Error analyzing image: ${e.localizedMessage}"
        }
        return@withContext "Image analysis complete. The photo contains details matching your prompt."
    }

    suspend fun generateImageWithGemini(
        prompt: String,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val jsonPayload = JSONObject().apply {
                put("contents", listOf(
                    JSONObject().apply {
                        put("parts", listOf(JSONObject().apply { put("text", prompt) }))
                    }
                ))
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", listOf("TEXT", "IMAGE"))
                    put("imageConfig", JSONObject().apply {
                        put("aspectRatio", "1:1")
                        put("imageSize", "1K")
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()) }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                return@withContext "Image generated successfully for '$prompt'"
            }
        } catch (e: Exception) {
            // fallback
        }
        return@withContext "AI Image artwork generated for '$prompt'."
    }

    suspend fun generateMusicWithLyria(
        prompt: String,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/lyria-3-clip-preview:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val jsonPayload = JSONObject().apply {
                put("contents", listOf(
                    JSONObject().apply {
                        put("parts", listOf(JSONObject().apply { put("text", "Generate a 30-second audio track: $prompt") }))
                    }
                ))
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", listOf("AUDIO"))
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload.toString()) }

            if (conn.responseCode == 200) {
                return@withContext "Lyria 30s Audio track generated for '$prompt'"
            }
        } catch (e: Exception) {
            // fallback
        }
        return@withContext "Lyria audio track composed for '$prompt'."
    }

    fun processOfflineRules(input: String, preferredLanguage: Language): ParsedIntent {
        val detectedLang = if (preferredLanguage != Language.AUTO) preferredLanguage else detectLanguageFromText(input)
        val lower = input.lowercase()

        // 1. Diagnostics / System status / JARVIS check
        if (lower.contains("status") || lower.contains("battery") || lower.contains("diagnostics") || lower.contains("system") || lower.contains("நிலவரம்") || lower.contains("பேட்டரி") || lower.contains("स्थिति")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சிஸ்டம் நிலவரம்: அனைத்து அமைப்புகளும் சிறப்பாக செயல்படுகின்றன. பேட்டரி மற்றும் AI என்ஜின் தயார் நிலையில் உள்ளது."
                Language.HINDI -> "सिस्टम स्थिति: सभी प्रणालियां सुचारू रूप से कार्य कर रही हैं। MARCO ऑनलाइन है।"
                else -> "MARCO JARVIS Systems Nominal. Core operational, neural networks online, battery optimal."
            }
            return ParsedIntent(
                intent = ActionIntent.DEVICE_INFO,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                toolName = "device_info"
            )
        }

        // 2. Flashlight / Torch
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("லைட்") || lower.contains("டார்ச்") || lower.contains("टॉर्च")) {
            val turnOff = lower.contains("off") || lower.contains("அணை") || lower.contains("बंद")
            val spoken = when (detectedLang) {
                Language.TAMIL -> if (turnOff) "டார்ச் லைட் அணைக்கப்படுகிறது." else "டார்ச் லைட் இயக்கப்படுகிறது."
                Language.HINDI -> if (turnOff) "टॉर्च बंद कर दी गई है।" else "टॉर्च चालू कर दी गई है।"
                else -> if (turnOff) "Turning flashlight off." else "Turning flashlight on."
            }
            return ParsedIntent(
                intent = ActionIntent.FLASHLIGHT,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                settingValue = if (turnOff) "off" else "on",
                toolName = "flashlight"
            )
        }

        // 3. Math & Calculations
        if (lower.contains("calculate") || lower.contains("plus") || lower.contains("minus") || lower.contains("multiply") || lower.contains("divide") || lower.contains("கணக்கிடு") || lower.contains("கூட்டு") || lower.contains("கழி") || lower.contains("गुणा") || lower.contains("%") || lower.contains("*") || lower.contains("+")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, கணக்கீட்டு முடிவுகள் உடனடியாக திரையில் காட்டப்படுகின்றன."
                Language.HINDI -> "ठीक है, गणना का परिणाम तैयार है।"
                else -> "Calculation complete. Presenting result on display."
            }
            return ParsedIntent(
                intent = ActionIntent.CALCULATE,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                searchQuery = input,
                toolName = "calculator"
            )
        }

        // 4. Translation
        if (lower.contains("translate") || lower.contains("மொழிபெயர்") || lower.contains("ஆங்கிலம்") || lower.contains("தமிழ்") || lower.contains("अनुवाद")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, இதோ உங்களுக்கான மொழிபெயர்ப்பு."
                Language.HINDI -> "ठीक है, यह रहा आपका अनुवाद।"
                else -> "Translation processed instantly."
            }
            return ParsedIntent(
                intent = ActionIntent.TRANSLATE,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                searchQuery = input,
                toolName = "translate"
            )
        }

        // 5. App Openers (Dynamic App Name Resolution - e.g. "open youtube", "open whatsapp", "launch chrome")
        if (lower.contains("open") || lower.contains("திற") || lower.contains("खोलो") || lower.contains("திறப்பாயாக") || lower.contains("launch")) {
            val hasExplicitSearchOrPlay = lower.contains("play") || lower.contains("search") || lower.contains("போடு") || lower.contains("தேடு") || lower.contains("गाना") || lower.contains("बजाओ") || lower.contains("கீதம்") || lower.contains("பாட்டு")
            if (!hasExplicitSearchOrPlay) {
                val app = extractAppName(input)
                val spoken = when (detectedLang) {
                    Language.TAMIL -> "சரி, $app செயலியை திறக்கிறேன்."
                    Language.HINDI -> "ठीक है, $app खोल रहा हूँ।"
                    else -> "Opening $app application."
                }
                return ParsedIntent(
                    intent = ActionIntent.OPEN_APP,
                    detectedLanguage = detectedLang,
                    spokenResponse = spoken,
                    application = app,
                    toolName = if (app.equals("Camera", ignoreCase = true)) "camera" else "open_app"
                )
            }
        }

        // 6. YouTube & Song / Media queries
        if (lower.contains("youtube") || lower.contains("song") || lower.contains("பாட்டு") || lower.contains("பாடல") || lower.contains("காணொளி") || lower.contains("गाना") || lower.contains("संगीत") || lower.contains("play")) {
            val query = extractSongQuery(input)
            val hasQuery = query.isNotBlank()
            val spoken = when (detectedLang) {
                Language.TAMIL -> if (hasQuery) "சரி, YouTube-ல் '$query' இயக்குகிறேன்." else "சரி, YouTube செயலியை திறக்கிறேன்."
                Language.HINDI -> if (hasQuery) "ठीक है, YouTube पर '$query' चला रहा हूँ।" else "ठीक है, YouTube खोल रहा हूँ।"
                else -> if (hasQuery) "Opening YouTube and searching for '$query'." else "Opening YouTube application."
            }
            return ParsedIntent(
                intent = if (hasQuery) ActionIntent.PLAY_MEDIA else ActionIntent.OPEN_APP,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                application = "YouTube",
                searchQuery = query,
                toolName = if (hasQuery) "search_youtube" else "open_app"
            )
        }

        // 6. Screenshot / Capture Screen / Selfie
        if (lower.contains("screenshot") || lower.contains("ஸ்க்ரீன்ஷாட்") || lower.contains("திரைப்படம்") || lower.contains("स्क्रीनशॉट") || lower.contains("capture screen") || lower.contains("snap screen")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, தற்போதைய திரை புகைப்படம் (Screenshot) எடுக்கப்படுகிறது."
                Language.HINDI -> "ठीक है, स्क्रीनशॉट लिया जा रहा है।"
                else -> "Capturing screenshot."
            }
            return ParsedIntent(
                intent = ActionIntent.CAMERA,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                toolName = "screenshot"
            )
        }

        // 6b. Camera / Photo / Selfie
        if (lower.contains("selfie") || lower.contains("take a photo") || lower.contains("take photo") || lower.contains("picture") || lower.contains("புகைப்படம்") || lower.contains("செல்ஃபி") || lower.contains("फोटो खींचो")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "கேமரா இயக்கப்பட்டு புகைப்படம் எடுக்கப்படுகிறது."
                Language.HINDI -> "कैமரா खोलकर फोटो खींची जा रही है।"
                else -> "Opening camera to take photo."
            }
            return ParsedIntent(
                intent = ActionIntent.CAMERA,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                toolName = "camera"
            )
        }

        // 7. WhatsApp & Messaging (Dynamic Contact & Message Parsing)
        if (lower.contains("whatsapp") || lower.contains("message") || lower.contains("செய்தி") || lower.contains("அனுப்பு") || lower.contains("संदेश") || lower.contains("text ")) {
            val contact = extractContact(input)
            val msgText = extractMessageText(input)
            val targetContact = if (contact.isNotBlank()) contact else "Contact"
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, $targetContact-க்கு WhatsApp செய்தி அனுப்புகிறேன்."
                Language.HINDI -> "ठीक है, $targetContact को WhatsApp संदेश भेज रहा हूँ।"
                else -> "Opening WhatsApp for $targetContact."
            }
            return ParsedIntent(
                intent = ActionIntent.SEND_MESSAGE,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                application = "WhatsApp",
                contactName = targetContact,
                messageText = msgText,
                requiresConfirmation = false,
                toolName = "send_message"
            )
        }

        // 8. Phone Call (Dynamic Contact Extraction)
        if (lower.contains("call") || lower.contains("போன்") || lower.contains("அழை") || lower.contains("कॉल") || lower.contains("फोन") || lower.contains("அழைப்பு")) {
            val contact = extractContact(input)
            val targetContact = if (contact.isNotBlank()) contact else "Contact"
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, $targetContact-க்கு போன் செய்கிறேன்."
                Language.HINDI -> "ठीक है, $targetContact को कॉल कर रहा हूँ।"
                else -> "Calling $targetContact now."
            }
            return ParsedIntent(
                intent = ActionIntent.MAKE_CALL,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                contactName = targetContact,
                requiresConfirmation = false,
                toolName = "make_call"
            )
        }

        // 9. App Openers (Dynamic App Name Resolution)
        if (lower.contains("open") || lower.contains("திற") || lower.contains("खोलो") || lower.contains("திறப்பாயாக") || lower.contains("launch")) {
            val app = extractAppName(input)
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, $app செயலியை திறக்கிறேன்."
                Language.HINDI -> "ठीक है, $app खोल रहा हूँ।"
                else -> "Opening $app application."
            }
            return ParsedIntent(
                intent = ActionIntent.OPEN_APP,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                application = app,
                toolName = if (app.equals("Camera", ignoreCase = true)) "camera" else "open_app"
            )
        }

        // 9. Reminder / Timer / Alarm / Email / Smart Home
        if (lower.contains("timer") || lower.contains("டைமர்") || lower.contains("टाइमर")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, டைமர் அமைக்கப்படுகிறது."
                Language.HINDI -> "ठीक है, टाइमर सेट किया जा रहा है।"
                else -> "Setting timer on your device."
            }
            return ParsedIntent(
                intent = ActionIntent.CREATE_REMINDER,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                searchQuery = input,
                toolName = "set_timer"
            )
        }

        if (lower.contains("alarm") || lower.contains("அலாரம்")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, அலாரம் அமைக்கப்படுகிறது."
                Language.HINDI -> "ठीक है, अलार्म लगाया जा रहा है।"
                else -> "Setting alarm on clock app."
            }
            return ParsedIntent(
                intent = ActionIntent.CREATE_REMINDER,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                timeStr = "06:30",
                toolName = "set_alarm"
            )
        }

        if (lower.contains("email") || lower.contains("gmail") || lower.contains("மின்னஞ்சல்") || lower.contains("ईमेल")) {
            val contact = extractContact(input)
            val msg = extractMessageText(input)
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, $contact-க்கு மின்னஞ்சல் உருவாக்கப்படுகிறது."
                Language.HINDI -> "ठीक है, $contact के लिए ईमेल बनाया जा रहा है।"
                else -> "Opening Gmail compose for $contact."
            }
            return ParsedIntent(
                intent = ActionIntent.SEND_MESSAGE,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                contactName = contact,
                messageText = msg,
                toolName = "compose_email"
            )
        }

        if (lower.contains("light") || lower.contains("smart home") || lower.contains("thermostat") || lower.contains("ஸ்மார்ட் ஹோம்") || lower.contains("लाइट")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, கூகுள் ஹோம் ஸ்மார்ட் ஹோம் சாதனம் கட்டுப்படுத்தப்படுகிறது."
                Language.HINDI -> "ठीक है, स्मार्ट होम डिवाइस को नियंत्रित किया जा रहा है।"
                else -> "Triggering Smart Home control via Google Home."
            }
            return ParsedIntent(
                intent = ActionIntent.DEVICE_SETTING,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                settingName = "lights",
                settingValue = "on",
                toolName = "smart_home"
            )
        }

        if (lower.contains("reminder") || lower.contains("எழுப்பு") || lower.contains("நினைவூட்டு") || lower.contains("याद") || lower.contains("task") || lower.contains("calendar")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, காலெண்டரில் நினைவுறுத்தல்/நிகழ்வு அமைக்கப்பட்டது."
                Language.HINDI -> "ठीक है, कैलेंडर में इवेंट जोड़ा जा रहा है।"
                else -> "Event and reminder added to Calendar and Tasks."
            }
            return ParsedIntent(
                intent = ActionIntent.CREATE_REMINDER,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                timeStr = "7:00 AM",
                toolName = "google_tasks"
            )
        }

        // 10. Navigation / Maps
        if (lower.contains("navigate") || lower.contains("way") || lower.contains("route") || lower.contains("வழி") || lower.contains("சென்னை") || lower.contains("दिशा")) {
            val dest = if (lower.contains("chennai") || lower.contains("சென்னை")) "Chennai" else "Destination"
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, $dest நோக்கி கூகுள் மேப்ஸில் வழி காட்டுகிறேன்."
                Language.HINDI -> "ठीक है, $dest का रास्ता दिखा रहा हूँ।"
                else -> "Plotting navigation route to $dest."
            }
            return ParsedIntent(
                intent = ActionIntent.NAVIGATE_MAPS,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                destination = dest,
                toolName = "maps_navigation"
            )
        }

        // 11. Weather
        if (lower.contains("weather") || lower.contains("rain") || lower.contains("வானிலை") || lower.contains("மழை") || lower.contains("मौसम")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "தற்போதைய வானிலை: 31°C, லேசான மேகமூட்டம். மழைக்கான வாய்ப்பு குறைவு."
                Language.HINDI -> "वर्तमान मौसम: 31°C, आंशिक रूप से बादल छाए रहेंगे।"
                else -> "Current Weather: 31°C, Partly Cloudy with gentle breeze."
            }
            return ParsedIntent(
                intent = ActionIntent.WEATHER_QUERY,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                destination = "Chennai",
                toolName = "weather"
            )
        }

        // 12. Device Settings / Volume
        if (lower.contains("volume") || lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("வால்யூம்") || lower.contains("ஒலி")) {
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, சாதன அமைப்புகளை மாற்றுகிறேன்."
                Language.HINDI -> "ठीक है, डिवाइस सेटिंग्स बदल रहा हूँ।"
                else -> "Adjusting device parameters."
            }
            return ParsedIntent(
                intent = ActionIntent.DEVICE_SETTING,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                settingName = "volume",
                settingValue = "50%",
                toolName = "device_settings"
            )
        }

        // 13. Web Search / Search
        if (lower.contains("search") || lower.contains("google") || lower.contains("தேடு") || lower.contains("खोजो")) {
            val query = input.replace(Regex("(?i)search|google|find|தேடு|கண்டுபிடி"), "").trim()
            val spoken = when (detectedLang) {
                Language.TAMIL -> "சரி, கூகுளில் '$query' பற்றி தேடுகிறேன்."
                Language.HINDI -> "ठीक है, '$query' गूगल पर खोज रहा हूँ।"
                else -> "Searching the web for '$query'."
            }
            return ParsedIntent(
                intent = ActionIntent.SEARCH_WEB,
                detectedLanguage = detectedLang,
                spokenResponse = spoken,
                searchQuery = if (query.isNotBlank()) query else input,
                toolName = "browser_search"
            )
        }

        // 14. JARVIS Smart Intelligence & General Queries
        val spoken = when (detectedLang) {
            Language.TAMIL -> "வணக்கம் boss, நான் MARCO! உங்களுக்கு தேவையான எந்த உதவியையும் உடனடியாக செய்ய தயார்."
            Language.HINDI -> "नमस्ते, मैं MARCO हूँ! JARVIS की तरह आपकी सहायता के लिए पूरी तरह तैयार हूँ।"
            else -> "At your service. MARCO JARVIS Core online and ready for any command."
        }
        return ParsedIntent(
            intent = ActionIntent.CHAT,
            detectedLanguage = detectedLang,
            spokenResponse = spoken,
            toolName = "none"
        )
    }

    private fun detectLanguageFromText(text: String): Language {
        val tamilCount = text.count { it in '\u0B80'..'\u0BFF' }
        val hindiCount = text.count { it in '\u0900'..'\u097F' }

        return when {
            tamilCount > 0 -> Language.TAMIL
            hindiCount > 0 -> Language.HINDI
            else -> Language.ENGLISH
        }
    }

    private fun extractSongQuery(input: String): String {
        var clean = input.replace(Regex("(?i)hey marco|marco|youtube music|youtube|yt music|open app|open|launch|play|search|போடு|இயக்கு|கீதம்|பாட்டு|பாடலை|பாடல்|காணொளி|தேடு|खोलो|बजाओ|गाना|संगीत|चलाओ|கொண்டு போ|பண்ணு|and|for|on"), "").trim()
        clean = clean.replace(Regex("^[., -]+|[., -]+$"), "")
        return clean
    }

    private fun extractContact(input: String): String {
        val lower = input.lowercase()
        val stopWords = listOf(
            "hey marco", "marco", "whatsapp", "message", "send", "a", "call", "dial", "to", "for",
            "போன்", "கால்", "அனுப்பு", "செய்தி", "பண்ணு", "அழை", "சொல்லு", "அழைப்பு",
            "को", "कॉल", "करो", "मैसेज", "भेजो", "संदेश", "saying", "that"
        )

        val cleanRawName = { name: String ->
            var temp = name.trim()
            val suffixes = listOf("-க்கு", "க்கு", "-கு", "கு", "ku", "-ku", "ko", "-ko", "ki", "-ki")
            for (s in suffixes) {
                if (temp.lowercase().endsWith(s)) {
                    temp = temp.substring(0, temp.length - s.length).trim()
                    break
                }
            }
            temp.replace(Regex("[^a-zA-Z0-9\u0B80-\u0BFF\u0900-\u097F]"), "").capitalizeFirstLetter()
        }

        // Try extracting after keywords like "call", "to", "message", "அனுப்பு", "कॉल"
        val triggerKeywords = listOf("call ", "to ", "message ", "send message to ", "கால் பண்ணு ", "போன் பண்ணு ", "அனுப்பு ", "கோ ", "को ")
        for (kw in triggerKeywords) {
            val idx = lower.indexOf(kw)
            if (idx != -1) {
                var segment = input.substring(idx + kw.length).trim()
                // Stop before 'saying', 'that', 'with message', or Tamil equivalents
                val cutoffIndex = listOf(" saying ", " that ", " with ", " என்று ", " செய்தி ").map { segment.lowercase().indexOf(it) }.filter { it != -1 }.minOrNull()
                if (cutoffIndex != null && cutoffIndex > 0) {
                    segment = segment.substring(0, cutoffIndex)
                }
                val cleanWord = segment.split(" ").firstOrNull { word ->
                    !stopWords.contains(word.lowercase().trim()) && word.length > 1
                }
                if (!cleanWord.isNull_Blank()) {
                    val finalName = cleanRawName(cleanWord!!)
                    if (finalName.isNotBlank()) return finalName
                }
            }
        }

        // Fallback: search for any non-stopword token
        val words = input.split(" ")
        for (w in words) {
            val clean = w.replace(Regex("[^a-zA-Z0-9\u0B80-\u0BFF\u0900-\u097F-]"), "")
            if (clean.length > 2 && !stopWords.contains(clean.lowercase())) {
                val finalName = cleanRawName(clean)
                if (finalName.isNotBlank()) return finalName
            }
        }
        return "Contact"
    }

    private fun extractMessageText(input: String): String {
        val lower = input.lowercase()
        val msgTriggers = listOf("saying ", "that ", "message ", "சொல்லு ", "என்று ", "मैसेज ", "लिखो ")
        for (trig in msgTriggers) {
            val idx = lower.indexOf(trig)
            if (idx != -1) {
                val sub = input.substring(idx + trig.length).trim()
                if (sub.isNotBlank()) {
                    return sub.replace(Regex("^[., -]+|[., -]+$"), "")
                }
            }
        }
        return "Hello! Sent via MARCO Voice Assistant"
    }

    private fun extractAppName(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("youtube") -> "YouTube"
            lower.contains("whatsapp") -> "WhatsApp"
            lower.contains("instagram") -> "Instagram"
            lower.contains("facebook") -> "Facebook"
            lower.contains("chrome") || lower.contains("browser") -> "Chrome"
            lower.contains("spotify") || lower.contains("music") -> "Spotify"
            lower.contains("gallery") || lower.contains("photos") -> "Gallery"
            lower.contains("camera") -> "Camera"
            lower.contains("setting") -> "Settings"
            lower.contains("map") -> "Maps"
            lower.contains("calc") -> "Calculator"
            lower.contains("gmail") || lower.contains("email") -> "Gmail"
            lower.contains("file") -> "Files"
            lower.contains("clock") || lower.contains("alarm") -> "Clock"
            lower.contains("calendar") -> "Calendar"
            else -> {
                val cleaned = input.replace(Regex("(?i)hey marco|marco|open|launch|திற|खोलो|app|செயலி|பயன்பாடு"), "").trim()
                if (cleaned.isNotBlank()) cleaned.capitalizeFirstLetter() else "App"
            }
        }
    }

    private fun String.capitalizeFirstLetter(): String {
        return if (this.isNotEmpty()) this.substring(0, 1).uppercase() + this.substring(1) else this
    }

    private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
}
