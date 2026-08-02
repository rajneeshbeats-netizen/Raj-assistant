package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AssistantViewModel
import com.example.ui.components.AddShortcutDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val shortcutsList by viewModel.shortcutsList.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()

    var wakeWordInput by remember(settingsState?.wakeWord) {
        mutableStateOf(settingsState?.wakeWord ?: "Hey Raj")
    }

    var pitchVal by remember(settingsState?.speechPitch) {
        mutableFloatStateOf(settingsState?.speechPitch ?: 1.0f)
    }

    var rateVal by remember(settingsState?.speechRate) {
        mutableFloatStateOf(settingsState?.speechRate ?: 1.0f)
    }

    var showAddShortcutDialog by remember { mutableStateOf(false) }

    val porcupineStatus by viewModel.porcupineManager.statusMessage.collectAsStateWithLifecycle()
    val porcupineInit by viewModel.porcupineManager.isInitialized.collectAsStateWithLifecycle()
    val voskStatus by viewModel.voskManager.statusMessage.collectAsStateWithLifecycle()
    val voskLoaded by viewModel.voskManager.isModelLoaded.collectAsStateWithLifecycle()
    val gemmaStatus by viewModel.gemmaEngine.statusMessage.collectAsStateWithLifecycle()
    val gemmaRamFootprint by viewModel.gemmaEngine.modelMemoryFootprint.collectAsStateWithLifecycle()
    val isBgServiceRunning by viewModel.isBgServiceRunning.collectAsStateWithLifecycle()
    val bgServiceStatus by viewModel.bgServiceStatus.collectAsStateWithLifecycle()
    val isFloatingBubbleActive by viewModel.isFloatingBubbleActive.collectAsStateWithLifecycle()

    var porcupineKeyInput by remember(settingsState?.porcupineAccessKey) {
        mutableStateOf(settingsState?.porcupineAccessKey ?: "")
    }

    if (showAddShortcutDialog) {
        AddShortcutDialog(
            onDismiss = { showAddShortcutDialog = false },
            onConfirm = { newShortcut ->
                viewModel.addCustomShortcut(newShortcut)
                showAddShortcutDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF21262D))
                    .testTag("back_nav_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Assistant Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Custom Wake Word Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wake Word Configuration",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Set your custom trigger phrase to activate Raj Assistant.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = wakeWordInput,
                                onValueChange = { wakeWordInput = it },
                                label = { Text("Wake Word") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wake_word_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF30363D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { viewModel.updateWakeWord(wakeWordInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                modifier = Modifier.testTag("save_wake_word_button")
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }

            // --- Battery-Optimized Background Listening Service ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Background Wake-Word Service",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Low CPU & Battery Optimized",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }

                            Switch(
                                checked = isBgServiceRunning,
                                onCheckedChange = { viewModel.toggleBackgroundService() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFF00E676),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF21262D)
                                ),
                                modifier = Modifier.testTag("toggle_bg_service_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Runs in passive low-power standby mode listening for 'Hey Raj'. Speech recognition starts ONLY after 'Hey Raj' is detected to minimize power consumption.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBgServiceRunning) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFF21262D))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isBgServiceRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isBgServiceRunning) Color(0xFF00E676) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bgServiceStatus,
                                fontSize = 12.sp,
                                color = if (isBgServiceRunning) Color(0xFF00E676) else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- Floating Assistant Bubble (ChatGPT Style) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Floating Assistant Bubble",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "ChatGPT Style Overlay",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                            }

                            Switch(
                                checked = isFloatingBubbleActive,
                                onCheckedChange = { viewModel.toggleFloatingBubble() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFF00E5FF),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF21262D)
                                ),
                                modifier = Modifier.testTag("toggle_floating_bubble_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Displays a draggable floating chat bubble on top of all apps. Tap the bubble anytime to open the assistant instantly from any screen.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFloatingBubbleActive) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF21262D))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFloatingBubbleActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isFloatingBubbleActive) Color(0xFF00E5FF) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFloatingBubbleActive) "Floating Bubble Active (Overlay Granted)" else "Floating Bubble Inactive",
                                fontSize = 12.sp,
                                color = if (isFloatingBubbleActive) Color(0xFF00E5FF) else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- Porcupine Offline Wake Word Engine ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Porcupine Offline Wake Word Engine",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Porcupine by Picovoice provides zero-latency offline keyword detection.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Status badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (porcupineInit) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFFFF9100).copy(alpha = 0.15f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (porcupineInit) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (porcupineInit) Color(0xFF00E676) else Color(0xFFFF9100),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = porcupineStatus,
                                fontSize = 12.sp,
                                color = if (porcupineInit) Color(0xFF00E676) else Color(0xFFFF9100),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Picovoice Access Key:", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = porcupineKeyInput,
                                onValueChange = { porcupineKeyInput = it },
                                placeholder = { Text("Enter Porcupine Access Key...", color = Color.Gray, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("porcupine_key_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF30363D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.updatePorcupineAccessKey(porcupineKeyInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                modifier = Modifier.testTag("save_porcupine_key_button")
                            ) {
                                Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- Vosk Offline Speech Recognition Engine ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF7C4DFF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vosk Offline Speech Recognition",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vosk provides lightweight local acoustic models for completely offline continuous speech-to-text.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Status badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (voskLoaded) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = voskStatus,
                                fontSize = 12.sp,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- Gemma 3n Nano Offline AI Chat Engine ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color(0xFFFFD600)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemma 3n Nano Offline AI Engine",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gemma 3n Nano provides instant offline natural language AI chat in Hindi and English with low memory overhead.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFD600).copy(alpha = 0.15f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$gemmaStatus • $gemmaRamFootprint",
                                fontSize = 12.sp,
                                color = Color(0xFFFFD600),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- Voice Language Selection Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF00E676)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Speech Recognition & Language",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(
                            "auto" to "Auto-Detect (Hindi + English)",
                            "en" to "English (US / India)",
                            "hi" to "Hindi (हिंदी)"
                        ).forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (settingsState?.languageCode ?: "auto") == code,
                                    onClick = { viewModel.updateLanguage(code) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- Voice Speed & Pitch Controls ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color(0xFFFFD600)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Pitch & Speed Rate",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Speech Rate (Speed): ${String.format("%.1fx", rateVal)}",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Slider(
                            value = rateVal,
                            onValueChange = {
                                rateVal = it
                                viewModel.updatePitchAndRate(pitchVal, rateVal)
                            },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Voice Pitch: ${String.format("%.1fx", pitchVal)}",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Slider(
                            value = pitchVal,
                            onValueChange = {
                                pitchVal = it
                                viewModel.updatePitchAndRate(pitchVal, rateVal)
                            },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF7C4DFF),
                                activeTrackColor = Color(0xFF7C4DFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.replayResponseText("Hello! I am Raj, your voice assistant.")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Voice Audio Sample", color = Color(0xFF00E5FF))
                        }
                    }
                }
            }

            // --- Gesture Control & Accessibility Service Card ---
            item {
                val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsStateWithLifecycle()
                val gestureEnabled = settingsState?.isGestureEnabled ?: true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Gesture Control & Customization",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Screen-On & Accessibility Gestures",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                            }

                            Switch(
                                checked = gestureEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateGestureSettings(
                                        isEnabled = isChecked,
                                        swipeLeft = settingsState?.gestureSwipeLeftAction ?: "PREVIOUS_TRACK",
                                        swipeRight = settingsState?.gestureSwipeRightAction ?: "NEXT_TRACK",
                                        doubleTap = settingsState?.gestureDoubleTapAction ?: "PLAY_PAUSE",
                                        drawC = settingsState?.gestureDrawCAction ?: "OPEN_CAMERA",
                                        drawV = settingsState?.gestureDrawVAction ?: "TOGGLE_FLASHLIGHT"
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFF00E5FF),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF21262D)
                                ),
                                modifier = Modifier.testTag("toggle_gesture_control_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Control music, camera, flashlight, and settings with touch gestures on screen or system-wide using the Accessibility Service.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Accessibility Service Status & Activation Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAccessibilityActive) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFF21262D))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accessibility Service Status",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAccessibilityActive) "Active (System-Wide Gestures Enabled)" else "Disabled (Tap to enable system-wide gestures)",
                                    fontSize = 11.sp,
                                    color = if (isAccessibilityActive) Color(0xFF00E676) else Color.Gray
                                )
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAccessibilityActive) Color(0xFF30363D) else Color(0xFF7C4DFF)
                                ),
                                modifier = Modifier.testTag("open_accessibility_settings_button")
                            ) {
                                Text(if (isAccessibilityActive) "Configured" else "Enable", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gesture Mappings Summary / Customization List
                        Text(
                            text = "Gesture Mappings (Customization):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val currentSwipeLeft = settingsState?.gestureSwipeLeftAction ?: "PREVIOUS_TRACK"
                        val currentSwipeRight = settingsState?.gestureSwipeRightAction ?: "NEXT_TRACK"
                        val currentDoubleTap = settingsState?.gestureDoubleTapAction ?: "PLAY_PAUSE"
                        val currentDrawC = settingsState?.gestureDrawCAction ?: "OPEN_CAMERA"
                        val currentDrawV = settingsState?.gestureDrawVAction ?: "TOGGLE_FLASHLIGHT"

                        val actionOptions = listOf(
                            "PREVIOUS_TRACK" to "Previous Track ⏮️",
                            "NEXT_TRACK" to "Next Track ⏭️",
                            "PLAY_PAUSE" to "Play / Pause ⏯️",
                            "OPEN_CAMERA" to "Open Camera 📷",
                            "TOGGLE_FLASHLIGHT" to "Toggle Flashlight 🔦",
                            "OPEN_SETTINGS" to "Open Settings ⚙️",
                            "VOLUME_UP" to "Volume Up 🔊",
                            "VOLUME_DOWN" to "Volume Down 🔉"
                        )

                        // Function to update a single gesture mapping
                        fun updateSingleGesture(gestureKey: String, newAction: String) {
                            val sl = if (gestureKey == "SL") newAction else currentSwipeLeft
                            val sr = if (gestureKey == "SR") newAction else currentSwipeRight
                            val dt = if (gestureKey == "DT") newAction else currentDoubleTap
                            val dc = if (gestureKey == "DC") newAction else currentDrawC
                            val dv = if (gestureKey == "DV") newAction else currentDrawV
                            viewModel.updateGestureSettings(gestureEnabled, sl, sr, dt, dc, dv)
                        }

                        GestureMappingRow("👈 Swipe Left", "SL", currentSwipeLeft, actionOptions) { key, act -> updateSingleGesture(key, act) }
                        GestureMappingRow("👉 Swipe Right", "SR", currentSwipeRight, actionOptions) { key, act -> updateSingleGesture(key, act) }
                        GestureMappingRow("👆👆 Double Tap", "DT", currentDoubleTap, actionOptions) { key, act -> updateSingleGesture(key, act) }
                        GestureMappingRow("🔤 Draw 'C'", "DC", currentDrawC, actionOptions) { key, act -> updateSingleGesture(key, act) }
                        GestureMappingRow("🔤 Draw 'V'", "DV", currentDrawV, actionOptions) { key, act -> updateSingleGesture(key, act) }
                    }
                }
            }

            // --- Custom Command Shortcuts Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Custom Voice Shortcuts",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            IconButton(
                                onClick = { showAddShortcutDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C4DFF))
                                    .testTag("add_shortcut_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Shortcut",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (shortcutsList.isEmpty()) {
                            Text(
                                text = "No custom shortcuts defined yet. Tap '+' to create custom triggers like 'Night mode'.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                shortcutsList.forEach { shortcut ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF21262D))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "\"${shortcut.triggerPhrase}\"",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E5FF)
                                            )
                                            Text(
                                                text = "${shortcut.actionType}: ${shortcut.actionTarget}",
                                                fontSize = 12.sp,
                                                color = Color.LightGray
                                            )
                                        }

                                        IconButton(onClick = { viewModel.deleteCustomShortcut(shortcut) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFFF5252)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Device System Permissions Status ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System Permissions Check",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionRow("Microphone (Voice Recognition)", permissionState.hasAudioPermission)
                        PermissionRow("Phone Calls (Direct Dial)", permissionState.hasCallPermission)
                        PermissionRow("SMS Messages (Send Text)", permissionState.hasSmsPermission)
                        PermissionRow("Camera & Flashlight", permissionState.hasCameraPermission)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_permissions_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Text("Grant / Request Missing Permissions", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open App System Settings Page", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF00E676) else Color(0xFFFF9100),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isGranted) "Granted" else "Missing",
                color = if (isGranted) Color(0xFF00E676) else Color(0xFFFF9100),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GestureMappingRow(
    gestureLabel: String,
    gestureKey: String,
    currentAction: String,
    options: List<Pair<String, String>>,
    onActionSelected: (String, String) -> Unit
) {
    val currentLabel = options.firstOrNull { it.first == currentAction }?.second ?: currentAction

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = gestureLabel,
            fontSize = 13.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.Medium
        )

        // Interactive chip that cycles through action options on tap
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF21262D))
                .clickable {
                    val currentIndex = options.indexOfFirst { it.first == currentAction }
                    val nextIndex = if (currentIndex == -1 || currentIndex == options.lastIndex) 0 else currentIndex + 1
                    onActionSelected(gestureKey, options[nextIndex].first)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = currentLabel,
                fontSize = 12.sp,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
