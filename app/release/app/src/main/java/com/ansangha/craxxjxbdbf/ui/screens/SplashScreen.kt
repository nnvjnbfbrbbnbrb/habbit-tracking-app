package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ansangha.craxxjxbdbf.ui.components.AuroraBackground
import com.ansangha.craxxjxbdbf.ui.theme.PremiumGradients
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingComplete: Boolean,
    onFinished: (SplashDestination) -> Unit,
) {
    val density = LocalDensity.current.density
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val phase by shimmer.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    LaunchedEffect(Unit) {
        delay(1650)
        onFinished(if (onboardingComplete) SplashDestination.Main else SplashDestination.Onboarding)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumGradients.BackgroundGradient),
    ) {
        AuroraBackground()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.12f),
                            Color.Transparent,
                            Color(0xFF22D3EE).copy(alpha = 0.1f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f * phase, 1400f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = 1f + (phase * 0.04f).coerceIn(-0.06f, 0.06f)
                        scaleY = 1f + (phase * 0.04f).coerceIn(-0.06f, 0.06f)
                        rotationY = 12f * sin(phase.toDouble()).toFloat()
                        cameraDistance = 18f * density
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(PremiumGradients.BorderNeon),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✦",
                        fontSize = 56.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "HabitPro",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Neon focus. Quiet discipline.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
    }
}

enum class SplashDestination {
    Onboarding,
    Main,
}
