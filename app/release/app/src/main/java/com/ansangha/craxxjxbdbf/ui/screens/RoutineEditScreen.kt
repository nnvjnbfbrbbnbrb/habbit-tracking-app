package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.domain.RoutineDayMask
import com.ansangha.craxxjxbdbf.ui.viewmodel.RoutineEditViewModel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RoutineEditViewModel = hiltViewModel(),
) {
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var repeat by remember { mutableIntStateOf(1) }
    var daysMask by remember { mutableIntStateOf(RoutineDayMask.defaultWeekdayMask()) }
    var enabled by remember { mutableStateOf(true) }
    var grace by remember { mutableIntStateOf(15) }

    LaunchedEffect(existing) {
        val e = existing ?: return@LaunchedEffect
        name = e.name
        hour = e.timeMinutesFromMidnight / 60
        minute = e.timeMinutesFromMidnight % 60
        repeat = e.repeatCount
        daysMask = e.daysOfWeekMask
        enabled = e.enabled
        grace = e.graceMinutes
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New routine" else "Edit routine") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(hour.toString(), { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour }, label = { Text("Hour") })
                OutlinedTextField(minute.toString(), { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute }, label = { Text("Min") })
            }
            OutlinedTextField(repeat.toString(), { repeat = it.toIntOrNull()?.coerceIn(1, 12) ?: repeat }, label = { Text("Repeats / day") })
            Text("Days", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { d ->
                    val bit = RoutineDayMask.bitFor(d)
                    val on = (daysMask and bit) != 0
                    FilterChip(
                        selected = on,
                        onClick = { daysMask = daysMask xor bit },
                        label = { Text(d.name.take(3)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            OutlinedTextField(grace.toString(), { grace = it.toIntOrNull()?.coerceIn(5, 120) ?: grace }, label = { Text("Grace (min)") })
            androidx.compose.material3.Button(
                onClick = {
                    viewModel.save(name, hour, minute, repeat, daysMask, enabled, grace, onSaved)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            if (existing != null) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Button(onClick = { viewModel.delete(onSaved) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete")
                }
            }
        }
    }
}
