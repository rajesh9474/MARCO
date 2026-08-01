package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseAuthManager
import com.example.ui.MarcoViewModel
import kotlinx.coroutines.launch

data class ChatRoleItem(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val systemInstruction: String,
    val defaultModel: String
)

val CHAT_ROLES = listOf(
    ChatRoleItem(
        id = "marco",
        title = "MARCO Voice Assistant",
        icon = Icons.Default.SmartToy,
        systemInstruction = "You are MARCO, an advanced autonomous AI assistant. Respond intelligently, politely, and effectively.",
        defaultModel = "gemini-3.5-flash"
    ),
    ChatRoleItem(
        id = "coder",
        title = "Code & Tech Expert",
        icon = Icons.Default.Code,
        systemInstruction = "You are an expert software developer and system architect. Provide clean, well-annotated code, step-by-step algorithms, and technical solutions.",
        defaultModel = "gemini-3.1-pro-preview"
    ),
    ChatRoleItem(
        id = "speed",
        title = "Fast Concierge",
        icon = Icons.Default.Bolt,
        systemInstruction = "You are a lightning fast assistant. Keep responses ultra concise, direct, and under 3 sentences.",
        defaultModel = "gemini-3.1-flash-lite-preview"
    ),
    ChatRoleItem(
        id = "creative",
        title = "Content & Creative Editor",
        icon = Icons.Default.EditNote,
        systemInstruction = "You are a creative writer and content editor. Refine, proofread, and compose engaging copy.",
        defaultModel = "gemini-3.5-flash"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    viewModel: MarcoViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isChatGenerating.collectAsState()
    val selectedRole by viewModel.selectedChatRole.collectAsState()
    val selectedModel by viewModel.selectedGeminiModel.collectAsState()
    val isHighThinkingEnabled by viewModel.isHighThinkingEnabled.collectAsState()
    val currentUser by FirebaseAuthManager.instance.currentUser.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showRoleMenu by remember { mutableStateOf(false) }
    var showCustomInstructionDialog by remember { mutableStateOf(false) }
    var customInstructionText by remember { mutableStateOf(selectedRole.systemInstruction) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar & Auth Indicator
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedRole.icon,
                                contentDescription = selectedRole.title,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = selectedRole.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUser != null) "User: ${currentUser?.displayName ?: "Signed In"} (Firestore Persistent)" else "Guest User • Sign in for Cloud Persistence",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { showRoleMenu = !showRoleMenu },
                            modifier = Modifier.testTag("role_select_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Role and Model Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearChatHistory() },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Chat Thread",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Expandable Role & Model Configuration Panel
                AnimatedVisibility(visible = showRoleMenu) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select Chatbot Role",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CHAT_ROLES.forEach { role ->
                                val isSelected = role.id == selectedRole.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setSelectedChatRole(role)
                                        customInstructionText = role.systemInstruction
                                    },
                                    label = { Text(role.title.split(" ").first(), fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = role.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("role_chip_${role.id}")
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Text(
                            text = "Gemini Model Selection",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val models = listOf(
                                "gemini-3.5-flash" to "3.5 Flash (General)",
                                "gemini-3.1-pro-preview" to "3.1 Pro (Complex)",
                                "gemini-3.1-flash-lite-preview" to "3.1 Lite (Fast)"
                            )
                            models.forEach { (modKey, modName) ->
                                val isModSelected = selectedModel == modKey
                                FilterChip(
                                    selected = isModSelected,
                                    onClick = { viewModel.setSelectedGeminiModel(modKey) },
                                    label = { Text(modName, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("model_chip_${modKey.replace(".", "_")}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable High Thinking Mode",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Uses gemini-3.1-pro-preview with ThinkingLevel.HIGH for complex queries",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isHighThinkingEnabled,
                                onCheckedChange = { viewModel.setHighThinkingEnabled(it) },
                                modifier = Modifier.testTag("high_thinking_toggle")
                            )
                        }

                        OutlinedButton(
                            onClick = { showCustomInstructionDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_system_instruction_button")
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View / Edit System Instruction", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Chat Message Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedRole.icon,
                                contentDescription = "Gemini",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .testTag(if (isUser) "user_message_bubble" else "gemini_message_bubble")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!isUser && msg.modelUsed.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Model: ${msg.modelUsed}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (isUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (isGenerating) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isHighThinkingEnabled) "Gemini is reasoning (High Thinking mode)..." else "Gemini is generating response...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Input Field Bar
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Gemini multi-turn chatbot...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendChatMessage(textToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("send_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Custom System Instruction Dialog
    if (showCustomInstructionDialog) {
        AlertDialog(
            onDismissRequest = { showCustomInstructionDialog = false },
            title = { Text("System Instruction") },
            text = {
                Column {
                    Text("System instructions set the chatbot's core role and behavior:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customInstructionText,
                        onValueChange = { customInstructionText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateRoleSystemInstruction(customInstructionText)
                    showCustomInstructionDialog = false
                }) {
                    Text("Save Instruction")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomInstructionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
