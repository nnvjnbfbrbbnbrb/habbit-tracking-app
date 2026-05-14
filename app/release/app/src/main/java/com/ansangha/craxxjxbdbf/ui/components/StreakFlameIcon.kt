package com.ansangha.craxxjxbdbf.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.sin

@Composable
fun StreakFlameIcon(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val t = rememberInfiniteTransition(label = "flame")
    val wave by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sway = (sin((wave + intensity) * Math.PI * 2).toFloat()) * w * 0.06f

        val outer = Path().apply {
            moveTo(w * 0.5f + sway, h * 0.92f)
            cubicTo(
                w * 0.2f + sway, h * 0.65f,
                w * 0.18f, h * 0.35f,
                w * 0.5f, h * 0.08f,
            )
            cubicTo(
                w * 0.82f, h * 0.35f,
                w * 0.8f - sway, h * 0.65f,
                w * 0.5f - sway, h * 0.92f,
            )
            close()
        }

        val inner = Path().apply {
            moveTo(w * 0.5f + sway * 0.5f, h * 0.78f)
            cubicTo(
                w * 0.34f, h * 0.58f,
                w * 0.36f, h * 0.4f,
                w * 0.5f, h * 0.22f,
            )
            cubicTo(
                w * 0.64f, h * 0.4f,
                w * 0.66f, h * 0.58f,
                w * 0.5f - sway * 0.5f, h * 0.78f,
            )
            close()
        }

        drawPath(
            path = outer,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFB020).copy(alpha = 0.95f * intensity),
                    Color(0xFFFF4D8D).copy(alpha = 0.75f * intensity),
                    Color(0xFF7C3AED).copy(alpha = 0.55f * intensity),
                ),
            ),
            style = Fill,
        )
        drawPath(
            path = inner,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF3C2).copy(alpha = 0.9f * intensity),
                    Color(0xFFFFD54F).copy(alpha = 0.55f * intensity),
                ),
            ),
            style = Fill,
        )
    }
}
