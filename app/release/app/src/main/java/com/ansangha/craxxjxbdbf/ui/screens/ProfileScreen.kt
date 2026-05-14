package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.ui.components.AuroraBackground
import com.ansangha.craxxjxbdbf.ui.components.GradientGlassBox
import com.ansangha.craxxjxbdbf.ui.gamification.PlayerProgress
import com.ansangha.craxxjxbdbf.ui.theme.PremiumGradients
import com.ansangha.craxxjxbdbf.ui.viewmodel.HabitViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: HabitViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val xp = remember(habits) { PlayerProgress.totalXp(habits) }
    val level = remember(xp) { PlayerProgress.levelFromXp(xp) }
    val (into, span) = remember(xp) { PlayerProgress.levelProgress(xp) }
    val progress = if (span == 0) 0f else into.toFloat() / span.toFloat()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(650),
        label = "xp",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumGradients.BackgroundGradient),
    ) {
        AuroraBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            RowTopBar(title = "Profile", onBack = onBack)
            Spacer(modifier = Modifier.height(20.dp))
            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        XpNeonRing(progress = animated)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$xp",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = "Total XP",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Next level",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = Color(0xFF22D3EE),
                        trackColor = Color.White.copy(alpha = 0.12f),
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$into / $span XP in this level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowTopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun XpNeonRing(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 14.dp.toPx()
        val radius = (size.minDimension - stroke) / 2f
        val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
        val sweep = 360f * progress.coerceIn(0f, 1f)
        drawArc(
            color = Color.White.copy(alpha = 0.12f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color(0xFF7C3AED), Color(0xFF22D3EE), Color(0xFFEAB308), Color(0xFF7C3AED)),
                center = Offset(size.width / 2f, size.height / 2f),
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
