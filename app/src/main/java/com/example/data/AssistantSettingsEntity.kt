package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing user settings for Raj Assistant.
 */
@Entity(tableName = "assistant_settings")
data class AssistantSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val wakeWord: String = "Hey Raj",
    val languageCode: String = "auto", // "auto", "en", "hi"
    val speechPitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val isContinuousListening: Boolean = true,
    val isTtsEnabled: Boolean = true,
    val wakeWordEngine: String = "porcupine", // "porcupine" or "builtin"
    val porcupineAccessKey: String = "",
    val speechEngine: String = "vosk", // "vosk" or "builtin"
    val isGestureEnabled: Boolean = true,
    val gestureSwipeLeftAction: String = "PREVIOUS_TRACK",
    val gestureSwipeRightAction: String = "NEXT_TRACK",
    val gestureDoubleTapAction: String = "PLAY_PAUSE",
    val gestureDrawCAction: String = "OPEN_CAMERA",
    val gestureDrawVAction: String = "TOGGLE_FLASHLIGHT"
)
