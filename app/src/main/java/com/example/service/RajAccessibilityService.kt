package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.engine.ActionProcessor
import com.example.engine.FlashlightManager
import com.example.engine.GemmaNanoOfflineEngine
import com.example.engine.IntentLauncherManager
import com.example.engine.MusicControllerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * System Accessibility Service for Raj Assistant.
 * Enables system-wide accessibility gestures, screen-on gesture triggers, and global shortcut handling.
 */
class RajAccessibilityService : AccessibilityService() {

    private lateinit var flashlightManager: FlashlightManager
    private lateinit var musicController: MusicControllerManager
    private lateinit var intentLauncher: IntentLauncherManager
    private lateinit var gemmaEngine: GemmaNanoOfflineEngine
    private lateinit var actionProcessor: ActionProcessor

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RajAccessibilityService created")

        flashlightManager = FlashlightManager(applicationContext)
        musicController = MusicControllerManager(applicationContext)
        intentLauncher = IntentLauncherManager(applicationContext)
        gemmaEngine = GemmaNanoOfflineEngine(applicationContext)
        actionProcessor = ActionProcessor(
            applicationContext,
            flashlightManager,
            musicController,
            intentLauncher,
            gemmaEngine
        )

        _isAccessibilityActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing for screen accessibility state changes
    }

    override fun onInterrupt() {
        Log.d(TAG, "RajAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isAccessibilityActive.value = false
        Log.d(TAG, "RajAccessibilityService destroyed")
    }

    fun executeGestureAction(actionCode: String): String {
        Log.d(TAG, "Executing accessibility gesture action: $actionCode")
        return when (actionCode) {
            "PREVIOUS_TRACK" -> musicController.previousSong()
            "NEXT_TRACK" -> musicController.nextSong()
            "PLAY_PAUSE" -> musicController.playMusic()
            "OPEN_CAMERA" -> intentLauncher.openCamera()
            "TOGGLE_FLASHLIGHT" -> flashlightManager.toggleFlashlight()
            "OPEN_SETTINGS" -> intentLauncher.openAppByName("settings")
            "VOLUME_UP" -> musicController.volumeUp()
            "VOLUME_DOWN" -> musicController.volumeDown()
            else -> "Action executed"
        }
    }

    companion object {
        private const val TAG = "RajAccessibilityService"

        private val _isAccessibilityActive = MutableStateFlow(false)
        val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val prefString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            return prefString.contains(context.packageName)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
