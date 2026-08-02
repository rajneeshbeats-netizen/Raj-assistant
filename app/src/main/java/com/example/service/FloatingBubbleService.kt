package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Floating Assistant Bubble Overlay Service (ChatGPT style).
 * Displays a draggable floating assistant bubble over any application screen.
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingBubbleService created")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (hasOverlayPermission(this)) {
            createFloatingBubble()
            _isBubbleActive.value = true
        } else {
            Log.w(TAG, "Overlay permission not granted!")
            stopSelf()
        }
    }

    private fun createFloatingBubble() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Build floating circular bubble container
        val frameLayout = FrameLayout(this).apply {
            setPadding(12, 12, 12, 12)
        }

        val circleBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#1A00E5FF")) // Cyan translucent glow
            setStroke(4, Color.parseColor("#00E5FF"))
        }

        val innerCircle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#161B22")) // Dark card background
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            background = circleBackground
            setPadding(16, 16, 16, 16)
        }

        val density = resources.displayMetrics.density
        val bubbleSize = (60 * density).toInt()
        val lp = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
        frameLayout.addView(iconView, lp)

        floatingView = frameLayout

        // Draggable touch listener
        frameLayout.setOnTouchListener(object : View.OnTouchListener {
            private var lastTouchTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = params ?: return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastTouchTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        currentParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager.updateViewLayout(floatingView, currentParams)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating floating view layout", e)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = abs(event.rawX - initialTouchX)
                        val diffY = abs(event.rawY - initialTouchY)
                        val timeDiff = System.currentTimeMillis() - lastTouchTime

                        // Interpret as Click if movement is tiny and duration short
                        if (diffX < 10 && diffY < 10 && timeDiff < 300) {
                            openAssistantApp()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating bubble view to WindowManager", e)
        }
    }

    private fun openAssistantApp() {
        Log.d(TAG, "Floating bubble clicked - Opening Assistant App!")
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view", e)
            }
        }
        _isBubbleActive.value = false
        Log.d(TAG, "FloatingBubbleService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "FloatingBubbleService"

        private val _isBubbleActive = MutableStateFlow(false)
        val isBubbleActive: StateFlow<Boolean> = _isBubbleActive.asStateFlow()

        fun hasOverlayPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        }

        fun requestOverlayPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }

        fun startService(context: Context) {
            if (hasOverlayPermission(context)) {
                val intent = Intent(context, FloatingBubbleService::class.java)
                context.startService(intent)
            } else {
                requestOverlayPermission(context)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        }
    }
}
