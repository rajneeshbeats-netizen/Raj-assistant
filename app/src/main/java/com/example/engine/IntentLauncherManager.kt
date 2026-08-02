package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

/**
 * Handles launching offline Android system intents for apps, phone calls, SMS, settings, alarms, and camera.
 */
class IntentLauncherManager(private val context: Context) {

    /**
     * Search and open an installed app by matching target name (e.g., "whatsapp", "youtube", "camera", etc.)
     */
    fun openAppByName(query: String): String {
        val cleanQuery = query.trim().lowercase()
        val pm = context.packageManager

        // Settings page checks
        if (cleanQuery.contains("setting") || cleanQuery.contains("सेटिंग")) {
            return openSettingsPage(cleanQuery)
        }

        // Common package mapping shortcuts for instant matching
        val knownPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "telegram" to "org.telegram.messenger",
            "spotify" to "com.spotify.music"
        )

        // Special handling for Camera
        if (cleanQuery.contains("camera") || cleanQuery.contains("कैमरा")) {
            return openCamera()
        }

        // Special handling for Contacts / Dialer
        if (cleanQuery.contains("contact") || cleanQuery.contains("कॉन्टेक्ट")) {
            return openContacts()
        }
        if (cleanQuery.contains("dialer") || cleanQuery.contains("phone") || cleanQuery.contains("फ़ोन")) {
            return openDialer()
        }

        // Check if query matches a known package keyword
        for ((key, pkg) in knownPackages) {
            if (cleanQuery.contains(key)) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opening ${key.replaceFirstChar { it.uppercase() }}."
                }
            }
        }

        // Generic lookup through installed applications
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                val appLabel = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (appLabel.contains(cleanQuery) || cleanQuery.contains(appLabel)) {
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "Opening ${pm.getApplicationLabel(appInfo)}."
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Error searching installed apps", e)
        }

        return "App '$query' not found on device."
    }

    /**
     * Open specific Android Settings pages offline.
     */
    fun openSettingsPage(query: String): String {
        val clean = query.lowercase()
        val (action, name) = when {
            clean.contains("wifi") || clean.contains("वाईफाई") -> Settings.ACTION_WIFI_SETTINGS to "Wi-Fi Settings"
            clean.contains("bluetooth") || clean.contains("ब्लूटूथ") -> Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth Settings"
            clean.contains("display") || clean.contains("screen") || clean.contains("स्क्रीन") -> Settings.ACTION_DISPLAY_SETTINGS to "Display Settings"
            clean.contains("sound") || clean.contains("volume") || clean.contains("साउंड") -> Settings.ACTION_SOUND_SETTINGS to "Sound Settings"
            clean.contains("battery") || clean.contains("power") || clean.contains("बैटरी") -> Settings.ACTION_BATTERY_SAVER_SETTINGS to "Battery Settings"
            clean.contains("location") || clean.contains("gps") || clean.contains("लोकेशन") -> Settings.ACTION_LOCATION_SOURCE_SETTINGS to "Location Settings"
            clean.contains("app") || clean.contains("application") -> Settings.ACTION_APPLICATION_SETTINGS to "Apps Settings"
            clean.contains("accessibility") -> Settings.ACTION_ACCESSIBILITY_SETTINGS to "Accessibility Settings"
            else -> Settings.ACTION_SETTINGS to "Device Settings"
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening $name."
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Error opening settings page $action", e)
            "Unable to open $name."
        }
    }

    /**
     * Open Contacts app.
     */
    fun openContacts(): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening Contacts."
        } catch (e: Exception) {
            openAppByName("contacts")
        }
    }

    /**
     * Open Dialer app.
     */
    fun openDialer(): String {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening Dialer."
        } catch (e: Exception) {
            "Unable to open Dialer."
        }
    }

    /**
     * Make a phone call or open the dialer with the phone number.
     */
    fun makePhoneCall(phoneNumberOrName: String, directCallPermissionGranted: Boolean = false): String {
        val cleanInput = phoneNumberOrName.trim()
        val digitsOnly = cleanInput.replace(Regex("[^0-9+]"), "")

        if (digitsOnly.length >= 3) {
            return try {
                val intent = if (directCallPermissionGranted) {
                    Intent(Intent.ACTION_CALL, Uri.parse("tel:$digitsOnly"))
                } else {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digitsOnly"))
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Calling $digitsOnly..."
            } catch (e: Exception) {
                Log.e("IntentLauncher", "Error initiating call", e)
                "Unable to place phone call."
            }
        }

        // If target is a contact name (e.g., "Mom"), open Contacts or Dialer with query
        return if (cleanInput.isNotEmpty()) {
            openContacts()
        } else {
            openDialer()
        }
    }

    /**
     * Open SMS composer prefilled with number and message.
     */
    fun sendSMS(phoneNumberOrName: String, message: String): String {
        val cleanNumber = phoneNumberOrName.replace(Regex("[^0-9+]"), "")
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${cleanNumber.ifEmpty { "" }}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (cleanNumber.isNotEmpty()) {
                "Opening SMS composer for $cleanNumber."
            } else {
                "Opening SMS composer."
            }
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Error opening SMS composer", e)
            "Unable to open SMS application."
        }
    }

    /**
     * Open device camera.
     */
    fun openCamera(): String {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening camera..."
        } catch (e: Exception) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.android.camera")
                ?: pm.getLaunchIntentForPackage("com.google.android.GoogleCamera")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening camera..."
            } else {
                "Unable to open camera."
            }
        }
    }

    /**
     * Set alarm for specified hour and minute (24-hour format).
     */
    fun setAlarm(hour: Int, minute: Int, message: String = "Raj Assistant Alarm"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val formattedTime = String.format("%02d:%02d", hour, minute)
            "Setting alarm for $formattedTime."
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Error setting alarm", e)
            "Unable to set alarm automatically."
        }
    }

    /**
     * Set timer in seconds.
     */
    fun setTimer(durationSeconds: Int, message: String = "Raj Assistant Timer"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val durationLabel = if (durationSeconds >= 60) "${durationSeconds / 60} minutes" else "$durationSeconds seconds"
            "Setting timer for $durationLabel."
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Error setting timer", e)
            "Unable to set timer."
        }
    }
}

