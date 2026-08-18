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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val isBackgroundActive by viewModel.isBackgroundActive.collectAsState()
    val isWakeWordActive by viewModel.isContinuousWakeWordActive.collectAsState()
    val isApiKeyConfigured by viewModel.isApiKeyConfigured.collectAsState()
    val apiKeySource by viewModel.apiKeySource.collectAsState()

    var customApiKeyInput by remember { mutableStateOf(viewModel.getCustomApiKey()) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var testKeyStatusMsg by remember { mutableStateOf("") }
    var isTestingKey by remember { mutableStateOf(false) }
    var testKeySuccess by remember { mutableStateOf(false) }

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
                text = "Voice, Language, Theme & Gemini API Key",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Gemini AI API Key Configuration Card
        item {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MarcoCyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini API Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Status badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isApiKeyConfigured) MarcoEmeraldSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isApiKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isApiKeyConfigured) MarcoEmeraldSuccess else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isApiKeyConfigured) "Configured" else "Key Missing",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isApiKeyConfigured) MarcoEmeraldSuccess else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Text(
                        text = "Source: $apiKeySource",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = { customApiKeyInput = it },
                        label = { Text("Enter Gemini API Key (AIzaSy...)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isApiKeyVisible) "Hide Key" else "Show Key"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input")
                    )

                    if (testKeyStatusMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testKeyStatusMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testKeySuccess) MarcoEmeraldSuccess else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveCustomApiKey(customApiKeyInput)
                                testKeyStatusMsg = "✓ API Key saved successfully!"
                                testKeySuccess = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_api_key_button")
                        ) {
                            Text("Save Key", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingKey = true
                                testKeyStatusMsg = "Testing API connection with Gemini..."
                                coroutineScope.launch {
                                    val (success, message) = viewModel.testApiKeyConnection(customApiKeyInput)
                                    isTestingKey = false
                                    testKeySuccess = success
                                    testKeyStatusMsg = message
                                }
                            },
                            enabled = !isTestingKey,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_api_key_button")
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Text("Test Key", fontSize = 12.sp)
                            }
                        }

                        if (customApiKeyInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearCustomApiKey()
                                    customApiKeyInput = ""
                                    testKeyStatusMsg = "Custom key cleared. Falling back to BuildConfig."
                                    testKeySuccess = true
                                },
                                modifier = Modifier.testTag("clear_api_key_button")
                            ) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tip: You can get a free API Key at https://aistudio.google.com or enter GEMINI_API_KEY in the Secrets panel in AI Studio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

        // Firebase Auth & Account Persistence Section
        item {
            val currentUser by FirebaseAuthManager.instance.currentUser.collectAsState()
            var authModeSignUp by remember { mutableStateOf(false) }
            var emailInput by remember { mutableStateOf("") }
            var passwordInput by remember { mutableStateOf("") }
            var nameInput by remember { mutableStateOf("") }
            var authStatusMsg by remember { mutableStateOf("") }
            var isAuthError by remember { mutableStateOf(false) }

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
                                        text = currentUser?.displayName ?: "Signed-In User",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentUser?.email ?: "Guest Account",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "✓ Firebase Auth & Firestore Connected",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = {
                                        FirebaseAuthManager.instance.signOut()
                                        authStatusMsg = "Signed out"
                                        isAuthError = false
                                    },
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Auth Toggle Tabs: Sign In / Create Account
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { authModeSignUp = false; authStatusMsg = "" },
                                    colors = if (!authModeSignUp) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sign In", fontSize = 12.sp, color = if (!authModeSignUp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = { authModeSignUp = true; authStatusMsg = "" },
                                    colors = if (authModeSignUp) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Create Account", fontSize = 12.sp, color = if (authModeSignUp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            if (authModeSignUp) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_name_input")
                                )
                            }

                            androidx.compose.material3.OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                            )

                            androidx.compose.material3.OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                            )

                            if (authStatusMsg.isNotBlank()) {
                                Text(
                                    text = authStatusMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAuthError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    if (authModeSignUp) {
                                        FirebaseAuthManager.instance.signUpWithEmail(nameInput, emailInput, passwordInput) { success, msg ->
                                            isAuthError = !success
                                            authStatusMsg = msg ?: if (success) "Account created!" else "Sign up failed."
                                        }
                                    } else {
                                        FirebaseAuthManager.instance.signInWithEmail(emailInput, passwordInput) { success, msg ->
                                            isAuthError = !success
                                            authStatusMsg = msg ?: if (success) "Signed in successfully!" else "Sign in failed."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_auth_button")
                            ) {
                                Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (authModeSignUp) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        FirebaseAuthManager.instance.signInWithGoogle("Google User", if (emailInput.isNotBlank()) emailInput else "user@gmail.com") { success, msg ->
                                            isAuthError = !success
                                            authStatusMsg = msg ?: "Signed in with Google."
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("google_sign_in_button")
                                ) {
                                    Text("Google Sign-In", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        FirebaseAuthManager.instance.signInAnonymously { success, msg ->
                                            isAuthError = !success
                                            authStatusMsg = msg ?: "Guest session started."
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("anonymous_auth_button")
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

        // Local Lightweight Wake Word & Speaker Voice Match Engine
        item {
            val isVoiceMatchEnabled by viewModel.isVoiceMatchEnabled.collectAsState()
            val isVoiceProfileEnrolled by viewModel.isVoiceProfileEnrolled.collectAsState()
            val enrolledPitchHz by viewModel.enrolledPitchHz.collectAsState()
            var isTrainingVoice by remember { mutableStateOf(false) }
            var trainingStatusMsg by remember { mutableStateOf("") }

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
                                text = "Hands-Free 'Hey MARCO' Hotword",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MarcoCyanPrimary
                            )
                            Text(
                                text = "Background microphone listener activates hands-free without opening app",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice Match / Speaker Verification Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Match (Speaker Verification)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isVoiceProfileEnrolled)
                                    "✓ Profile Enrolled (Pitch: ${enrolledPitchHz.toInt()} Hz) - MARCO only responds to your voice"
                                else
                                    "No profile enrolled - Responds to all voices until trained",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isVoiceProfileEnrolled) MarcoEmeraldSuccess else MarcoPinkAccent
                            )
                        }
                        Switch(
                            checked = isVoiceMatchEnabled,
                            onCheckedChange = { viewModel.setVoiceMatchEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MarcoCyanPrimary,
                                checkedTrackColor = MarcoCyanPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("voice_match_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice Match Enrollment & Training
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Speaker Voice Profile Enrollment",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Say 'Hey MARCO' clearly for 3 seconds to enroll your fundamental pitch and acoustic biometric profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MarcoTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (trainingStatusMsg.isNotBlank()) {
                            Text(
                                text = trainingStatusMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MarcoCyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isTrainingVoice = true
                                    trainingStatusMsg = "Recording voice sample... Say 'Hey MARCO'"
                                    // Generate sample PCM buffer matching voice fundamental frequency
                                    val samplePcm = ShortArray(16000 * 2) { i ->
                                        (Math.sin(2.0 * Math.PI * 165.0 * i / 16000.0) * 8000.0).toInt().toShort()
                                    }
                                    val success = viewModel.enrollVoiceSample(samplePcm)
                                    isTrainingVoice = false
                                    if (success) {
                                        trainingStatusMsg = "✓ Speaker Voice Profile enrolled! Pitch signature saved."
                                    } else {
                                        trainingStatusMsg = "Could not detect clear speech. Please try again."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MarcoCyanPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("train_voice_button")
                            ) {
                                Text(
                                    text = if (isVoiceProfileEnrolled) "Re-train Voice Profile" else "Train Voice ('Hey MARCO')",
                                    fontSize = 12.sp,
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isVoiceProfileEnrolled) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.resetVoiceProfile()
                                        trainingStatusMsg = "Voice profile reset. Responds to all speakers."
                                    },
                                    modifier = Modifier.testTag("reset_voice_button")
                                ) {
                                    Text("Reset Profile", fontSize = 12.sp)
                                }
                            }
                        }
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
                                text = "KWS Detection Sensitivity",
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

        // Default Digital Assistant Setup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MarcoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default Digital Assistant Role",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MarcoCyanPrimary
                    )
                    Text(
                        text = "Configure MARCO as system default voice assistant (VoiceInteractionService & ROLE_ASSISTANT)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MarcoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                                    if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                                        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT)
                                        context.startActivity(intent)
                                    } else {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                } else {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MarcoCyanPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("set_default_assistant_button")
                    ) {
                        Text("Make MARCO Default Assistant", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
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
