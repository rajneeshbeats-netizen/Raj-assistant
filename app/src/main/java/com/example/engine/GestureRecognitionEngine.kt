package com.example.engine

import android.graphics.PointF
import android.util.Log
import kotlin.math.abs
import kotlin.math.atan2

enum class RecognizedGesture {
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP,
    DRAW_C,
    DRAW_V,
    UNKNOWN
}

/**
 * High-accuracy Offline Gesture Classifier Engine.
 * Analyzes touch point trajectories and detects Swipes, Taps, and Drawn Shapes (C & V).
 */
class GestureRecognitionEngine {

    private var lastTapTimestamp = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    /**
     * Process single tap / double tap timestamps and coordinates.
     */
    fun registerTap(x: Float, y: Float): RecognizedGesture? {
        val now = System.currentTimeMillis()
        val timeDiff = now - lastTapTimestamp
        val dist = Math.hypot((x - lastTapX).toDouble(), (y - lastTapY).toDouble()).toFloat()

        lastTapTimestamp = now
        lastTapX = x
        lastTapY = y

        return if (timeDiff < 350 && dist < 100f) {
            Log.d(TAG, "Gesture Detected: DOUBLE_TAP")
            RecognizedGesture.DOUBLE_TAP
        } else {
            null
        }
    }

    /**
     * Process list of touch points captured during a touch stroke.
     */
    fun classifyStroke(points: List<PointF>): RecognizedGesture {
        if (points.size < 3) return RecognizedGesture.UNKNOWN

        val start = points.first()
        val end = points.last()

        val dx = end.x - start.x
        val dy = end.y - start.y
        val totalDist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

        var minX = points.minOf { it.x }
        var maxX = points.maxOf { it.x }
        var minY = points.minOf { it.y }
        var maxY = points.maxOf { it.y }

        val width = maxX - minX
        val height = maxY - minY

        // 1. Check Horizontal Swipes
        if (abs(dx) > 120f && abs(dy) < 150f && width > abs(dy) * 1.5f) {
            return if (dx > 0) RecognizedGesture.SWIPE_RIGHT else RecognizedGesture.SWIPE_LEFT
        }

        // 2. Check Drawn 'V' shape
        // 'V' shape characteristics:
        // - Starts top left, moves downward-right to a bottom vertex (min Y), then upward-right.
        if (points.size >= 6 && height > 80f) {
            var lowestPointIndex = 0
            var maxObservedY = Float.MIN_VALUE
            for (i in points.indices) {
                if (points[i].y > maxObservedY) {
                    maxObservedY = points[i].y
                    lowestPointIndex = i
                }
            }

            // Lowest point (vertex) should be in the middle ~20%-80% of the gesture stroke
            val ratio = lowestPointIndex.toFloat() / points.size
            if (ratio in 0.2f..0.8f) {
                val firstLegDx = points[lowestPointIndex].x - start.x
                val secondLegDx = end.x - points[lowestPointIndex].x

                if (firstLegDx >= -10f && secondLegDx >= -10f) {
                    Log.d(TAG, "Gesture Detected: DRAW_V")
                    return RecognizedGesture.DRAW_V
                }
            }
        }

        // 3. Check Drawn 'C' shape
        // 'C' shape characteristics:
        // - Starts right/top, curves left (min X occurs in middle), then ends right/bottom.
        if (points.size >= 6 && height > 80f) {
            var leftmostPointIndex = 0
            var minObservedX = Float.MAX_VALUE
            for (i in points.indices) {
                if (points[i].x < minObservedX) {
                    minObservedX = points[i].x
                    leftmostPointIndex = i
                }
            }

            val ratio = leftmostPointIndex.toFloat() / points.size
            if (ratio in 0.2f..0.8f) {
                val indent = (start.x - minObservedX).coerceAtLeast(end.x - minObservedX)
                if (indent > 40f) {
                    Log.d(TAG, "Gesture Detected: DRAW_C")
                    return RecognizedGesture.DRAW_C
                }
            }
        }

        Log.d(TAG, "Stroke unclassified (dx=$dx, dy=$dy, totalDist=$totalDist)")
        return RecognizedGesture.UNKNOWN
    }

    companion object {
        private const val TAG = "GestureRecognitionEngine"
    }
}
