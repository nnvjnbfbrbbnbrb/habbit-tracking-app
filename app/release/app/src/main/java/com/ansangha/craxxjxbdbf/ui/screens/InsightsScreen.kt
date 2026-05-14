package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.data.local.entity.AchievementEntity
import com.ansangha.craxxjxbdbf.ui.components.AuroraBackground
import com.ansangha.craxxjxbdbf.ui.components.GradientGlassBox
import com.ansangha.craxxjxbdbf.ui.theme.CustomTypography
import com.ansangha.craxxjxbdbf.ui.theme.PremiumGradients
import com.ansangha.craxxjxbdbf.ui.theme.Typography
import androidx.compose.material.icons.rounded.QueryStats
import com.ansangha.craxxjxbdbf.ui.analytics.DailyCompletionChart
import com.ansangha.craxxjxbdbf.ui.analytics.HourProductivityBars
import com.ansangha.craxxjxbdbf.ui.analytics.MonthCompletionGrid
import com.ansangha.craxxjxbdbf.ui.analytics.WeeklyHabitHeatmap
import com.ansangha.craxxjxbdbf.ui.analytics.lastSevenDayLabels
import com.ansangha.craxxjxbdbf.ui.viewmodel.AnalyticsViewModel
import com.ansangha.craxxjxbdbf.ui.viewmodel.HabitViewModel

@Composable
fun InsightsScreen(
    onOpenAchievements: () -> Unit = {},
    viewModel: HabitViewModel = hiltViewModel(),
) {
    val analyticsViewModel = hiltViewModel<AnalyticsViewModel>()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()

    val stats = remember(habits) {
        val total = habits.size
        val active = habits.count { it.isActive }
        val bestEver = habits.maxOfOrNull { it.bestStreak } ?: 0
        val sumStreaks = habits.sumOf { it.streakDays }
        val completedToday = habits.count { it.isActive && it.isCompleted }
        val byCategory = habits.groupingBy { it.category.ifBlank { "other" } }.eachCount()
        Triple(total, active, Triple(bestEver, sumStreaks, completedToday)) to byCategory
    }
    val (triple, byCategory) = stats
    val (total, active, inner) = triple
    val (bestEver, sumStreaks, completedToday) = inner

    val analytics by analyticsViewModel.snapshot.collectAsStateWithLifecycle()

    val maxCat = remember(byCategory) { byCategory.values.maxOrNull() ?: 1 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp)),
    ) {
        AuroraBackground()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617).copy(alpha = 0.42f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Pulse",
                style = CustomTypography.HeroTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Live momentum from every habit you run.",
                style = Typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
            )

            analytics?.let { s ->
                GradientGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = "Analytics",
                                style = Typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Text(
                            text = "Daily completion (distinct habits / active), last 30 local days.",
                            style = Typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                        DailyCompletionChart(
                            points = s.dailyRates.takeLast(30),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Weekly heatmap",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        WeeklyHabitHeatmap(
                            cells = s.heatmap,
                            dayLabels = lastSevenDayLabels(s.zone),
                        )
                        Text(
                            text = "This month",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        MonthCompletionGrid(markers = s.monthMarkers)
                        Text(
                            text = "Streaks & consistency",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            text = "Global best streak: ${s.globalBestStreak} days",
                            style = Typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        s.perHabitBestStreak.take(5).forEach { (name, st) ->
                            Text(
                                "• $name — best $st d",
                                style = Typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        s.mostConsistent?.let {
                            Text(
                                text = "Most consistent (30d): ${it.name} (${(it.rate * 100).toInt()}% of days)",
                                style = Typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                        s.gentleWeakest?.let {
                            Text(
                                text = "Room to shine: ${it.name} (${(it.rate * 100).toInt()}% of days) — gentle reset, no guilt.",
                                style = Typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                        }
                        Text(
                            text = "Time-of-day rhythm",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        HourProductivityBars(counts = s.hourlyHeat)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatGlowCard(
                    modifier = Modifier.weight(1f),
                    title = "Habits",
                    value = total.toString(),
                    subtitle = "$active active",
                    brush = PremiumGradients.PrimaryGradient,
                )
                StatGlowCard(
                    modifier = Modifier.weight(1f),
                    title = "Best streak",
                    value = bestEver.toString(),
                    subtitle = "days peak",
                    brush = PremiumGradients.WarningGradient,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatGlowCard(
                    modifier = Modifier.weight(1f),
                    title = "Streak sum",
                    value = sumStreaks.toString(),
                    subtitle = "total days",
                    brush = PremiumGradients.SuccessGradient,
                )
                StatGlowCard(
                    modifier = Modifier.weight(1f),
                    title = "Done today",
                    value = completedToday.toString(),
                    subtitle = stringResource(R.string.insights_done_today_subtitle),
                    brush = PremiumGradients.AccentGradient,
                )
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = stringResource(R.string.insights_achievements_title),
                                style = Typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = onOpenAchievements,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Shelf")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (achievements.isEmpty()) {
                        Text(
                            text = stringResource(R.string.insights_achievements_empty),
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        for (a in achievements.sortedBy { it.category }) {
                            AchievementInsightRow(achievement = a)
                        }
                    }
                }
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Category mix",
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (byCategory.isEmpty()) {
                        Text(
                            text = "Add habits to see your category landscape.",
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        CategoryBarChart(
                            entries = byCategory.entries.sortedByDescending { it.value },
                            maxValue = maxCat,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryLineSpark(
                            values = byCategory.entries.sortedBy { it.key }.map { it.value },
                        )
                    }
                }
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                containerAlpha = 0.55f,
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = "Keep the heat on",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Small daily reps beat rare hero sessions. Tap complete on your list to feed the streak engine.",
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBarChart(
    entries: List<Map.Entry<String, Int>>,
    maxValue: Int,
) {
    val wave by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900),
        label = "bars",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        if (entries.isEmpty()) return@Canvas
        val m = maxValue.coerceAtLeast(1)
        val barWidth = size.width / (entries.size * 1.6f)
        val gap = barWidth * 0.35f
        entries.forEachIndexed { index, (_, count) ->
            val frac = (count.toFloat() / m) * wave
            val barHeight = frac * (size.height * 0.72f)
            val top = size.height - barHeight
            val left = gap + index * (barWidth + gap)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF7C3AED), Color(0xFF22D3EE)),
                ),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
    }
}

@Composable
private fun CategoryLineSpark(values: List<Int>) {
    val maxV = remember(values) { values.maxOrNull()?.coerceAtLeast(1) ?: 1 }
    val wave by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900),
        label = "spark",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
    ) {
        if (values.isEmpty()) return@Canvas
        val path = Path()
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        values.forEachIndexed { i, v ->
            val x = i * step
            val frac = (v.toFloat() / maxV) * wave
            val y = size.height - (frac * size.height * 0.75f) - 6f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(Color(0xFFEAB308), Color(0xFF22D3EE))),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun AchievementInsightRow(achievement: AchievementEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = achievement.icon,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                style = Typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = achievement.description,
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (achievement.isUnlocked) "✓" else "${achievement.currentProgress}/${achievement.requirementValue}",
            style = Typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (achievement.isUnlocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun StatGlowCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    brush: Brush,
) {
    GradientGlassBox(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(22.dp),
        containerAlpha = 0.18f,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = Typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = value,
                    style = CustomTypography.StatsNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = subtitle,
                    style = Typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}
