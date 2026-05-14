package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineBadgeEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import com.ansangha.craxxjxbdbf.domain.RoutineDayMask
import com.ansangha.craxxjxbdbf.ui.viewmodel.RoutineListViewModel
import kotlinx.coroutines.launch

@Composable
fun RoutineListScreen(
    onAddRoutine: () -> Unit,
    onOpenComplete: (Long) -> Unit,
    viewModel: RoutineListViewModel = hiltViewModel(),
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val focusOn by viewModel.focusEnabled.collectAsStateWithLifecycle()
    val sleepOn by viewModel.sleepEnabled.collectAsStateWithLifecycle()

    var streakMap by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    LaunchedEffect(tasks) {
        val next = LinkedHashMap<Long, Int>(tasks.size)
        for (t in tasks) {
            next[t.id] = viewModel.streakFor(t.id)
        }
        streakMap = next
    }

    var sleepStart by remember { mutableIntStateOf(22 * 60) }
    var sleepWake by remember { mutableIntStateOf(7 * 60) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("Daily routines", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Exact-time reminders use AlarmManager (with battery-aware fallbacks). " +
                "Focus and sleep modes are transparent and best-effort — not unbreakable locks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        val (into, need) = viewModel.xpBar()
        Text("Level ${progress?.level ?: 1} · XP ${progress?.xp ?: 0}", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { if (need == 0) 0f else into.toFloat() / need.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
        Text("Weekly leaderboard (local demo)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
        Text("#1 You — keep the streak alive.", style = MaterialTheme.typography.bodyMedium)
        if (badges.isNotEmpty()) {
            Text("Badges", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.take(6).forEach { b: RoutineBadgeEntity ->
                    FilterChip(selected = true, onClick = {}, label = { Text(b.badgeId) })
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Focus mode", style = MaterialTheme.typography.titleSmall)
                Text("Blocks other apps when usage access is granted.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = focusOn,
                onCheckedChange = { viewModel.applyFocusToggle(it, ctx.applicationContext) },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Sleep schedule", style = MaterialTheme.typography.titleSmall)
                Text("Full-screen rest reminder at night.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = sleepOn,
                onCheckedChange = { en ->
                    scope.launch {
                        viewModel.updateSleepSchedule(en, sleepStart, sleepWake)
                    }
                },
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = (sleepStart / 60).toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { h -> sleepStart = h.coerceIn(0, 23) * 60 + sleepStart % 60 } },
                    label = { Text("Sleep hour") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = (sleepWake / 60).toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { h -> sleepWake = h.coerceIn(0, 23) * 60 + sleepWake % 60 } },
                    label = { Text("Wake hour") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        TextButton(onClick = { viewModel.pushRoutineSnapshotBestEffort() }) {
            Text("Push routines to bridge (if configured)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tasks, key = { it.id }) { task ->
                RoutineTaskRow(
                    task = task,
                    streak = streakMap[task.id] ?: 0,
                    viewModel = viewModel,
                    onOpenComplete = onOpenComplete,
                )
            }
        }
    }
}

@Composable
private fun RoutineTaskRow(
    task: RoutineTaskEntity,
    streak: Int,
    viewModel: RoutineListViewModel,
    onOpenComplete: (Long) -> Unit,
) {
    var today by remember { mutableIntStateOf(0) }
    LaunchedEffect(task.id, task.repeatCount) {
        today = viewModel.todayCount(task.id)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenComplete(task.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    RoutineDayMask.label(task.daysOfWeekMask),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Streak ${streak}d · Today $today/${task.repeatCount}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = { onOpenComplete(task.id) }) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Complete")
            }
        }
    }
}
