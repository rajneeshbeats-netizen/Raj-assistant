package com.example.engine

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Porcupine offline wake word engine for instant keyword detection ("Porcupine", "Jarvis", etc.).
 */
class PorcupineWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) : PorcupineManagerCallback {

    private var porcupineManager: PorcupineManager? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _statusMessage = MutableStateFlow("Porcupine Wake Word: Standby")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Initialize Porcupine Engine using Picovoice AccessKey.
     * If key is empty or invalid, falls back gracefully.
     */
    fun initEngine(accessKey: String, keywordName: String = "PORCUPINE") {
        if (accessKey.isBlank()) {
            _statusMessage.value = "Porcupine: AccessKey required in Settings."
            _isInitialized.value = false
            return
        }

        try {
            porcupineManager?.delete()
            
            // Map keyword string to Porcupine built-in keyword if available
            val builtinKeyword = when (keywordName.uppercase()) {
                "JARVIS" -> Porcupine.BuiltInKeyword.JARVIS
                "ALEXA" -> Porcupine.BuiltInKeyword.ALEXA
                "COMPUTER" -> Porcupine.BuiltInKeyword.COMPUTER
                "BUMBLEBEE" -> Porcupine.BuiltInKeyword.BUMBLEBEE
                "GRAPEFRUIT" -> Porcupine.BuiltInKeyword.GRAPEFRUIT
                "GRASSHOPPER" -> Porcupine.BuiltInKeyword.GRASSHOPPER
                "PICOVOICE" -> Porcupine.BuiltInKeyword.PICOVOICE
                else -> Porcupine.BuiltInKeyword.PORCUPINE
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(builtinKeyword)
                .setSensitivity(0.7f)
                .build(context, this)

            _isInitialized.value = true
            _statusMessage.value = "Porcupine Engine Ready ('${builtinKeyword.name}')"
            Log.d("PorcupineManager", "Porcupine initialized successfully with $keywordName")
        } catch (e: PorcupineException) {
            Log.e("PorcupineManager", "PorcupineException during init", e)
            _isInitialized.value = false
            _statusMessage.value = "Porcupine Error: ${e.message}"
        } catch (e: Exception) {
            Log.e("PorcupineManager", "Unexpected error init Porcupine", e)
            _isInitialized.value = false
            _statusMessage.value = "Porcupine Initialization Failed."
        }
    }

    fun startListening() {
        if (porcupineManager != null && _isInitialized.value) {
            try {
                porcupineManager?.start()
                _isListening.value = true
                _statusMessage.value = "Porcupine Listening for Wake Word..."
            } catch (e: Exception) {
                Log.e("PorcupineManager", "Error starting Porcupine listening", e)
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        try {
            porcupineManager?.stop()
            _isListening.value = false
            _statusMessage.value = "Porcupine Paused"
        } catch (e: Exception) {
            Log.e("PorcupineManager", "Error stopping Porcupine", e)
        }
    }

    override fun invoke(keywordIndex: Int) {
        Log.d("PorcupineManager", "Wake word detected at index $keywordIndex!")
        _statusMessage.value = "Wake Word Detected!"
        onWakeWordDetected()
    }

    fun destroy() {
        try {
            porcupineManager?.delete()
            porcupineManager = null
            _isListening.value = false
            _isInitialized.value = false
        } catch (e: Exception) {
            Log.e("PorcupineManager", "Error destroying Porcupine", e)
        }
    }
}
