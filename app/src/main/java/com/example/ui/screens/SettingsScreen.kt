package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp
import com.example.tools.MarcoLogger
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import com.example.data.FirebaseAuthManager
import com.example.data.Language
import com.example.ui.MarcoViewModel
import com.example.ui.theme.MarcoCardSurface
import com.example.ui.theme.MarcoCyanPrimary
import com.example.ui.theme.MarcoDarkBackground
import com.example.ui.theme.MarcoEmeraldSuccess
import com.example.ui.theme.MarcoPinkAccent
import com.example.ui.theme.MarcoTextSecondary

@Composable
fun SettingsScreen(
    viewModel: MarcoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val isBackgroundActive by viewModel.isBackgroundActive.collectAsState()
    val isWakeWordActive by viewModel.isContinuousWakeWordActive.collectAsState()

    var speechRate by remember { mutableFloatStateOf(viewModel.textToSpeech.speechRate) }
    var pitch by remember { mutableFloatStateOf(viewModel.textToSpeech.pitch) }

    val permissionsToRequest = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "MARCO Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Voice, Language, Theme & Permissions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Theme & Visual Appearance Setting
        item {
            val currentThemeMode by viewModel.themeMode.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme & Visual Appearance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Switch interface theme (meets accessibility contrast guidelines)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.ThemeMode.entries.forEach { mode ->
                            val isSelected = currentThemeMode == mode
                            val buttonModifier = Modifier
                                .weight(1f)
                                .testTag("set_theme_${mode.name.lowercase()}")

                            if (isSelected) {
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = buttonModifier
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setThemeMode(mode) },
                                    modifier = buttonModifier
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Firebase Auth & Database Persistence Section
        item {
            val currentUser by FirebaseAuthManager.instance.currentUser.collectAsState()
            var isSigningIn by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Account & Cloud Persistence",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Firebase Auth & Firestore Data Sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentUser?.displayName ?: "Google Signed-In User",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentUser?.email ?: "uid: ${currentUser?.uid}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "✓ Firestore Database Connected",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = { FirebaseAuthManager.instance.signOut() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    modifier = Modifier.testTag("firebase_sign_out_button")
                                ) {
                                    Text(
                                        text = "Sign Out",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sign in with Google / Firebase Auth to sync chat history, voice notes, and settings securely to Cloud Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isSigningIn = true
                                        FirebaseAuthManager.instance.signInWithDemoGoogleUser()
                                        isSigningIn = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("google_sign_in_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign In with Google", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isSigningIn = true
                                        FirebaseAuthManager.instance.signInAnonymously { _, _ ->
                                            isSigningIn = false
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("anonymous_auth_button")
                                ) {
                                    Text("Guest Auth", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Response Language Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Response Language",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MarcoCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Language.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.displayName, color = MaterialTheme.colorScheme.onSurface)
                            if (preferredLanguage == lang) {
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = MarcoCyanPrimary)
                                ) {
                                    Text("Active", color = androidx.compose.ui.graphics.Color.Black)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setPreferredLanguage(lang) },
                                    modifier = Modifier.testTag("set_lang_${lang.code}")
                                ) {
                                    Text("Select")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Lightweight Wake Word Detection Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local Lightweight Wake-Word Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MarcoCyanPrimary
                            )
                            Text(
                                text = "Always listening locally for 'Hey MARCO', 'MARCO', or 'JARVIS'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MarcoTextSecondary
                            )
                        }
                        Switch(
                            checked = isWakeWordActive,
                            onCheckedChange = { viewModel.setContinuousWakeWord(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MarcoEmeraldSuccess,
                                checkedTrackColor = MarcoEmeraldSuccess.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("wakeword_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var sensitivity by remember { mutableFloatStateOf(0.80f) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KWS Sensitivity (Battery Mode)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(sensitivity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MarcoCyanPrimary
                            )
                        }
                        Slider(
                            value = sensitivity,
                            onValueChange = {
                                sensitivity = it
                                viewModel.setKwsSensitivity(it)
                            },
                            valueRange = 0.3f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MarcoCyanPrimary,
                                activeTrackColor = MarcoCyanPrimary
                            ),
                            modifier = Modifier.testTag("kws_sensitivity_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Active Trigger Phrases:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• English: \"Hey MARCO\", \"MARCO\", \"JARVIS\", \"Hey JARVIS\"\n• Tamil: \"மார்கோ\", \"ஹே மார்கோ\"\n• Hindi: \"मार्को\", \"हे मार्को\", \"जार्विस\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MarcoTextSecondary
                    )
                }
            }
        }

        // Background Service Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Assistant Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MarcoCyanPrimary
                        )
                        Text(
                            text = "Listens for 'Hey MARCO' in foreground service",
                            style = MaterialTheme.typography.bodySmall,
                            color = MarcoTextSecondary
                        )
                    }
                    Switch(
                        checked = isBackgroundActive,
                        onCheckedChange = { viewModel.toggleBackgroundService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MarcoEmeraldSuccess,
                            checkedTrackColor = MarcoEmeraldSuccess.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("bg_service_switch")
                    )
                }
            }
        }

        // Voice Model Selection (Male vs Female)
        item {
            val currentGender by viewModel.voiceGender.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Voice Model Selection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MarcoCyanPrimary
                    )
                    Text(
                        text = "Select assistant voice persona model",
                        style = MaterialTheme.typography.bodySmall,
                        color = MarcoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    com.example.voice.VoiceGender.entries.forEach { gender ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gender.displayName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            if (currentGender == gender) {
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = MarcoCyanPrimary)
                                ) {
                                    Text("Active", color = androidx.compose.ui.graphics.Color.Black)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setVoiceGender(gender) },
                                    modifier = Modifier.testTag("select_voice_${gender.name}")
                                ) {
                                    Text("Select")
                                }
                            }
                        }
                    }
                }
            }
        }

        // TTS Speech Rate & Pitch
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Voice Parameters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MarcoCyanPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Speech Speed: ${String.format("%.1f", speechRate)}x", color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            viewModel.textToSpeech.speechRate = it
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = MarcoCyanPrimary, activeTrackColor = MarcoCyanPrimary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Voice Pitch: ${String.format("%.1f", pitch)}x", color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = pitch,
                        onValueChange = {
                            pitch = it
                            viewModel.textToSpeech.pitch = it
                        },
                        valueRange = 0.5f..1.8f,
                        colors = SliderDefaults.colors(thumbColor = MarcoCyanPrimary, activeTrackColor = MarcoCyanPrimary)
                    )
                }
            }
        }

        // Permission Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Android System Permissions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MarcoCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    val callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                    val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

                    PermissionStatusRow("Microphone (Voice)", micGranted)
                    PermissionStatusRow("Phone Calls", callGranted)
                    PermissionStatusRow("Contacts", contactsGranted)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                        colors = ButtonDefaults.buttonColors(containerColor = MarcoCyanPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_permissions_button")
                    ) {
                        Text("Grant Required Permissions", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Centralized Timber Execution Logs Debugger Card
        item {
            var logText by remember { mutableStateOf(MarcoLogger.getLogs(context)) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = MarcoCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Timber Execution Error Logs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MarcoCyanPrimary
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { logText = MarcoLogger.getLogs(context) },
                                modifier = Modifier.testTag("refresh_logs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Logs",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    MarcoLogger.clearLogs(context)
                                    logText = MarcoLogger.getLogs(context)
                                },
                                modifier = Modifier.testTag("clear_logs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Logs",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Text(
                        text = "Persisted local errors from intent handlers and voice commands (marco_intent_errors.log):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MarcoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            item {
                                Text(
                                    text = logText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatusRow(label: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = if (isGranted) "Granted ✓" else "Not Granted ✗",
            color = if (isGranted) MarcoEmeraldSuccess else MarcoPinkAccent,
            fontWeight = FontWeight.Bold
        )
    }
}
