package com.example.engine

import android.content.Context
import com.example.data.CustomShortcutEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProcessedActionResult(
    val rawText: String,
    val responseMessage: String,
    val actionType: String, // "FLASHLIGHT", "APP_LAUNCH", "CALL", "SMS", "ALARM", "MUSIC", "GENERAL", "SHORTCUT"
    val isSuccess: Boolean = true
)

/**
 * Intelligent offline natural language parser and intent execution engine for Hindi & English commands.
 */
class ActionProcessor(
    private val context: Context,
    private val flashlightManager: FlashlightManager,
    private val musicController: MusicControllerManager,
    private val intentLauncher: IntentLauncherManager,
    private val gemmaEngine: GemmaNanoOfflineEngine = GemmaNanoOfflineEngine(context)
) {

    /**
     * Parse raw voice or text input and execute matching offline action.
     */
    fun processCommand(
        input: String,
        wakeWord: String = "Hey Raj",
        customShortcuts: List<CustomShortcutEntity> = emptyList(),
        hasCallPermission: Boolean = false
    ): ProcessedActionResult {
        val rawTrimmed = input.trim()
        val lowerInput = rawTrimmed.lowercase()

        // Strip wake word prefix if present (e.g. "Hey Raj turn on flashlight" -> "turn on flashlight")
        val cleanInput = removeWakeWord(lowerInput, wakeWord)

        // 1. Check custom shortcuts first
        for (shortcut in customShortcuts) {
            if (shortcut.isEnabled && lowerInput.contains(shortcut.triggerPhrase.lowercase())) {
                val shortcutResult = executeCustomShortcut(shortcut)
                return ProcessedActionResult(
                    rawText = rawTrimmed,
                    responseMessage = shortcutResult,
                    actionType = "SHORTCUT"
                )
            }
        }

        // 2. Flashlight intent (English & Hindi)
        if (matchesAny(cleanInput, listOf("turn on flashlight", "flashlight on", "torch on", "light on", "टॉर्च चालू करो", "लाइट चालू करो", "टॉर्च ऑन करो"))) {
            val msg = flashlightManager.setFlashlight(true)
            return ProcessedActionResult(rawTrimmed, msg, "FLASHLIGHT")
        }
        if (matchesAny(cleanInput, listOf("turn off flashlight", "flashlight off", "torch off", "light off", "टॉर्च बंद करो", "लाइट बंद करो", "टॉर्च ऑफ करो"))) {
            val msg = flashlightManager.setFlashlight(false)
            return ProcessedActionResult(rawTrimmed, msg, "FLASHLIGHT")
        }
        if (cleanInput.contains("flashlight") || cleanInput.contains("torch") || cleanInput.contains("टॉर्च")) {
            val msg = flashlightManager.toggleFlashlight()
            return ProcessedActionResult(rawTrimmed, msg, "FLASHLIGHT")
        }

        // 3. Camera intent
        if (matchesAny(cleanInput, listOf("open camera", "take picture", "take photo", "start camera", "कैमरा खोलो", "फोटो खींचो", "कैमरा चालू करो"))) {
            val msg = intentLauncher.openCamera()
            return ProcessedActionResult(rawTrimmed, msg, "CAMERA")
        }

        // 4. Music Playback Control intents
        if (matchesAny(cleanInput, listOf("play music", "start music", "play song", "गाना बजाओ", "म्यूजिक चलाओ", "गाना शुरू करो"))) {
            val msg = musicController.playMusic()
            return ProcessedActionResult(rawTrimmed, msg, "MUSIC")
        }
        if (matchesAny(cleanInput, listOf("pause music", "stop music", "pause song", "गाना बंद करो", "म्यूजिक रोको", "गाना रोको"))) {
            val msg = musicController.pauseMusic()
            return ProcessedActionResult(rawTrimmed, msg, "MUSIC")
        }
        if (matchesAny(cleanInput, listOf("next song", "next track", "skip song", "अगला गाना", "नेक्स्ट गाना"))) {
            val msg = musicController.nextSong()
            return ProcessedActionResult(rawTrimmed, msg, "MUSIC")
        }
        if (matchesAny(cleanInput, listOf("previous song", "previous track", "पिछला गाना"))) {
            val msg = musicController.previousSong()
            return ProcessedActionResult(rawTrimmed, msg, "MUSIC")
        }
        if (matchesAny(cleanInput, listOf("volume up", "increase volume", "आवाज़ बढ़ाओ", "वोल्यूम बढ़ाओ"))) {
            val msg = musicController.volumeUp()
            return ProcessedActionResult(rawTrimmed, msg, "MUSIC")
        }
        if (matchesAny(cleanInput, listOf("volume down", "decrease volume", "آवाज़ कम करो", "आवाज़ कम करो", "वोल्यूम कम करो"))) {
            val msg = musicController.volumeDown()
            return ProcessedActionResult(rawTrimmed, msg, "MUSIC")
        }

        // 5. Phone Calls (English & Hindi)
        if (cleanInput.startsWith("call ") || cleanInput.startsWith("dial ") || cleanInput.contains("कॉल करो") || cleanInput.contains("फोन लगाओ")) {
            val target = cleanInput.replace("call ", "")
                .replace("dial ", "")
                .replace("कॉल करो", "")
                .replace("फोन लगाओ", "")
                .replace("to ", "")
                .trim()
            val msg = intentLauncher.makePhoneCall(target, hasCallPermission)
            return ProcessedActionResult(rawTrimmed, msg, "CALL")
        }

        // 6. SMS Messages
        if (cleanInput.startsWith("send sms") || cleanInput.startsWith("send message") || cleanInput.contains("मैसेज भेजो") || cleanInput.contains("एसएमएस करो")) {
            val parts = cleanInput.split("to ", limit = 2)
            val numberOrName = if (parts.size > 1) parts[1].split(" ", limit = 2).firstOrNull() ?: "" else ""
            val body = if (parts.size > 1 && parts[1].split(" ", limit = 2).size > 1) parts[1].split(" ", limit = 2)[1] else "Hello from Raj Assistant"
            val msg = intentLauncher.sendSMS(numberOrName, body)
            return ProcessedActionResult(rawTrimmed, msg, "SMS")
        }

        // 7. Alarms & Timers
        if (cleanInput.contains("alarm") || cleanInput.contains("अलार्म") || cleanInput.contains("wake me up")) {
            val (hour, minute) = extractTimeFromText(cleanInput)
            val msg = intentLauncher.setAlarm(hour, minute, "Raj Assistant Alarm")
            return ProcessedActionResult(rawTrimmed, msg, "ALARM")
        }
        if (cleanInput.contains("timer") || cleanInput.contains("टाइमर")) {
            val seconds = extractSecondsFromText(cleanInput)
            val msg = intentLauncher.setTimer(seconds, "Raj Assistant Timer")
            return ProcessedActionResult(rawTrimmed, msg, "ALARM")
        }

        // 8. Open Apps
        if (cleanInput.startsWith("open ") || cleanInput.startsWith("launch ") || cleanInput.contains("खोलो") || cleanInput.contains("चलाओ")) {
            val appQuery = cleanInput.replace("open ", "")
                .replace("launch ", "")
                .replace("खोलो", "")
                .replace("चलाओ", "")
                .trim()
            if (appQuery.isNotEmpty()) {
                val msg = intentLauncher.openAppByName(appQuery)
                return ProcessedActionResult(rawTrimmed, msg, "APP_LAUNCH")
            }
        }

        // 9. Time & Date info
        if (matchesAny(cleanInput, listOf("what time is it", "time", "current time", "समय क्या हुआ है", "टाइम बताओ", "कितने बजे हैं"))) {
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val isHindi = containsHindi(cleanInput)
            val msg = if (isHindi) "अभी समय $timeStr हुआ है।" else "The current time is $timeStr."
            return ProcessedActionResult(rawTrimmed, msg, "GENERAL")
        }

        if (matchesAny(cleanInput, listOf("what is today's date", "date today", "today date", "आज क्या तारीख है", "आज कौन सी तारीख है"))) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            val isHindi = containsHindi(cleanInput)
            val msg = if (isHindi) "आज की तारीख $dateStr है।" else "Today is $dateStr."
            return ProcessedActionResult(rawTrimmed, msg, "GENERAL")
        }

        // 10. Greetings & Identity
        if (matchesAny(cleanInput, listOf("who are you", "what is your name", "तुम कौन हो", "आपका नाम क्या है"))) {
            val isHindi = containsHindi(cleanInput)
            val msg = if (isHindi) "मैं राज हूं, आपका व्यक्तिगत ऑफ़लाइन वॉयस असिस्टेंट।" else "I am Raj, your personal offline smart voice assistant."
            return ProcessedActionResult(rawTrimmed, msg, "GENERAL")
        }

        if (matchesAny(cleanInput, listOf("hello", "hi", "hey", "नमस्ते", "हेलो", "प्रणाम"))) {
            val isHindi = containsHindi(cleanInput)
            val msg = if (isHindi) "नमस्ते! मैं आपकी क्या सहायता कर सकता हूं?" else "Hello! How can I assist you today?"
            return ProcessedActionResult(rawTrimmed, msg, "GENERAL")
        }

        // Fallback search app launch or Gemma 3n Nano Offline AI Chat Engine
        if (cleanInput.split(" ").size in 1..2) {
            val fallbackAppMsg = intentLauncher.openAppByName(cleanInput)
            if (!fallbackAppMsg.contains("not found")) {
                return ProcessedActionResult(rawTrimmed, fallbackAppMsg, "APP_LAUNCH")
            }
        }

        // Delegate to Gemma 3n Nano Offline AI Chat Engine for intelligent chat response
        val aiResponse = gemmaEngine.generateResponse(cleanInput)
        return ProcessedActionResult(rawTrimmed, aiResponse, "AI_CHAT")
    }

    private fun removeWakeWord(input: String, wakeWord: String): String {
        var clean = input.replace(wakeWord.lowercase(), "")
            .replace("hey raj", "")
            .replace("हे राज", "")
            .replace("राज", "")
            .trim()
        if (clean.startsWith("speak ") || clean.startsWith("please ")) {
            clean = clean.replace("speak ", "").replace("please ", "").trim()
        }
        return clean.ifEmpty { input }
    }

    private fun matchesAny(input: String, keywords: List<String>): Boolean {
        return keywords.any { input.contains(it) }
    }

    private fun containsHindi(text: String): Boolean {
        return text.any { it.code in 0x0900..0x097F }
    }

    private fun executeCustomShortcut(shortcut: CustomShortcutEntity): String {
        return when (shortcut.actionType) {
            "OPEN_APP" -> intentLauncher.openAppByName(shortcut.actionTarget)
            "TOGGLE_FLASHLIGHT" -> flashlightManager.toggleFlashlight()
            "SET_ALARM" -> {
                val (h, m) = extractTimeFromText(shortcut.actionTarget)
                intentLauncher.setAlarm(h, m, shortcut.triggerPhrase)
            }
            "PLAY_MUSIC" -> musicController.playMusic()
            else -> shortcut.actionTarget.ifEmpty { "Shortcut triggered." }
        }
    }

    private fun extractTimeFromText(text: String): Pair<Int, Int> {
        // Regex for patterns like "7:30", "07:00", "7 am", "8 pm"
        val timeRegex = Regex("(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val match = timeRegex.find(text)

        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 7
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase()

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        }

        return Pair(7, 0) // Default 7:00 AM
    }

    private fun extractSecondsFromText(text: String): Int {
        val numberMatch = Regex("(\\d+)").find(text)
        val valNum = numberMatch?.groupValues?.get(1)?.toIntOrNull() ?: 60

        return when {
            text.contains("minute") || text.contains("मिनट") -> valNum * 60
            text.contains("hour") || text.contains("घंटा") -> valNum * 3600
            else -> valNum
        }
    }
}
