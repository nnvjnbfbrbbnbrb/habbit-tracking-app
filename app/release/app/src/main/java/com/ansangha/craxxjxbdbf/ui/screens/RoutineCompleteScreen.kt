package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.repository.RoutineRepository
import com.ansangha.craxxjxbdbf.ui.viewmodel.RoutineCompleteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCompleteScreen(
    onDone: () -> Unit,
    viewModel: RoutineCompleteViewModel = hiltViewModel(),
) {
    val name by viewModel.taskName.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val scale by animateFloatAsState(
        targetValue = if (result is RoutineRepository.CompleteResult.Success) 1.08f else 1f,
        animationSpec = tween(400),
        label = "celebrate",
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Complete routine") }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name.ifEmpty { "Routine" },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
            Text(
                text = when (val r = result) {
                    is RoutineRepository.CompleteResult.Success -> {
                        if (r.leveledUp) "Level up! Now level ${r.newLevel}" else "Nice — XP added."
                    }
                    RoutineRepository.CompleteResult.AlreadyDone -> "Already finished for today."
                    RoutineRepository.CompleteResult.Disabled -> "This routine is disabled."
                    RoutineRepository.CompleteResult.TaskMissing -> "Routine not found."
                    null -> "Tap complete to log today."
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = { viewModel.markComplete() },
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text("Mark complete")
            }
            Button(onClick = onDone, modifier = Modifier.padding(top = 12.dp)) {
                Text("Close")
            }
        }
    }
}
