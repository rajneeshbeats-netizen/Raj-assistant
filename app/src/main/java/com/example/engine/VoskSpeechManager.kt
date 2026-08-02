package com.example.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException

/**
 * Manages Vosk offline speech-to-text engine for local voice recognition without network access.
 */
class VoskSpeechManager(
    private val context: Context,
    private val onResultRecognized: (String) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _statusMessage = MutableStateFlow("Vosk Engine: Standby")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    /**
     * Unpack and load Vosk model from assets or custom directory.
     */
    fun initModel(modelPathInAssets: String = "model-en-us") {
        _statusMessage.value = "Unpacking Vosk offline model..."
        StorageService.unpack(
            context,
            modelPathInAssets,
            "model",
            { loadedModel: Model ->
                model = loadedModel
                _isModelLoaded.value = true
                _statusMessage.value = "Vosk Offline Model Loaded Ready"
                Log.d("VoskSpeechManager", "Vosk model loaded successfully from $modelPathInAssets")
            },
            { exception: IOException ->
                Log.e("VoskSpeechManager", "Failed to unpack Vosk model", exception)
                _isModelLoaded.value = false
                _statusMessage.value = "Vosk Model Not Found in Assets (Using Native Speech Fallback)"
                onErrorOccurred("Vosk model assets unavailable.")
            }
        )
    }

    /**
     * Initialize with an existing loaded Model object or external file path.
     */
    fun initWithModelPath(externalModelDir: File) {
        try {
            if (externalModelDir.exists() && externalModelDir.isDirectory) {
                model = Model(externalModelDir.absolutePath)
                _isModelLoaded.value = true
                _statusMessage.value = "Vosk External Model Loaded"
            }
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Error loading model path", e)
            _isModelLoaded.value = false
        }
    }

    fun startListening() {
        val currentModel = model
        if (currentModel == null || !_isModelLoaded.value) {
            _statusMessage.value = "Vosk Model not loaded yet."
            onErrorOccurred("Vosk model not ready.")
            return
        }

        try {
            speechService?.stop()
            val recognizer = Recognizer(currentModel, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            _isListening.value = true
            _statusMessage.value = "Vosk Listening..."
            _partialText.value = ""
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Failed to start Vosk speech service", e)
            _isListening.value = false
            _statusMessage.value = "Vosk Speech Service Error"
            onErrorOccurred("Vosk initialization error: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechService?.stop()
            speechService = null
            _isListening.value = false
            _statusMessage.value = "Vosk Stopped"
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Error stopping Vosk", e)
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        if (!hypothesis.isNullOrBlank()) {
            val parsedText = parseVoskJson(hypothesis, "partial")
            if (parsedText.isNotEmpty()) {
                _partialText.value = parsedText
            }
        }
    }

    override fun onResult(hypothesis: String?) {
        if (!hypothesis.isNullOrBlank()) {
            val text = parseVoskJson(hypothesis, "text")
            if (text.isNotEmpty()) {
                _partialText.value = text
                onResultRecognized(text)
            }
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (!hypothesis.isNullOrBlank()) {
            val text = parseVoskJson(hypothesis, "text")
            if (text.isNotEmpty()) {
                onResultRecognized(text)
            }
        }
        _isListening.value = false
    }

    override fun onError(exception: Exception?) {
        _isListening.value = false
        val msg = exception?.message ?: "Unknown Vosk Error"
        _statusMessage.value = "Vosk Error: $msg"
        onErrorOccurred(msg)
    }

    override fun onTimeout() {
        _isListening.value = false
        _statusMessage.value = "Vosk Listening Timeout"
    }

    fun destroy() {
        try {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
            model?.close()
            model = null
            _isListening.value = false
            _isModelLoaded.value = false
        } catch (e: Exception) {
            Log.e("VoskSpeechManager", "Error destroying Vosk manager", e)
        }
    }

    private fun parseVoskJson(jsonStr: String, key: String): String {
        return try {
            val jsonObj = JSONObject(jsonStr)
            jsonObj.optString(key, "").trim()
        } catch (e: Exception) {
            ""
        }
    }
}
