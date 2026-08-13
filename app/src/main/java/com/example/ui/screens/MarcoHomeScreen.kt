package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.example.data.AssistantState
import com.example.data.Language
import com.example.ui.MarcoViewModel
import com.example.ui.theme.MarcoCardSurface
import com.example.ui.theme.MarcoCyanPrimary
import com.example.ui.theme.MarcoDarkBackground
import com.example.ui.theme.MarcoEmeraldSuccess
import com.example.ui.theme.MarcoPinkAccent
import com.example.ui.theme.MarcoPurpleSecondary
import com.example.ui.theme.MarcoSurfaceDark
import com.example.ui.theme.MarcoTextMuted
import com.example.ui.theme.MarcoTextPrimary
import com.example.ui.theme.MarcoTextSecondary

@OptIn(ExperimentalLayoutApi::class, ExperimentalPermissionsApi::class)
@Composable
fun MarcoHomeScreen(
    viewModel: MarcoViewModel,
    modifier: Modifier = Modifier
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_CONTACTS
        )
    )

    val assistantState by viewModel.assistantState.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val currentPrompt by viewModel.currentPrompt.collectAsState()
    val lastParsedIntent by viewModel.lastParsedIntent.collectAsState()
    val lastToolResult by viewModel.lastToolResult.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmationIntent.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val isListening by viewModel.speechToText.isListening.collectAsState()
    val partialText by viewModel.speechToText.partialText.collectAsState()
    val isSpeaking by viewModel.textToSpeech.isSpeaking.collectAsState()
    val rmsDb by viewModel.speechToText.rmsDb.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var typedInput by remember { mutableStateOf("") }

    val handleMicToggle = {
        val audioGranted = permissionsState.permissions.firstOrNull { it.permission == android.Manifest.permission.RECORD_AUDIO }?.status?.isGranted == true
        if (audioGranted) {
            if (isListening) viewModel.stopListening() else viewModel.startListening()
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    val quickActions = remember {
        listOf(
            QuickActionItem("WhatsApp Msg", "Hey MARCO, WhatsApp-ல Vijay-க்கு message: I am on my way.", "💬"),
            QuickActionItem("Phone Call", "Hey MARCO, Call Vijay", "📞"),
            QuickActionItem("Take Screenshot", "Hey MARCO, take a screenshot", "📸"),
            QuickActionItem("Capture Photo", "Hey MARCO, take a selfie", "🤳"),
            QuickActionItem("Open Instagram", "Hey MARCO, open Instagram", "📱"),
            QuickActionItem("YouTube Music", "YouTube-க்கு போய் ஒரு தமிழ் பாட்டு போடு.", "🎬"),
            QuickActionItem("JARVIS Diagnostics", "MARCO system status & battery check", "⚡"),
            QuickActionItem("Toggle Flashlight", "Turn on torch light", "🔦")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val ambientPrimary = MaterialTheme.colorScheme.primary
        val ambientSecondary = MaterialTheme.colorScheme.secondary
        // Atmospheric Radial Glow Canvas Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Cyan/Primary ambient top glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ambientPrimary.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.25f),
                    radius = canvasWidth * 0.7f
                )
            )

            // Indigo/Secondary bottom glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ambientSecondary.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.8f),
                    radius = canvasWidth * 0.8f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }

                // App Header
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "MARCO",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp
                            ),
                            color = MarcoCyanPrimary
                        )
                        Text(
                            text = "AUTONOMOUS AI VOICE ASSISTANT",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = MarcoTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // JARVIS CORE ONLINE / OFFLINE Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MarcoSurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isOnline) MarcoCyanPrimary.copy(alpha = 0.4f) else Color(0xFFF59E0B)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) MarcoEmeraldSuccess else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOnline) "JARVIS CORE ONLINE • தமிழ் | EN | हिन्दी" else "OFFLINE MODE ACTIVE • Local Rule Engine",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOnline) MarcoTextPrimary else Color(0xFFFDE68A)
                                )
                            }
                        }
                    }
                }

                // Assistant Runtime Permissions Banner (Accompanist Permissions)
                item {
                    AssistantPermissionsBanner(permissionsState = permissionsState)
                }

                // Offline Network Alert Banner
                if (!isOnline) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("offline_network_banner"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Offline Mode Alert",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Offline Mode Active",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFDE68A)
                                    )
                                    Text(
                                        text = "Device is disconnected from the internet. MARCO has automatically fallback-switched to the zero-latency local rule engine for offline voice commands (WhatsApp, Calls, Camera, Flashlight, Alarms).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Language Selector Pills
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Language.entries.forEach { lang ->
                            val isSelected = preferredLanguage == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setPreferredLanguage(lang) },
                                label = { Text(lang.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .testTag("lang_chip_${lang.code}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MarcoCyanPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = MarcoSurfaceDark,
                                    labelColor = MarcoTextSecondary
                                )
                            )
                        }
                    }
                }

                // The Voice Orb Visualizer with Equalizer Waveform
                item {
                    TheVoiceOrb(
                        state = assistantState,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        rmsDb = rmsDb,
                        onOrbClick = handleMicToggle
                    )
                }

                // Real-time Animated Waveform Component when listening
                item {
                    RealtimeListeningWaveform(
                        isListening = isListening,
                        rmsDb = rmsDb
                    )
                }

                // Real-time Speech Transcription & Status Output
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusText = when (assistantState) {
                            AssistantState.LISTENING -> "Listening to speech..."
                            AssistantState.PROCESSING -> "MARCO is thinking..."
                            AssistantState.SPEAKING -> "MARCO is speaking..."
                            AssistantState.EXECUTING -> "Executing Android command..."
                            AssistantState.WAITING_CONFIRMATION -> "Awaiting confirmation..."
                            AssistantState.IDLE -> "Tap orb or speak command"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (assistantState == AssistantState.LISTENING) {
                                PulsingMicIcon(
                                    isListening = true,
                                    iconSize = 18.dp,
                                    tint = MarcoPinkAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Text(
                                text = statusText.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = when (assistantState) {
                                    AssistantState.LISTENING -> MarcoPinkAccent
                                    AssistantState.EXECUTING -> MarcoEmeraldSuccess
                                    AssistantState.PROCESSING -> MarcoCyanPrimary
                                    else -> MarcoTextSecondary
                                }
                            )
                        }

                        val activeSpeechText = if (isListening && partialText.isNotBlank()) {
                            "\"$partialText\""
                        } else if (currentPrompt.isNotBlank()) {
                            "\"$currentPrompt\""
                        } else ""

                        if (activeSpeechText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MarcoSurfaceDark),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MarcoCyanPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = activeSpeechText,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                    color = MarcoTextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                )
                            }
                        }
                    }
                }

                // Stop Speaking Action Button
                if (isSpeaking) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.stopSpeaking() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MarcoPinkAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MarcoPinkAccent),
                            modifier = Modifier.testTag("stop_speaking_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Speech")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Voice Response")
                        }
                    }
                }

                // Action Result & Response Banner
                if (lastParsedIntent != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_result_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (lastToolResult?.success == true)
                                    Color(0xFF064E3B).copy(alpha = 0.85f)
                                else
                                    MarcoCardSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (lastToolResult?.success == true) MarcoEmeraldSuccess else MarcoPurpleSecondary
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MarcoPurpleSecondary)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = lastParsedIntent?.intent?.name ?: "ACTION",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MarcoCyanPrimary)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = lastParsedIntent?.detectedLanguage?.displayName ?: "Lang",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = lastParsedIntent?.spokenResponse ?: "",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White
                                )

                                if (lastToolResult != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Status: ${lastToolResult?.message}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (lastToolResult?.success == true) MarcoEmeraldSuccess else MarcoPinkAccent
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Creation & Intelligence Studio Card
                item {
                    val isHighThinking by viewModel.isHighThinkingEnabled.collectAsState()
                    val aiContent by viewModel.aiGeneratedContent.collectAsState()
                    var studioPrompt by remember { mutableStateOf("") }
                    var studioMode by remember { mutableStateOf("IMAGE") } // IMAGE, MUSIC, VISION

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_studio_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MarcoCardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MarcoCyanPrimary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "AI CREATIVE STUDIO",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                        color = MarcoCyanPrimary
                                    )
                                    Text(
                                        text = "Powered by Gemini 3.1 Pro, Flash & Lyria 3",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MarcoTextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("High Thinking", style = MaterialTheme.typography.labelSmall, color = MarcoTextMuted)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Switch(
                                        checked = isHighThinking,
                                        onCheckedChange = { viewModel.setHighThinking(it) },
                                        modifier = Modifier.testTag("high_thinking_switch")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Mode Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("IMAGE" to "🎨 Image", "MUSIC" to "🎵 Lyria", "VISION" to "👁️ Vision").forEach { (mode, label) ->
                                    FilterChip(
                                        selected = studioMode == mode,
                                        onClick = { studioMode = mode },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MarcoCyanPrimary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = MarcoTextSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = studioPrompt,
                                onValueChange = { studioPrompt = it },
                                placeholder = {
                                    Text(
                                        when (studioMode) {
                                            "IMAGE" -> "Prompt: 'A glowing futuristic JARVIS AI arc core...'"
                                            "MUSIC" -> "Prompt: 'A 30-second orchestral cyberpunk beat...'"
                                            else -> "Describe screenshot or image to analyze..."
                                        },
                                        color = MarcoTextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_prompt_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MarcoCyanPrimary,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF090D16),
                                    unfocusedContainerColor = Color(0xFF090D16)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    when (studioMode) {
                                        "IMAGE" -> viewModel.generateImagePrompt(studioPrompt)
                                        "MUSIC" -> viewModel.generateMusicTrack(studioPrompt)
                                        "VISION" -> viewModel.analyzeImagePhoto(studioPrompt, "")
                                    }
                                    studioPrompt = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MarcoCyanPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_generate_button")
                            ) {
                                Text(
                                    when (studioMode) {
                                        "IMAGE" -> "Generate AI Image Artwork"
                                        "MUSIC" -> "Compose Lyria Audio Track"
                                        else -> "Run Gemini Vision Analysis"
                                    },
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!aiContent.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF090D16),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MarcoPurpleSecondary)
                                ) {
                                    Text(
                                        text = aiContent!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MarcoTextPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Action Chip Carousel
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MarcoTextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickActions.forEachIndexed { idx, action ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MarcoSurfaceDark,
                                    modifier = Modifier
                                        .clickable { viewModel.processTextInput(action.command) }
                                        .testTag("quick_action_$idx"),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(text = action.iconEmoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = action.title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MarcoTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Conversations Logs (Up to 50 stored in local Room DB formatted as chat bubbles)
                if (conversations.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = MarcoCyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONVERSATION HISTORY (${conversations.size}/50)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MarcoTextSecondary
                                )
                            }
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MarcoPinkAccent,
                                modifier = Modifier
                                    .clickable { viewModel.clearConversationHistory() }
                                    .testTag("clear_history_button")
                            )
                        }
                    }

                    items(
                        items = conversations,
                        key = { it.id }
                    ) { item ->
                        val timeStr = remember(item.timestamp) {
                            try {
                                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(item.timestamp))
                            } catch (e: Exception) {
                                ""
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("conversation_card_${item.id}"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. User Chat Bubble (Aligned Right)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                                    color = MarcoCyanPrimary.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MarcoCyanPrimary.copy(alpha = 0.35f)),
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "You",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MarcoCyanPrimary
                                            )
                                            if (timeStr.isNotBlank()) {
                                                Text(
                                                    text = timeStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MarcoTextMuted,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.userPrompt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MarcoTextPrimary
                                        )
                                    }
                                }
                            }

                            // 2. MARCO Response Chat Bubble (Aligned Left)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                                    color = MarcoCardSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.widthIn(max = 320.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(MarcoPurpleSecondary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "M", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "MARCO",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MarcoPurpleSecondary
                                                )
                                            }
                                            if (item.language.isNotBlank()) {
                                                Surface(
                                                    color = MarcoSurfaceDark,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = item.language.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MarcoTextMuted,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.marcoResponse,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MarcoTextPrimary
                                        )
                                        if (item.executedTool.isNotBlank() && item.executedTool != "none") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(MarcoSurfaceDark, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MarcoPurpleSecondary,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Tool: ${item.executedTool}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MarcoPurpleSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Bottom Floating Input Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MarcoSurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, MarcoCyanPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedInput,
                        onValueChange = { typedInput = it },
                        placeholder = { Text("Ask MARCO in Tamil, English, or Hindi...", color = MarcoTextMuted, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input"),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MarcoCyanPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF090D16),
                            unfocusedContainerColor = Color(0xFF090D16)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (typedInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.processTextInput(typedInput)
                                typedInput = ""
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MarcoCyanPrimary)
                                .testTag("send_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.Black
                            )
                        }
                    } else {
                        IconButton(
                            onClick = handleMicToggle,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isListening) MarcoPinkAccent else MarcoCyanPrimary)
                                .testTag("mic_fab")
                        ) {
                            PulsingMicIcon(
                                isListening = isListening,
                                iconSize = 24.dp,
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Confirmation Dialog
        if (pendingConfirmation != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelPendingAction() },
                modifier = Modifier.testTag("confirm_dialog"),
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MarcoPinkAccent) },
                title = { Text("Confirmation Required") },
                text = {
                    Text(
                        pendingConfirmation?.spokenResponse
                            ?: "MARCO is ready to execute this action. Proceed?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmPendingAction() },
                        colors = ButtonDefaults.buttonColors(containerColor = MarcoEmeraldSuccess),
                        modifier = Modifier.testTag("confirm_yes_button")
                    ) {
                        Text("Yes, Execute")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { viewModel.cancelPendingAction() },
                        modifier = Modifier.testTag("confirm_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

data class QuickActionItem(
    val title: String,
    val command: String,
    val iconEmoji: String
)

@Composable
fun TheVoiceOrb(
    state: AssistantState,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    onOrbClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 450 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Rotating ring angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val reverseRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    val dbScaling = (rmsDb / 10f).coerceIn(0f, 0.35f)
    val totalScale = pulseScale + dbScaling

    val orbGradient = when (state) {
        AssistantState.LISTENING -> listOf(MarcoPinkAccent, MarcoPurpleSecondary, MarcoCyanPrimary)
        AssistantState.PROCESSING -> listOf(MarcoCyanPrimary, MarcoPurpleSecondary)
        AssistantState.SPEAKING -> listOf(MarcoEmeraldSuccess, MarcoCyanPrimary, MarcoPurpleSecondary)
        AssistantState.EXECUTING -> listOf(MarcoEmeraldSuccess, MarcoPinkAccent)
        else -> listOf(MarcoCyanPrimary, MarcoPurpleSecondary)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(230.dp)
            .padding(8.dp)
    ) {
        // Rotating JARVIS Arc Reactor HUD Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.width / 2f) - 6.dp.toPx()
            val midRadius = (size.width / 2f) - 24.dp.toPx()

            // Outer dashed cyan HUD ring
            drawCircle(
                color = MarcoCyanPrimary.copy(alpha = 0.4f),
                center = center,
                radius = outerRadius * totalScale.coerceAtMost(1.15f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 15f), rotationAngle)
                )
            )

            // Inner counter-rotating magenta HUD ring
            drawCircle(
                color = MarcoPinkAccent.copy(alpha = 0.35f),
                center = center,
                radius = midRadius * totalScale.coerceAtMost(1.1f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 20f), reverseRotationAngle)
                )
            )
        }

        // Pulse ring 1 (Outer)
        Box(
            modifier = Modifier
                .size(175.dp)
                .scale(totalScale * 1.12f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            orbGradient.first().copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Pulse ring 2 (Middle)
        Box(
            modifier = Modifier
                .size(145.dp)
                .scale(totalScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            orbGradient.last().copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Core Glowing Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(orbGradient))
                .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .clickable { onOrbClick() }
                .testTag("mic_button")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PulsingMicIcon(
                    isListening = isListening,
                    iconSize = 28.dp,
                    tint = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Inner Audio Visualizer Wave Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeWave = isListening || isSpeaking || state == AssistantState.PROCESSING
                    for (i in 0..5) {
                        val barHeightAnim by infiniteTransition.animateFloat(
                            initialValue = 8f,
                            targetValue = if (activeWave) (20f + (i * 5f) % 20f + (rmsDb * 1.5f)) else 6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 220 + (i * 65),
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "wave_bar_$i"
                        )

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeightAnim.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingMicIcon(
    isListening: Boolean,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp,
    tint: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isListening) 0.15f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        if (isListening) {
            // Concentric animated pulsing aura 1
            Box(
                modifier = Modifier
                    .size(iconSize * 2.2f)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MarcoPinkAccent.copy(alpha = pulseAlpha))
            )
            // Concentric animated pulsing aura 2
            Box(
                modifier = Modifier
                    .size(iconSize * 1.6f)
                    .scale(pulseScale * 0.85f)
                    .clip(CircleShape)
                    .background(MarcoCyanPrimary.copy(alpha = pulseAlpha * 1.5f))
            )
        }

        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = "Active Microphone",
            tint = if (isListening) tint else MarcoTextMuted,
            modifier = Modifier
                .size(iconSize)
                .testTag("pulsing_mic_icon")
        )
    }
}

@Composable
fun RealtimeListeningWaveform(
    isListening: Boolean,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isListening,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveform_animation")

        val phaseShift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase_shift"
        )

        val secondaryPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -(2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "secondary_phase"
        )

        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(vertical = 4.dp)
                .testTag("realtime_waveform_visualizer"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MarcoSurfaceDark.copy(alpha = 0.9f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MarcoPinkAccent.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f

                    // Calculate amplitude driven by audio input volume (rmsDb)
                    val baseAmp = 12f + (rmsDb.coerceIn(0f, 15f) * 3.5f)

                    // 1. Primary smooth cyan sine wave path
                    val cyanPath = androidx.compose.ui.graphics.Path()
                    val frequency1 = 0.025f
                    cyanPath.moveTo(0f, centerY)
                    for (x in 0..width.toInt() step 4) {
                        val envelope = kotlin.math.sin(x * Math.PI / width)
                        val y = centerY + kotlin.math.sin(x * frequency1 + phaseShift) * baseAmp * envelope
                        cyanPath.lineTo(x.toFloat(), y.toFloat())
                    }

                    drawPath(
                        path = cyanPath,
                        color = MarcoCyanPrimary,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    // 2. Secondary magenta sine wave path
                    val pinkPath = androidx.compose.ui.graphics.Path()
                    val frequency2 = 0.035f
                    pinkPath.moveTo(0f, centerY)
                    for (x in 0..width.toInt() step 4) {
                        val envelope = kotlin.math.sin(x * Math.PI / width)
                        val y = centerY + kotlin.math.cos(x * frequency2 + secondaryPhase) * (baseAmp * 0.75f) * envelope
                        pinkPath.lineTo(x.toFloat(), y.toFloat())
                    }

                    drawPath(
                        path = pinkPath,
                        color = MarcoPinkAccent,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    // 3. Central vertical spectrum frequency bars
                    val barCount = 22
                    val barSpacing = width / (barCount + 1)
                    for (i in 1..barCount) {
                        val barX = i * barSpacing
                        val barHeight = (baseAmp * kotlin.math.abs(kotlin.math.sin(i * 0.6f + phaseShift))).coerceAtLeast(6f)
                        drawLine(
                            color = MarcoPurpleSecondary.copy(alpha = 0.65f),
                            start = androidx.compose.ui.geometry.Offset(barX, centerY - barHeight / 2f),
                            end = androidx.compose.ui.geometry.Offset(barX, centerY + barHeight / 2f),
                            strokeWidth = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

/**
 * Accompanist Permissions Banner for requesting and managing RECORD_AUDIO, CALL_PHONE, and READ_CONTACTS permissions.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AssistantPermissionsBanner(
    permissionsState: com.google.accompanist.permissions.MultiplePermissionsState
) {
    if (!permissionsState.allPermissionsGranted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("assistant_permissions_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF31101E)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MarcoPinkAccent)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Assistant Permissions Required",
                        tint = MarcoPinkAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assistant Permissions Required",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Grant Microphone, Phone Call, and Contacts permissions for MARCO to directly make calls, send WhatsApp messages, and recognize voice commands.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { permissionsState.launchMultiplePermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = MarcoPinkAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_assistant_permissions_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Grant All Permissions",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        }
    }
}
