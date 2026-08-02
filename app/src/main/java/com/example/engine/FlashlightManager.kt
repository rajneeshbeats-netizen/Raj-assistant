package com.example.engine

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log

/**
 * Utility class to toggle device flashlight on and off.
 */
class FlashlightManager(private val context: Context) {

    private val cameraManager: CameraManager? by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private var cameraId: String? = null
    var isFlashlightOn: Boolean = false
        private set

    init {
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) {
            Log.e("FlashlightManager", "Error querying camera id list", e)
        }
    }

    /**
     * Turn flashlight ON or OFF.
     * @return Result message for assistant feedback.
     */
    fun setFlashlight(turnOn: Boolean): String {
        val targetId = cameraId ?: try {
            cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) {
            null
        }

        if (targetId == null) {
            return "Torch feature is not supported on this device camera."
        }

        return try {
            cameraManager?.setTorchMode(targetId, turnOn)
            isFlashlightOn = turnOn
            if (turnOn) "Flashlight turned ON." else "Flashlight turned OFF."
        } catch (e: CameraAccessException) {
            Log.e("FlashlightManager", "CameraAccessException while toggling flashlight", e)
            "Could not access flashlight camera."
        } catch (e: Exception) {
            Log.e("FlashlightManager", "Error toggling flashlight", e)
            "Failed to toggle flashlight."
        }
    }

    fun toggleFlashlight(): String {
        return setFlashlight(!isFlashlightOn)
    }
}
