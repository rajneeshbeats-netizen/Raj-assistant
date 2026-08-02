package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

/**
 * Manages offline Android SpeechRecognizer listening, RMS audio volume levels, and results.
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _soundLevelRms = MutableStateFlow(0f)
    val soundLevelRms: StateFlow<Float> = _soundLevelRms.asStateFlow()

    private var isListeningActive = false

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(this)
            } catch (e: Exception) {
                Log.e("SpeechManager", "Failed to create SpeechRecognizer", e)
            }
        } else {
            Log.w("SpeechManager", "Speech recognition not available on device")
        }
    }

    fun startListening(languageCode: String = "auto") {
        if (speechRecognizer == null) {
            initializeRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            // Explicitly prefer offline recognition for privacy and offline usage
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)

            when (languageCode) {
                "hi" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                "en" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                else -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN,hi-IN")
                }
            }
        }

        try {
            _partialText.value = ""
            _assistantState.value = AssistantState.LISTENING
            isListeningActive = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error starting listening", e)
            _assistantState.value = AssistantState.ERROR
            onError("Unable to access microphone recorder.")
        }
    }

    fun stopListening() {
        try {
            isListeningActive = false
            speechRecognizer?.stopListening()
            _assistantState.value = AssistantState.IDLE
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error stopping listening", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error destroying speech recognizer", e)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _assistantState.value = AssistantState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _assistantState.value = AssistantState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        // RMS sound level indicator (normalize around 0f..10f range for animations)
        _soundLevelRms.value = (rmsdB.coerceIn(-2f, 10f) + 2f) / 12f
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _assistantState.value = AssistantState.PROCESSING
    }

    override fun onError(error: Int) {
        _assistantState.value = AssistantState.IDLE
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try saying 'Hey Raj' or 'Turn on flashlight'."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap mic to try again."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK -> "Network issue. Operating in offline mode."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy. Restarting..."
            else -> "Speech recognition error code $error."
        }
        onError(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        _assistantState.value = AssistantState.PROCESSING
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognizedText = matches?.firstOrNull() ?: ""
        if (recognizedText.isNotEmpty()) {
            _partialText.value = recognizedText
            onResult(recognizedText)
        } else {
            _assistantState.value = AssistantState.IDLE
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull() ?: ""
        if (partial.isNotEmpty()) {
            _partialText.value = partial
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
