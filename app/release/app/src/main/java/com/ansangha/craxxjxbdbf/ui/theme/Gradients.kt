package com.ansangha.craxxjxbdbf.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object PremiumGradients {

    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF7C3AED),
            Color(0xFF22D3EE),
            Color(0xFFEAB308),
        )
    )

    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF020617),
            Color(0xFF0B1220),
            Color(0xFF0F172A),
        )
    )

    val AccentGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFA78BFA).copy(alpha = 0.55f),
            Color(0xFF22D3EE).copy(alpha = 0.35f),
            Color.Transparent,
        )
    )

    val SuccessGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF059669),
            Color(0xFF34D399),
        )
    )

    val WarningGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF59E0B),
            Color(0xFFEAB308),
        )
    )

    val ErrorGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFEF4444),
            Color(0xFFF97316),
        )
    )

    val GlassOverlay = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.14f),
            Color.Transparent,
            Color.White.copy(alpha = 0.06f),
        )
    )

    val CardGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.03f),
            Color.Transparent,
        )
    )

    val BorderNeon = Brush.linearGradient(
        colors = listOf(
            Color(0xFF7C3AED),
            Color(0xFF22D3EE),
            Color(0xFFEAB308),
            Color(0xFF7C3AED),
        )
    )

    object CategoryGradients {
        val Health = Brush.linearGradient(
            listOf(Color(0xFF34D399), Color(0xFF22D3EE))
        )

        val Fitness = Brush.linearGradient(
            listOf(Color(0xFFF472B6), Color(0xFFFBBF24))
        )

        val Learning = Brush.linearGradient(
            listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
        )

        val Mindfulness = Brush.linearGradient(
            listOf(Color(0xFF94A3B8), Color(0xFFC084FC))
        )

        val Creativity = Brush.linearGradient(
            listOf(Color(0xFFE879F9), Color(0xFF60A5FA))
        )

        val Productivity = Brush.linearGradient(
            listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
        )
    }
}

fun Brush.Companion.primary() = PremiumGradients.PrimaryGradient
fun Brush.Companion.background() = PremiumGradients.BackgroundGradient
fun Brush.Companion.accent() = PremiumGradients.AccentGradient
fun Brush.Companion.success() = PremiumGradients.SuccessGradient
fun Brush.Companion.warning() = PremiumGradients.WarningGradient
fun Brush.Companion.error() = PremiumGradients.ErrorGradient
fun Brush.Companion.glass() = PremiumGradients.GlassOverlay
fun Brush.Companion.card() = PremiumGradients.CardGradient
