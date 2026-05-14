package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.ui.components.AuroraBackground
import com.ansangha.craxxjxbdbf.ui.components.StreakFlameIcon
import com.ansangha.craxxjxbdbf.ui.components.pressScale
import com.ansangha.craxxjxbdbf.ui.components.HabitListShimmer
import com.ansangha.craxxjxbdbf.ui.components.ParticleBurstOverlay
import com.ansangha.craxxjxbdbf.ui.theme.*
import com.ansangha.craxxjxbdbf.ui.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    onOpenHabit: (String) -> Unit,
    viewModel: HabitViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    val displayedHabits = remember(habits, searchQuery, categoryFilter) {
        habits.filter { h ->
            val matchesCat = categoryFilter == null ||
                h.category.equals(categoryFilter, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                h.name.contains(q, ignoreCase = true) ||
                h.description.contains(q, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    val categoryChips = remember(habits) {
        buildList {
            add(null)
            habits.map { it.category.ifBlank { "other" } }.distinct().sorted().forEach { add(it) }
        }
    }

    val density = LocalDensity.current
    val floatTransition = rememberInfiniteTransition(label = "homeFloat")
    val floatPhase by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "floatPhase",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AuroraBackground()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PremiumGradients.BackgroundGradient)
        ) {
        // Premium gradient overlay effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.15f
                }
                .background(PremiumGradients.PrimaryGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Premium Glassmorphism Header with gradient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PremiumGradients.GlassOverlay, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.habit_list_headline),
                            style = Typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { 
                                    translationY = (-2).dp.toPx()
                                }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.habit_list_tagline),
                                style = CustomTypography.HabitName,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text(stringResource(R.string.search_habits_placeholder)) },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshHabits() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Search
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                items(
                    items = categoryChips,
                    key = { it ?: "all" }
                ) { chip ->
                    val label = chip?.replaceFirstChar { it.uppercase() } ?: "All"
                    val selected = (chip == null && categoryFilter == null) ||
                        (chip != null && chip.equals(categoryFilter, ignoreCase = true))
                    FilterChip(
                        selected = selected,
                        onClick = {
                            categoryFilter = chip
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            // Habits List with premium cards
            when {
                isLoading && habits.isEmpty() -> {
                    HabitListShimmer(modifier = Modifier.fillMaxWidth())
                }

                habits.isEmpty() -> {
                    EmptyStateCard()
                }

                displayedHabits.isEmpty() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .shadow(16.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.filter_empty_message),
                            style = Typography.bodyLarge,
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = displayedHabits,
                            key = { _, h -> h.id },
                        ) { index, habit ->
                            val offsetPx = sin(floatPhase + index * 0.55f) * with(density) { 5.dp.toPx() }
                            PremiumHabitCard(
                                habit = habit,
                                floatOffsetPx = offsetPx,
                                onOpenDetail = { onOpenHabit(habit.id) },
                                onComplete = { viewModel.completeHabit(habit.id) },
                                onDelete = { viewModel.deleteHabit(habit) },
                            )
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Error Message with premium styling
            errorMessage?.let { error ->
                AnimatedVisibility(
                    visible = error.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PremiumGradients.ErrorGradient, RoundedCornerShape(16.dp))
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = Typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun PremiumHabitCard(
    habit: HabitEntity,
    floatOffsetPx: Float,
    onOpenDetail: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val isCompleted by remember(habit.id, habit.isCompleted) { derivedStateOf { habit.isCompleted } }
    var burst by remember(habit.id) { mutableIntStateOf(0) }
    val openInteraction = remember(habit.id) { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = floatOffsetPx },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isCompleted) 8.dp else 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = if (isCompleted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        try {
                            Color(android.graphics.Color.parseColor(habit.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        }
                    },
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                },
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isCompleted) 8.dp else 16.dp,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isCompleted) PremiumGradients.SuccessGradient else PremiumGradients.CardGradient,
                        RoundedCornerShape(24.dp),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = openInteraction,
                            indication = null,
                            onClick = onOpenDetail,
                        )
                        .pressScale(openInteraction)
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (isCompleted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    try {
                                        Color(android.graphics.Color.parseColor(habit.color))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                }.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = habit.icon,
                            fontSize = 32.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = habit.name,
                                style = CustomTypography.HabitName,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            if (isCompleted) {
                                Card(
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 4.dp,
                                    ),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = habit.description,
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StreakFlameIcon(
                                            modifier = Modifier.size(18.dp),
                                            intensity = (habit.streakDays.coerceAtMost(30) / 30f).coerceIn(0.35f, 1f),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${habit.streakDays} days", style = Typography.labelMedium)
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = AssistChipDefaults.assistChipElevation(
                                    elevation = 4.dp,
                                ),
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                ),
                            ) {
                                Text(
                                    text = "${habit.currentCount}/${habit.targetCount}",
                                    style = Typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (!isCompleted) {
                                onComplete()
                                burst++
                            }
                        },
                        enabled = !isCompleted,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete",
                            tint = if (isCompleted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            )
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        ParticleBurstOverlay(
            pulseKey = burst,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyStateCard() {
    val title = stringResource(R.string.empty_state_title)
    val body = stringResource(R.string.empty_state_description)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 16.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PremiumGradients.AccentGradient, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎯",
                    fontSize = 80.sp,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = 5f
                    }
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = body,
                    style = CustomTypography.EmptyStateText,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}