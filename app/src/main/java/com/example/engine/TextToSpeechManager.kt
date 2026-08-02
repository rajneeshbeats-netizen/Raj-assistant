package com.example.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages offline Text-To-Speech with English and Hindi dual support.
 */
class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            _isInitialized.value = true
            Log.d("TextToSpeechManager", "TTS initialized successfully")
        } else {
            Log.e("TextToSpeechManager", "TTS initialization failed with status: $status")
        }
    }

    /**
     * Speak out text using detected language (Hindi devanagari script check vs English).
     */
    fun speak(text: String, languagePreference: String = "auto") {
        if (tts == null || !_isInitialized.value) {
            Log.w("TextToSpeechManager", "TTS not ready yet")
            return
        }

        // Determine locale based on text script or preference
        val locale = when {
            languagePreference == "hi" -> Locale("hi", "IN")
            languagePreference == "en" -> Locale.ENGLISH
            containsHindiScript(text) -> Locale("hi", "IN")
            else -> Locale("en", "IN")
        }

        try {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if Hindi voice package not installed locally
                tts?.setLanguage(Locale.ENGLISH)
            }
        } catch (e: Exception) {
            tts?.setLanguage(Locale.ENGLISH)
        }

        tts?.setPitch(speechPitch)
        tts?.setSpeechRate(speechRate)

        val utteranceId = "raj_speech_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Error shutting down TTS", e)
        }
    }

    private fun containsHindiScript(text: String): Boolean {
        // Devanagari unicode range: U+0900 to U+097F
        for (char in text) {
            if (char.code in 0x0900..0x097F) {
                return true
            }
        }
        return false
    }
}
