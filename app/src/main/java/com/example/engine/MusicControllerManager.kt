package com.example.engine

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Utility to control music playback (Play/Pause, Next, Previous, Volume) via System Media intents.
 */
class MusicControllerManager(private val context: Context) {

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun playPauseMusic(): String {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Toggled music playback.")
    }

    fun playMusic(): String {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, "Playing music.")
    }

    fun pauseMusic(): String {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, "Music paused.")
    }

    fun nextSong(): String {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, "Skipped to next track.")
    }

    fun previousSong(): String {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Returned to previous track.")
    }

    fun volumeUp(): String {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
        return "Volume increased."
    }

    fun volumeDown(): String {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        return "Volume decreased."
    }

    private fun sendMediaKeyEvent(keyCode: Int, successMessage: String): String {
        return try {
            val eventTime = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)

            successMessage
        } catch (e: Exception) {
            // Alternative fallback: Broadcast intent
            try {
                val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                }
                context.sendOrderedBroadcast(mediaIntent, null)
                successMessage
            } catch (err: Exception) {
                "Unable to control music player."
            }
        }
    }
}
