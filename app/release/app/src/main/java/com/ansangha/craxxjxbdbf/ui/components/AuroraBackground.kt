package com.ansangha.craxxjxbdbf.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Slow-moving aurora / mesh glow behind screens for a high-end look.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    baseTint: Color = Color(0xFF020617)
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx1 = w * (0.25f + phase * 0.5f)
        val cy1 = h * (0.2f + (1f - phase) * 0.35f)
        val cx2 = w * (0.75f - phase * 0.45f)
        val cy2 = h * (0.55f + phase * 0.25f)

        drawRect(baseTint)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF7C3AED).copy(alpha = 0.42f),
                    Color(0xFF7C3AED).copy(alpha = 0f)
                ),
                center = Offset(cx1, cy1),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(cx1, cy1)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF22D3EE).copy(alpha = 0.38f),
                    Color(0xFF22D3EE).copy(alpha = 0f)
                ),
                center = Offset(cx2, cy2),
                radius = h * 0.75f
            ),
            radius = h * 0.75f,
            center = Offset(cx2, cy2)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFEAB308).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * (0.85f - phase * 0.15f)),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.5f, h * (0.85f - phase * 0.15f))
        )
    }
}
