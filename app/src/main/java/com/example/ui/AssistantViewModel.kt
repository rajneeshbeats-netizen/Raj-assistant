package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AssistantRepository
import com.example.data.AssistantSettingsEntity
import com.example.data.CommandHistoryEntity
import com.example.data.CustomShortcutEntity
import com.example.data.RajDatabase
import com.example.engine.ActionProcessor
import com.example.engine.AssistantState
import android.graphics.Bitmap
import com.example.engine.CameraOcrEngine
import com.example.engine.FlashlightManager
import com.example.engine.GemmaNanoOfflineEngine
import com.example.engine.IntentLauncherManager
import com.example.engine.MusicControllerManager
import com.example.engine.PorcupineWakeWordManager
import com.example.engine.RecognizedGesture
import com.example.engine.SpeechRecognitionManager
import com.example.engine.TextToSpeechManager
import com.example.engine.VoskSpeechManager
import com.example.service.AssistantBackgroundService
import com.example.service.FloatingBubbleService
import com.example.service.RajAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionState(
    val hasAudioPermission: Boolean = false,
    val hasCallPermission: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val hasCameraPermission: Boolean = false
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RajDatabase.getInstance(application)
    private val repository = AssistantRepository(db)

    val flashlightManager = FlashlightManager(application)
    val musicController = MusicControllerManager(application)
    val intentLauncher = IntentLauncherManager(application)
    val ttsManager = TextToSpeechManager(application)
    val gemmaEngine = GemmaNanoOfflineEngine(application)
    val cameraOcrEngine = CameraOcrEngine(application)
    val actionProcessor = ActionProcessor(application, flashlightManager, musicController, intentLauncher, gemmaEngine)

    val porcupineManager = PorcupineWakeWordManager(application) {
        // Triggered when Porcupine detects the wake word!
        toggleListening()
    }

    val voskManager = VoskSpeechManager(
        context = application,
        onResultRecognized = { recognizedText ->
            processIncomingText(recognizedText, isVoice = true)
        },
        onErrorOccurred = { error ->
            _statusText.value = "Vosk STT Notice: $error"
        }
    )

    val historyList: StateFlow<List<CommandHistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcutsList: StateFlow<List<CustomShortcutEntity>> = repository.shortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsState: StateFlow<AssistantSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _lastQuery = MutableStateFlow("")
    val lastQuery: StateFlow<String> = _lastQuery.asStateFlow()

    private val _lastResponse = MutableStateFlow("Tap the mic button or say 'Hey Raj' / 'Porcupine' to start!")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _statusText = MutableStateFlow("Ready - Porcupine & Vosk Offline Engine Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    val isBgServiceRunning: StateFlow<Boolean> = AssistantBackgroundService.isServiceRunning
    val bgServiceStatus: StateFlow<String> = AssistantBackgroundService.serviceStatusText
    val isFloatingBubbleActive: StateFlow<Boolean> = FloatingBubbleService.isBubbleActive
    val isAccessibilityActive: StateFlow<Boolean> = RajAccessibilityService.isAccessibilityActive

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private var speechManager: SpeechRecognitionManager? = null

    val assistantState: StateFlow<AssistantState> get() = speechManager?.assistantState ?: MutableStateFlow(AssistantState.IDLE)
    val soundLevelRms: StateFlow<Float> get() = speechManager?.soundLevelRms ?: MutableStateFlow(0f)
    val partialText: StateFlow<String> get() = speechManager?.partialText ?: voskManager.partialText

    init {
        checkPermissions()
        initializeSpeechManager()
        voskManager.initModel()

        // Initialize default settings & Porcupine if key present
        viewModelScope.launch {
            repository.settings.collect { current ->
                if (current == null) {
                    repository.updateSettings(AssistantSettingsEntity())
                } else {
                    ttsManager.speechPitch = current.speechPitch
                    ttsManager.speechRate = current.speechRate

                    if (current.porcupineAccessKey.isNotBlank()) {
                        porcupineManager.initEngine(current.porcupineAccessKey, current.wakeWord)
                    }
                }
            }
        }
    }

    private fun initializeSpeechManager() {
        speechManager = SpeechRecognitionManager(
            context = getApplication(),
            onResult = { recognizedText ->
                processIncomingText(recognizedText, isVoice = true)
            },
            onError = { error ->
                _statusText.value = error
            }
        )
    }

    fun checkPermissions() {
        val context: Context = getApplication()
        _permissionState.value = PermissionState(
            hasAudioPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
            hasCallPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED,
            hasSmsPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED,
            hasCameraPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun toggleListening() {
        checkPermissions()
        if (!_permissionState.value.hasAudioPermission) {
            _statusText.value = "Microphone permission required! Please grant in settings."
            return
        }

        if (assistantState.value == AssistantState.LISTENING) {
            speechManager?.stopListening()
            _statusText.value = "Listening stopped."
        } else {
            ttsManager.stop()
            val lang = settingsState.value?.languageCode ?: "auto"
            speechManager?.startListening(lang)
            _statusText.value = "Listening... Speak your command or say 'Hey Raj'"
        }
    }

    fun processIncomingText(text: String, isVoice: Boolean = false) {
        if (text.isBlank()) return

        _lastQuery.value = text
        _statusText.value = "Processing command..."

        val wakeWord = settingsState.value?.wakeWord ?: "Hey Raj"
        val customShortcuts = shortcutsList.value
        val hasCallPerm = _permissionState.value.hasCallPermission

        val result = actionProcessor.processCommand(
            input = text,
            wakeWord = wakeWord,
            customShortcuts = customShortcuts,
            hasCallPermission = hasCallPerm
        )

        _lastResponse.value = result.responseMessage
        _statusText.value = if (result.isSuccess) "Action executed" else "Command not recognized"

        // Speak response if TTS is enabled in settings
        if (settingsState.value?.isTtsEnabled != false) {
            val prefLang = settingsState.value?.languageCode ?: "auto"
            ttsManager.speak(result.responseMessage, prefLang)
        }

        // Save history item to Room DB
        viewModelScope.launch {
            repository.logCommand(
                CommandHistoryEntity(
                    rawText = text,
                    responseText = result.responseMessage,
                    actionType = result.actionType,
                    isSuccess = result.isSuccess,
                    isVoiceInput = isVoice
                )
            )
        }
    }

    fun replayResponseText(text: String) {
        val prefLang = settingsState.value?.languageCode ?: "auto"
        ttsManager.speak(text, prefLang)
    }

    fun updatePorcupineAccessKey(key: String) {
        val current = settingsState.value ?: AssistantSettingsEntity()
        viewModelScope.launch {
            repository.updateSettings(current.copy(porcupineAccessKey = key))
            if (key.isNotBlank()) {
                porcupineManager.initEngine(key, current.wakeWord)
            }
        }
    }

    fun updateWakeWordEngine(engine: String) {
        val current = settingsState.value ?: AssistantSettingsEntity()
        viewModelScope.launch {
            repository.updateSettings(current.copy(wakeWordEngine = engine))
        }
    }

    fun updateSpeechEngine(engine: String) {
        val current = settingsState.value ?: AssistantSettingsEntity()
        viewModelScope.launch {
            repository.updateSettings(current.copy(speechEngine = engine))
        }
    }

    fun updateWakeWord(newWakeWord: String) {
        val current = settingsState.value ?: AssistantSettingsEntity()
        val formattedWord = newWakeWord.ifBlank { "Hey Raj" }
        viewModelScope.launch {
            repository.updateSettings(current.copy(wakeWord = formattedWord))
            if (current.porcupineAccessKey.isNotBlank()) {
                porcupineManager.initEngine(current.porcupineAccessKey, formattedWord)
            }
        }
    }

    fun updateLanguage(languageCode: String) {
        val current = settingsState.value ?: AssistantSettingsEntity()
        viewModelScope.launch {
            repository.updateSettings(current.copy(languageCode = languageCode))
        }
    }

    fun updatePitchAndRate(pitch: Float, rate: Float) {
        ttsManager.speechPitch = pitch
        ttsManager.speechRate = rate
        val current = settingsState.value ?: AssistantSettingsEntity()
        viewModelScope.launch {
            repository.updateSettings(current.copy(speechPitch = pitch, speechRate = rate))
        }
    }

    fun addCustomShortcut(shortcut: CustomShortcutEntity) {
        viewModelScope.launch {
            repository.saveShortcut(shortcut)
        }
    }

    fun deleteCustomShortcut(shortcut: CustomShortcutEntity) {
        viewModelScope.launch {
            repository.deleteShortcut(shortcut)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun startBackgroundService() {
        AssistantBackgroundService.startService(getApplication())
    }

    fun stopBackgroundService() {
        AssistantBackgroundService.stopService(getApplication())
    }

    fun toggleBackgroundService() {
        if (isBgServiceRunning.value) {
            stopBackgroundService()
        } else {
            startBackgroundService()
        }
    }

    fun hasOverlayPermission(): Boolean {
        return FloatingBubbleService.hasOverlayPermission(getApplication())
    }

    fun toggleFloatingBubble() {
        val app = getApplication<Application>()
        if (isFloatingBubbleActive.value) {
            FloatingBubbleService.stopService(app)
        } else {
            if (FloatingBubbleService.hasOverlayPermission(app)) {
                FloatingBubbleService.startService(app)
            } else {
                FloatingBubbleService.requestOverlayPermission(app)
            }
        }
    }

    fun processCameraBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _statusText.value = "Processing Camera OCR & Object Detection..."
            val result = cameraOcrEngine.analyzeImage(bitmap)

            val fullResponse = "${result.objectDescription}\n\n${result.extractedText}"
            _lastQuery.value = "[Camera Scan OCR & Vision]"
            _lastResponse.value = fullResponse
            _statusText.value = "Camera Scan Complete"

            // Voice out result
            ttsManager.speak(result.objectDescription, if (result.isHindiDetected) "hi" else "en")

            // Log entry into database
            repository.logCommand(
                CommandHistoryEntity(
                    rawText = "[Camera OCR Scan]",
                    responseText = fullResponse,
                    actionType = "CAMERA_OCR",
                    isSuccess = true,
                    isVoiceInput = false
                )
            )
        }
    }

    fun isAccessibilityEnabled(): Boolean {
        return RajAccessibilityService.isAccessibilityServiceEnabled(getApplication())
    }

    fun openAccessibilitySettings() {
        RajAccessibilityService.openAccessibilitySettings(getApplication())
    }

    fun processGesture(gesture: RecognizedGesture) {
        val currentSettings = settingsState.value ?: AssistantSettingsEntity()
        if (!currentSettings.isGestureEnabled) return

        val actionCode = when (gesture) {
            RecognizedGesture.SWIPE_LEFT -> currentSettings.gestureSwipeLeftAction
            RecognizedGesture.SWIPE_RIGHT -> currentSettings.gestureSwipeRightAction
            RecognizedGesture.DOUBLE_TAP -> currentSettings.gestureDoubleTapAction
            RecognizedGesture.DRAW_C -> currentSettings.gestureDrawCAction
            RecognizedGesture.DRAW_V -> currentSettings.gestureDrawVAction
            RecognizedGesture.UNKNOWN -> return
        }

        executeGestureAction(gesture.name, actionCode)
    }

    fun executeGestureAction(gestureName: String, actionCode: String) {
        val responseMsg = when (actionCode) {
            "PREVIOUS_TRACK" -> musicController.previousSong()
            "NEXT_TRACK" -> musicController.nextSong()
            "PLAY_PAUSE" -> musicController.playMusic()
            "OPEN_CAMERA" -> intentLauncher.openCamera()
            "TOGGLE_FLASHLIGHT" -> flashlightManager.toggleFlashlight()
            "OPEN_SETTINGS" -> intentLauncher.openAppByName("settings")
            "VOLUME_UP" -> musicController.volumeUp()
            "VOLUME_DOWN" -> musicController.volumeDown()
            else -> "Gesture action executed"
        }

        _lastQuery.value = "[Gesture: $gestureName]"
        _lastResponse.value = responseMsg
        _statusText.value = "Gesture Executed: $responseMsg"

        // Audio feedback
        ttsManager.speak(responseMsg, settingsState.value?.languageCode ?: "auto")

        viewModelScope.launch {
            repository.logCommand(
                CommandHistoryEntity(
                    rawText = "[Gesture $gestureName]",
                    responseText = responseMsg,
                    actionType = "GESTURE",
                    isSuccess = true,
                    isVoiceInput = false
                )
            )
        }
    }

    fun updateGestureSettings(
        isEnabled: Boolean,
        swipeLeft: String,
        swipeRight: String,
        doubleTap: String,
        drawC: String,
        drawV: String
    ) {
        val current = settingsState.value ?: AssistantSettingsEntity()
        viewModelScope.launch {
            repository.updateSettings(
                current.copy(
                    isGestureEnabled = isEnabled,
                    gestureSwipeLeftAction = swipeLeft,
                    gestureSwipeRightAction = swipeRight,
                    gestureDoubleTapAction = doubleTap,
                    gestureDrawCAction = drawC,
                    gestureDrawVAction = drawV
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        porcupineManager.destroy()
        voskManager.destroy()
        ttsManager.shutdown()
    }
}
