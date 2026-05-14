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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeonProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 14.dp,
    trackColor: Color = Color.White.copy(alpha = 0.12f),
) {
    val transition = rememberInfiniteTransition(label = "ringGlow")
    val glow by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Canvas(modifier = modifier) {
        val sweep = (360f * progress.coerceIn(0f, 1f))
        val strokePx = stroke.toPx()
        val radius = (size.minDimension - strokePx) / 2f
        val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        // Outer soft glow (layered arc, no blur)
        for (i in 3 downTo 1) {
            val w = strokePx + i * 3.2f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFB794F6).copy(alpha = 0.08f * i),
                        Color(0xFF22D3EE).copy(alpha = 0.06f * i),
                        Color(0xFFEAB308).copy(alpha = 0.05f * i),
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = w * glow, cap = StrokeCap.Round),
            )
        }

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF7C3AED),
                    Color(0xFF22D3EE),
                    Color(0xFFEAB308),
                    Color(0xFF7C3AED),
                ),
                center = Offset(size.width / 2f, size.height / 2f),
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}
