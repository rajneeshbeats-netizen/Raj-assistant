package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing user-customized voice command shortcuts.
 * E.g. "Good night" -> Turn off flashlight & set alarm for 6 AM
 */
@Entity(tableName = "custom_shortcuts")
data class CustomShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val triggerPhrase: String,
    val actionType: String, // e.g., "OPEN_APP", "TOGGLE_FLASHLIGHT", "SET_ALARM", "PLAY_MUSIC", "CUSTOM_TEXT"
    val actionTarget: String, // e.g. "com.whatsapp" or "07:00" or "Custom response"
    val isEnabled: Boolean = true
)
