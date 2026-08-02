package com.example.ui.components

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GestureRecognitionEngine
import com.example.engine.RecognizedGesture

@Composable
fun GestureTouchPad(
    modifier: Modifier = Modifier,
    onGestureDetected: (RecognizedGesture) -> Unit
) {
    val gestureEngine = remember { GestureRecognitionEngine() }
    val pathPoints = remember { mutableStateListOf<PointF>() }
    var detectedGestureLabel by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D1117))
            .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .testTag("gesture_touch_pad_container")
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val doubleTapGesture = RecognizedGesture.DOUBLE_TAP
                        detectedGestureLabel = "Double Tap -> Play / Pause"
                        onGestureDetected(doubleTapGesture)
                    },
                    onTap = { offset ->
                        val res = gestureEngine.registerTap(offset.x, offset.y)
                        if (res != null) {
                            detectedGestureLabel = "Double Tap -> Play / Pause"
                            onGestureDetected(res)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        if (changes.isNotEmpty()) {
                            val change = changes.first()
                            if (change.pressed) {
                                pathPoints.add(PointF(change.position.x, change.position.y))
                            } else {
                                if (pathPoints.size >= 3) {
                                    val gesture = gestureEngine.classifyStroke(pathPoints.toList())
                                    if (gesture != RecognizedGesture.UNKNOWN) {
                                        val label = when (gesture) {
                                            RecognizedGesture.SWIPE_LEFT -> "Swipe Left -> Previous Track"
                                            RecognizedGesture.SWIPE_RIGHT -> "Swipe Right -> Next Track"
                                            RecognizedGesture.DOUBLE_TAP -> "Double Tap -> Play/Pause"
                                            RecognizedGesture.DRAW_C -> "Draw 'C' -> Open Camera"
                                            RecognizedGesture.DRAW_V -> "Draw 'V' -> Flashlight"
                                            else -> null
                                        }
                                        detectedGestureLabel = label
                                        onGestureDetected(gesture)
                                    }
                                }
                                pathPoints.clear()
                            }
                        }
                    }
                }
            }
    ) {
        // Draw real-time touch path on canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (pathPoints.size > 1) {
                val path = Path().apply {
                    moveTo(pathPoints[0].x, pathPoints[0].y)
                    for (i in 1 until pathPoints.size) {
                        lineTo(pathPoints[i].x, pathPoints[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFF00E5FF),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Touch pad label and hint overlay
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Gesture,
                contentDescription = null,
                tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Screen-On Gesture Touch Pad",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Swipe ← / → | Double Tap | Draw 'C' or 'V'",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // Live recognized gesture badge feedback
        AnimatedVisibility(
            visible = detectedGestureLabel != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = detectedGestureLabel ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
