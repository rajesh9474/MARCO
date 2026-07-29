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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarcoHomeScreen(
    viewModel: MarcoViewModel,
    modifier: Modifier = Modifier
) {
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

    var typedInput by remember { mutableStateOf("") }

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
            .background(MarcoDarkBackground)
    ) {
        // Atmospheric Radial Glow Canvas Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Cyan ambient top glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(MarcoCyanPrimary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.25f),
                    radius = canvasWidth * 0.7f
                )
            )

            // Indigo bottom glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(MarcoPurpleSecondary.copy(alpha = 0.15f), Color.Transparent),
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

                        // JARVIS CORE ONLINE Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MarcoSurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MarcoCyanPrimary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MarcoEmeraldSuccess)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "JARVIS CORE ONLINE • தமிழ் | EN | हिन्दी",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MarcoTextPrimary
                                )
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
                        onOrbClick = {
                            if (isListening) viewModel.stopListening() else viewModel.startListening()
                        }
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

                // Recent Conversations Logs
                if (conversations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Interaction History",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MarcoTextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }

                    items(conversations.take(4)) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MarcoSurfaceDark.copy(alpha = 0.7f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "You: ${item.userPrompt}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MarcoCyanPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MARCO: ${item.marcoResponse}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MarcoTextPrimary
                                )
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
                            onClick = {
                                if (isListening) viewModel.stopListening() else viewModel.startListening()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isListening) MarcoPinkAccent else MarcoCyanPrimary)
                                .testTag("mic_fab")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Mic FAB",
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
            // Inner Audio Visualizer Wave Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeWave = isListening || isSpeaking || state == AssistantState.PROCESSING
                for (i in 0..5) {
                    val barHeightAnim by infiniteTransition.animateFloat(
                        initialValue = 12f,
                        targetValue = if (activeWave) (28f + (i * 7f) % 28f + (rmsDb * 1.8f)) else 10f,
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
                            .width(5.dp)
                            .height(barHeightAnim.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
