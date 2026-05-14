package com.ansangha.craxxjxbdbf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ansangha.craxxjxbdbf.ui.theme.PremiumGradients

@Composable
fun GradientGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    containerAlpha: Float = 0.72f,
    content: @Composable BoxScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha)
    Box(
        modifier = modifier
            .clip(shape)
            .background(PremiumGradients.BorderNeon)
            .padding(borderWidth)
            .clip(shape)
            .background(surface),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PremiumGradients.GlassOverlay),
        )
        content()
    }
}
