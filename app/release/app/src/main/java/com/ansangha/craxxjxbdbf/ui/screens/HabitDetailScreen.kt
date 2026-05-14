package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.ui.components.AuroraBackground
import com.ansangha.craxxjxbdbf.ui.components.GradientGlassBox
import com.ansangha.craxxjxbdbf.ui.components.NeonProgressRing
import com.ansangha.craxxjxbdbf.ui.components.StreakFlameIcon
import com.ansangha.craxxjxbdbf.ui.theme.PremiumGradients
import com.ansangha.craxxjxbdbf.ui.viewmodel.HabitViewModel

@Composable
fun HabitDetailScreen(
    habitId: String,
    onBack: () -> Unit,
    viewModel: HabitViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val habit = remember(habits, habitId) { habits.firstOrNull { it.id == habitId } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumGradients.BackgroundGradient),
    ) {
        AuroraBackground()
        if (habit == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Habit unavailable", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
            }
        } else {
            HabitDetailContent(
                habit = habit,
                onBack = onBack,
                onComplete = { viewModel.completeHabit(habit.id) },
            )
        }
    }
}

@Composable
private fun HabitDetailContent(
    habit: HabitEntity,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val density = LocalDensity.current.density
    var face by remember { mutableIntStateOf(0) }
    val rotationY by animateFloatAsState(
        targetValue = if (face == 1) 180f else 0f,
        animationSpec = tween(480),
        label = "flip",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FilterChip(
                selected = face == 1,
                onClick = { face = if (face == 0) 1 else 0 },
                label = { Text(if (face == 0) "Flip" else "Front") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White.copy(alpha = 0.18f),
                    selectedLabelColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    labelColor = Color.White,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 16f * density
                },
            contentAlignment = Alignment.Center,
        ) {
            if (rotationY <= 90f) {
                FrontFace(habit = habit, onComplete = onComplete)
            } else {
                Box(
                    modifier = Modifier.graphicsLayer(rotationY = 180f),
                ) {
                    BackFace(habit = habit)
                }
            }
        }
    }
}

@Composable
private fun FrontFace(
    habit: HabitEntity,
    onComplete: () -> Unit,
) {
    GradientGlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = habit.icon, fontSize = 42.sp)
                Spacer(modifier = Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = habit.category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StreakFlameIcon(
                    modifier = Modifier.size(36.dp),
                    intensity = (habit.streakDays.coerceAtMost(30) / 30f).coerceIn(0.35f, 1f),
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${habit.streakDays} day streak") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    ),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                val progress = if (habit.targetCount <= 0) 0f else habit.currentCount.toFloat() / habit.targetCount.toFloat()
                NeonProgressRing(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.size(170.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${habit.currentCount}/${habit.targetCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (habit.isCompleted) "Completed today" else "Daily target",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!habit.isCompleted) {
                AssistChip(
                    onClick = onComplete,
                    label = { Text("Mark complete") },
                    leadingIcon = {
                        Icon(Icons.Default.Check, contentDescription = null)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun BackFace(habit: HabitEntity) {
    GradientGlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Cadence & difficulty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Frequency: ${habit.frequency} · Priority: ${habit.priority}/5 · Difficulty: ${habit.difficulty}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Best streak: ${habit.bestStreak} days · Completions logged: ${habit.completedDates.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
