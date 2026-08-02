package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.AssistantState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated glowing voice visualizer orb with dynamic audio waveform rings.
 */
@Composable
fun AssistantOrbVisualizer(
    state: AssistantState,
    soundRms: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_angle"
    )

    // Smooth RMS audio level scale boost
    val audioBoost = remember { Animatable(0f) }
    LaunchedEffect(soundRms) {
        audioBoost.animateTo(
            targetValue = soundRms.coerceIn(0f, 1f),
            animationSpec = tween(80)
        )
    }

    val baseColor = when (state) {
        AssistantState.LISTENING -> Color(0xFF00E5FF) // Neon Cyan
        AssistantState.PROCESSING -> Color(0xFF7C4DFF) // Deep Violet
        AssistantState.SPEAKING -> Color(0xFF00E676) // Mint Green
        AssistantState.ERROR -> Color(0xFFFF5252) // Vibrant Red
        AssistantState.IDLE -> Color(0xFF2979FF) // Electric Blue
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("assistant_orb_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2.8f) * (if (state == AssistantState.LISTENING) pulseScale else 1.0f)
            val dynamicRadius = baseRadius + (audioBoost.value * 35.dp.toPx())

            // Outer ambient glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.35f + audioBoost.value * 0.3f),
                        baseColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.6f
                ),
                radius = dynamicRadius * 1.6f,
                center = center
            )

            // Animated orbital audio nodes
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val nodeCount = 12
                val radiansStep = (2 * Math.PI / nodeCount).toFloat()
                for (i in 0 until nodeCount) {
                    val angle = (rotateAngle * Math.PI / 180f).toFloat() + i * radiansStep
                    val nodeDistance = dynamicRadius * (1.15f + 0.15f * sin((angle * 3).toDouble())).toFloat()
                    val nodeX = center.x + nodeDistance * cos(angle)
                    val nodeY = center.y + nodeDistance * sin(angle)
                    drawCircle(
                        color = baseColor.copy(alpha = 0.6f),
                        radius = 3.dp.toPx() * (1f + audioBoost.value),
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // Pulsing border ring
            drawCircle(
                color = baseColor.copy(alpha = 0.8f),
                radius = dynamicRadius * 1.05f,
                center = center,
                style = Stroke(width = (2.dp.toPx() + audioBoost.value * 4.dp.toPx()))
            )

            // Core inner glowing sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        baseColor.copy(alpha = 0.85f),
                        baseColor.copy(alpha = 0.4f)
                    ),
                    center = center,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )
        }

        // Inner content (e.g. Mic button or icon)
        content()
    }
}
