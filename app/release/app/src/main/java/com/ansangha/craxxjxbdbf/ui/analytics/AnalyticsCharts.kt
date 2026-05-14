package com.ansangha.craxxjxbdbf.ui.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansangha.craxxjxbdbf.repository.DailyCompletionRate
import com.ansangha.craxxjxbdbf.repository.HeatmapCell
import com.ansangha.craxxjxbdbf.repository.MonthDayMarker
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DailyCompletionChart(
    points: List<DailyCompletionRate>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val target = points.maxOfOrNull { it.rate }?.coerceAtLeast(0.05f) ?: 1f
    val animated by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(650),
        label = "chartReveal",
    )
    val lineColor = MaterialTheme.colorScheme.primary
    val fillBrush = Brush.verticalGradient(
        colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
    )
    Canvas(modifier = modifier.height(140.dp)) {
        val w = size.width
        val h = size.height
        val pad = 8.dp.toPx()
        val n = points.size
        if (n < 2) return@Canvas
        val step = (w - pad * 2) / (n - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()
        points.forEachIndexed { i, p ->
            val x = pad + i * step
            val y = h - pad - (p.rate / target) * (h - pad * 2) * animated
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - pad)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(pad + (n - 1) * step, h - pad)
        fillPath.close()
        drawPath(fillPath, brush = fillBrush)
        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun WeeklyHabitHeatmap(
    cells: List<HeatmapCell>,
    dayLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    val habits = cells.map { it.habitName to it.habitId }.distinctBy { it.second }
    if (habits.isEmpty()) {
        Text("Complete a habit to unlock the heatmap.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Spacer(modifier = Modifier.width(72.dp))
            dayLabels.forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        habits.forEach { (name, hid) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name.take(10) + if (name.length > 10) "…" else "",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(72.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in dayLabels.indices) {
                        val intensity = cells.find { it.habitId == hid && it.dayIndex == i }?.intensity ?: 0f
                        val base = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    base.copy(alpha = 0.12f + intensity * 0.78f),
                                    RoundedCornerShape(6.dp),
                                )
                                .border(1.dp, base.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCompletionGrid(
    markers: List<MonthDayMarker>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        markers.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { m ->
                    val bg = MaterialTheme.colorScheme.secondaryContainer.copy(
                        alpha = 0.25f + m.rate * 0.65f,
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(bg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                m.day.toString(),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            if (m.hasStreakHint) {
                                Text(
                                    "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
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
fun HourProductivityBars(
    counts: List<Int>,
    modifier: Modifier = Modifier,
) {
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val animated by animateFloatAsState(1f, tween(500), label = "bars")
    Row(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        counts.forEachIndexed { _, c ->
            val hFrac = (c.toFloat() / max) * animated
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 + 88 * hFrac).dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f + 0.55f * hFrac),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
    Text(
        "0h → 23h (local) · Productivity rhythm from habit check-in times.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

fun lastSevenDayLabels(zone: ZoneId): List<String> {
    val fmt = DateTimeFormatter.ofPattern("EEE")
    val today = java.time.LocalDate.now(zone)
    return (0..6).map { i ->
        today.minusDays(6 - i.toLong()).format(fmt)
    }
}
