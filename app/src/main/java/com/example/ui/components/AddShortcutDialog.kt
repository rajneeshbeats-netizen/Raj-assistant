package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CustomShortcutEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShortcutDialog(
    onDismiss: () -> Unit,
    onConfirm: (CustomShortcutEntity) -> Unit
) {
    var triggerPhrase by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("OPEN_APP") }
    var actionTarget by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    val actionTypes = listOf(
        "OPEN_APP" to "Open Application",
        "TOGGLE_FLASHLIGHT" to "Toggle Flashlight",
        "SET_ALARM" to "Set Alarm Time",
        "PLAY_MUSIC" to "Play Music",
        "CUSTOM_TEXT" to "Custom Voice Response"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Voice Shortcut", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Define a voice phrase and the automated offline action Raj will perform.",
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = triggerPhrase,
                    onValueChange = { triggerPhrase = it },
                    label = { Text("Voice Trigger Phrase (e.g., Good Night)") },
                    modifier = Modifier.fillMaxWidth().testTag("shortcut_trigger_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = actionTypes.find { it.first == actionType }?.second ?: actionType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        actionTypes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.second) },
                                onClick = {
                                    actionType = item.first
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }

                if (actionType != "TOGGLE_FLASHLIGHT" && actionType != "PLAY_MUSIC") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actionTarget,
                        onValueChange = { actionTarget = it },
                        label = {
                            Text(
                                when (actionType) {
                                    "OPEN_APP" -> "App Name (e.g., WhatsApp, YouTube)"
                                    "SET_ALARM" -> "Time (e.g., 07:00 or 7:30 PM)"
                                    else -> "Response Text"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("shortcut_target_input"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (triggerPhrase.isNotBlank()) {
                        onConfirm(
                            CustomShortcutEntity(
                                triggerPhrase = triggerPhrase.trim(),
                                actionType = actionType,
                                actionTarget = actionTarget.trim(),
                                isEnabled = true
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_shortcut_button")
            ) {
                Text("Save Shortcut")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
