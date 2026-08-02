package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AssistantRepository
import com.example.data.CommandHistoryEntity
import com.example.data.RajDatabase
import com.example.engine.ActionProcessor
import com.example.engine.FlashlightManager
import com.example.engine.GemmaNanoOfflineEngine
import com.example.engine.IntentLauncherManager
import com.example.engine.MusicControllerManager
import com.example.engine.PorcupineWakeWordManager
import com.example.engine.SpeechRecognitionManager
import com.example.engine.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Battery-optimized Foreground Background Service for Raj Assistant.
 *
 * Energy & CPU Optimization Strategy:
 * 1. Passive Wake-Word Phase: Low-CPU audio buffer monitoring for "Hey Raj".
 * 2. Active Command Phase: Starts STT listening ONLY AFTER wake word detection.
 * 3. Auto Power Saver: Immediately shuts down active voice recognizers post-command and returns to passive standby.
 */
class AssistantBackgroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: AssistantRepository
    private lateinit var flashlightManager: FlashlightManager
    private lateinit var musicController: MusicControllerManager
    private lateinit var intentLauncher: IntentLauncherManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var gemmaEngine: GemmaNanoOfflineEngine
    private lateinit var actionProcessor: ActionProcessor

    private var porcupineManager: PorcupineWakeWordManager? = null
    private var speechRecognitionManager: SpeechRecognitionManager? = null

    private var isListeningForCommand = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AssistantBackgroundService created.")

        val db = RajDatabase.getInstance(applicationContext)
        repository = AssistantRepository(db)
        flashlightManager = FlashlightManager(applicationContext)
        musicController = MusicControllerManager(applicationContext)
        intentLauncher = IntentLauncherManager(applicationContext)
        ttsManager = TextToSpeechManager(applicationContext)
        gemmaEngine = GemmaNanoOfflineEngine(applicationContext)
        actionProcessor = ActionProcessor(
            applicationContext,
            flashlightManager,
            musicController,
            intentLauncher,
            gemmaEngine
        )

        initWakeWordManager()
        initSpeechRecognizer()
        _isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithNotification()
                startPassiveWakeWordListening()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Raj Assistant Active")
            .setContentText("Listening for 'Hey Raj' (Battery-Optimized Mode)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Raj Assistant Background Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Battery optimized background wake word listening service"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Stage 1: Passive low-CPU wake-word listener.
     */
    private fun initWakeWordManager() {
        porcupineManager = PorcupineWakeWordManager(applicationContext) {
            onWakeWordTriggered()
        }
    }

    /**
     * Stage 2: Active STT listener activated ONLY after wake word is detected.
     */
    private fun initSpeechRecognizer() {
        speechRecognitionManager = SpeechRecognitionManager(
            context = applicationContext,
            onResult = { recognizedText ->
                handleRecognizedCommand(recognizedText)
            },
            onError = { error ->
                Log.e(TAG, "Background Speech Error: $error")
                // Revert to passive wake-word mode to save CPU
                revertToPassiveMode()
            }
        )
    }

    private fun startPassiveWakeWordListening() {
        _serviceStatusText.value = "Standby: Listening for 'Hey Raj'"
        isListeningForCommand = false
        speechRecognitionManager?.stopListening()

        // Check user settings for custom key
        serviceScope.launch {
            repository.settings.collect { settings ->
                val key = settings?.porcupineAccessKey ?: ""
                val wakeWord = settings?.wakeWord ?: "Hey Raj"
                if (key.isNotBlank()) {
                    porcupineManager?.initEngine(key, wakeWord)
                    porcupineManager?.startListening()
                }
            }
        }
    }

    private fun onWakeWordTriggered() {
        Log.d(TAG, "Wake word 'Hey Raj' triggered in background service!")
        _serviceStatusText.value = "Wake word detected! Listening for command..."
        isListeningForCommand = true

        // Stop passive wake-word detector to free mic resource
        porcupineManager?.stopListening()

        // Audio feedback cue
        ttsManager.speak("Listening", "en")

        // Start active command recognition
        speechRecognitionManager?.startListening("auto")
    }

    private fun handleRecognizedCommand(commandText: String) {
        if (commandText.isBlank()) {
            revertToPassiveMode()
            return
        }

        Log.d(TAG, "Background command recognized: '$commandText'")
        _serviceStatusText.value = "Processing: '$commandText'"

        serviceScope.launch {
            var wakeWord = "Hey Raj"
            var shortcuts = emptyList<com.example.data.CustomShortcutEntity>()

            repository.settings.collect { settings ->
                if (settings != null) wakeWord = settings.wakeWord
            }

            val result = actionProcessor.processCommand(
                input = commandText,
                wakeWord = wakeWord,
                customShortcuts = shortcuts,
                hasCallPermission = true
            )

            // Speak response via TTS
            ttsManager.speak(result.responseMessage, "auto")

            // Log command into Room database
            repository.logCommand(
                CommandHistoryEntity(
                    rawText = commandText,
                    responseText = result.responseMessage,
                    actionType = result.actionType,
                    isSuccess = result.isSuccess,
                    isVoiceInput = true
                )
            )

            // Instantly revert to passive mode to optimize battery & CPU
            revertToPassiveMode()
        }
    }

    private fun revertToPassiveMode() {
        isListeningForCommand = false
        speechRecognitionManager?.stopListening()
        _serviceStatusText.value = "Standby: Listening for 'Hey Raj'"
        porcupineManager?.startListening()
    }

    private fun stopForegroundService() {
        porcupineManager?.destroy()
        speechRecognitionManager?.destroy()
        ttsManager.shutdown()
        _isServiceRunning.value = false
        _serviceStatusText.value = "Service Stopped"
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        porcupineManager?.destroy()
        speechRecognitionManager?.destroy()
        ttsManager.shutdown()
        _isServiceRunning.value = false
        Log.d(TAG, "AssistantBackgroundService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AssistantBgService"
        const val CHANNEL_ID = "raj_assistant_bg_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.example.service.ACTION_START"
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _serviceStatusText = MutableStateFlow("Service Inactive")
        val serviceStatusText: StateFlow<String> = _serviceStatusText.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, AssistantBackgroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AssistantBackgroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
