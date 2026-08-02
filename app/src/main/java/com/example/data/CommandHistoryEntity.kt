package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a logged voice or text command executed by Raj Assistant.
 */
@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String,
    val responseText: String,
    val actionType: String, // e.g. "FLASHLIGHT", "APP_LAUNCH", "CALL", "SMS", "ALARM", "MUSIC", "GENERAL"
    val isSuccess: Boolean = true,
    val isVoiceInput: Boolean = true
)
