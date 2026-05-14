package com.ansangha.craxxjxbdbf.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val ParticleCount = 32

@Composable
fun ParticleBurstOverlay(
    pulseKey: Int,
    modifier: Modifier = Modifier,
    originFraction: Pair<Float, Float> = 0.5f to 0.38f,
) {
    val anim = remember { Animatable(0f) }
    val seed = remember {
        val rnd = Random(42)
        List(ParticleCount) {
            (rnd.nextDouble() * Math.PI * 2).toFloat() to (0.35f + rnd.nextFloat() * 0.65f)
        }
    }

    LaunchedEffect(pulseKey) {
        if (pulseKey == 0) return@LaunchedEffect
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
        )
    }

    if (pulseKey == 0 && anim.value == 0f) {
        Box(modifier)
    } else {
        val phase = anim.value
        Canvas(modifier = modifier.fillMaxSize()) {
            val ox = size.width * originFraction.first
            val oy = size.height * originFraction.second
            val maxR = size.maxDimension * 0.48f
            for (i in 0 until ParticleCount) {
                val angle = seed[i].first
                val speed = seed[i].second
                val r = maxR * speed * phase
                val x = ox + cos(angle.toDouble()).toFloat() * r
                val y = oy + sin(angle.toDouble()).toFloat() * r
                val alpha = (1f - phase).coerceIn(0f, 1f) * 0.9f
                val color = when (i % 3) {
                    0 -> Color(0xFFB794F6)
                    1 -> Color(0xFF22D3EE)
                    else -> Color(0xFFEAB308)
                }.copy(alpha = alpha)
                drawCircle(color = color, radius = (2.5f + (i % 3)).dp.toPx(), center = Offset(x, y))
            }
            rotate(degrees = phase * 22f, pivot = Offset(ox, oy)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f * (1f - phase)),
                    radius = 52.dp.toPx() * (0.35f + phase),
                    center = Offset(ox, oy),
                )
            }
        }
    }
}
