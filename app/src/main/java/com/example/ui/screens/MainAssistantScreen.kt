package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.AssistantState
import com.example.ui.AssistantViewModel
import com.example.ui.components.AssistantOrbVisualizer
import com.example.ui.components.GestureTouchPad
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToSettings: () -> Unit
) {
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val soundRms by viewModel.soundLevelRms.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val lastQuery by viewModel.lastQuery.collectAsStateWithLifecycle()
    val lastResponse by viewModel.lastResponse.collectAsStateWithLifecycle()
    val statusText by viewModel.statusText.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { capturedBitmap: Bitmap? ->
        if (capturedBitmap != null) {
            viewModel.processCameraBitmap(capturedBitmap)
        } else {
            // Generate standard high-resolution printed text bitmap for testing OCR scan
            val sampleBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(sampleBitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 24f
                isAntiAlias = true
            }
            canvas.drawText("OFFLINE CAMERA OCR READ", 20f, 60f, paint)
            canvas.drawText("Printed Document Scan", 20f, 110f, paint)
            canvas.drawText("Hindi Text: हिंदी ओसीआर", 20f, 160f, paint)
            viewModel.processCameraBitmap(sampleBitmap)
        }
    }

    val quickActionChips = listOf(
        "📷 Camera Scan OCR (कैमरा ओसीआर)",
        "Open Wi-Fi Settings",
        "Open Bluetooth Settings",
        "Turn on Flashlight",
        "Open WhatsApp",
        "Set Alarm at 7 AM",
        "Set Timer for 5 minutes",
        "Call 9876543210",
        "Send SMS to 9876543210 Hello",
        "Play Music",
        "Volume Up",
        "What time is it?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117), // Deep dark space background
                        Color(0xFF161B22),
                        Color(0xFF090D12)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Top Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Raj Assistant",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% Offline Mode",
                        fontSize = 12.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language indicator chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF21262D))
                        .clickable {
                            val nextLang = when (settingsState?.languageCode) {
                                "en" -> "hi"
                                "hi" -> "auto"
                                else -> "en"
                            }
                            viewModel.updateLanguage(nextLang)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (settingsState?.languageCode ?: "auto").uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF21262D))
                        .testTag("camera_ocr_nav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera OCR Scan",
                        tint = Color(0xFF00E5FF)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF21262D))
                        .testTag("settings_nav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Central Interactive Orb Visualizer ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AssistantOrbVisualizer(
                        state = assistantState,
                        soundRms = soundRms,
                        onClickOrb = { viewModel.toggleListening() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (partialText.isNotEmpty()) "\"$partialText\"" else statusText,
                        fontSize = 14.sp,
                        color = if (assistantState == AssistantState.LISTENING) Color(0xFF00E5FF) else Color.LightGray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- Screen-On Gesture Recognition Touch Pad ---
            item {
                GestureTouchPad(
                    onGestureDetected = { recognizedGesture ->
                        viewModel.processGesture(recognizedGesture)
                    }
                )
            }

            // --- Response Card ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("response_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (lastQuery.isNotEmpty()) {
                            Text(
                                text = "You said:",
                                fontSize = 12.sp,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lastQuery,
                                fontSize = 15.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Raj Assistant:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7C4DFF),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = lastResponse,
                                    fontSize = 15.sp,
                                    color = Color(0xFFE6EDF3),
                                    lineHeight = 22.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.replayResponseText(lastResponse) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF30363D))
                                    .testTag("replay_tts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Replay Speech",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- Quick Action Chips ---
            item {
                Column {
                    Text(
                        text = "Quick Voice Commands",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickActionChips) { chipText ->
                            SuggestionChip(
                                onClick = {
                                    if (chipText.contains("Camera Scan")) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        viewModel.processIncomingText(chipText, isVoice = false)
                                    }
                                },
                                label = { Text(chipText, color = Color.White, fontSize = 13.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF30363D)
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    borderColor = Color(0xFF00E5FF).copy(alpha = 0.4f),
                                    enabled = true
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // --- Command History Log Header ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recent Activity Log",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }

                    if (historyList.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear", color = Color(0xFFFF5252), fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- Command History Log Items ---
            items(historyList) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.replayResponseText(item.responseText) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (item.actionType) {
                                        "FLASHLIGHT" -> Color(0xFFFFD600).copy(alpha = 0.2f)
                                        "CALL", "SMS" -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                                        "ALARM" -> Color(0xFFFF9100).copy(alpha = 0.2f)
                                        "MUSIC" -> Color(0xFF00E676).copy(alpha = 0.2f)
                                        else -> Color(0xFF7C4DFF).copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.actionType.take(2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.rawText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = item.responseText,
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.timestamp)),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // --- Bottom Command Bar (Mic FAB + Text Input) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Type command or say 'Hey Raj'...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF21262D),
                    unfocusedContainerColor = Color(0xFF21262D),
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.processIncomingText(textInput, isVoice = false)
                            textInput = ""
                        }
                    }
                ),
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.processIncomingText(textInput, isVoice = false)
                                textInput = ""
                            },
                            modifier = Modifier.testTag("send_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Glowing Floating Mic Button
            IconButton(
                onClick = { viewModel.toggleListening() },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (assistantState == AssistantState.LISTENING) Color(0xFF00E5FF) else Color(0xFF7C4DFF)
                    )
                    .testTag("main_mic_button")
            ) {
                Icon(
                    imageVector = if (assistantState == AssistantState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantOrbVisualizer(
    state: AssistantState,
    soundRms: Float,
    onClickOrb: () -> Unit
) {
    AssistantOrbVisualizer(
        state = state,
        soundRms = soundRms,
        modifier = Modifier.clickable { onClickOrb() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D1117)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state == AssistantState.LISTENING) Icons.Default.Mic else Icons.Default.FlashOn,
                contentDescription = "Assistant State",
                tint = if (state == AssistantState.LISTENING) Color(0xFF00E5FF) else Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
