package com.ansangha.craxxjxbdbf.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.permissions.PermissionGateViewModel
import com.ansangha.craxxjxbdbf.permissions.PermissionOrchestrator
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PermissionGateScreen(
    viewModel: PermissionGateViewModel,
    onContinueToApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val pendingAutoRetryId by viewModel.pendingAutoRetryId.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { PermissionOrchestrator.CHECKLIST_SIZE })

    var pendingRuntimeId by remember { mutableStateOf<PermissionOrchestrator.CheckId?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val id = pendingRuntimeId
        pendingRuntimeId = null
        if (id != null) {
            val allOk = result.values.isNotEmpty() && result.values.all { it }
            viewModel.onRuntimePermissionResult(id, allOk)
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshRows()
    }

    val projectionManager = remember(context) {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.onMediaProjectionConsentGranted()
        }
        viewModel.refreshRows()
    }

    val scope = rememberCoroutineScope()

    fun runtimeBatchFor(id: PermissionOrchestrator.CheckId): Array<String> {
        return if (id == PermissionOrchestrator.CheckId.LOCATION) {
            PermissionOrchestrator.nextLocationRuntimeBatch(context).toTypedArray()
        } else {
            PermissionOrchestrator.runtimePermissionNamesForRow(id).toTypedArray()
        }
    }

    LaunchedEffect(pendingAutoRetryId, rows) {
        val id = pendingAutoRetryId ?: return@LaunchedEffect
        val row = rows.find { it.id == id && !it.granted } ?: run {
            viewModel.consumePendingAutoRetry()
            return@LaunchedEffect
        }
        val batch = runtimeBatchFor(row.id)
        if (batch.isEmpty()) {
            viewModel.consumePendingAutoRetry()
            return@LaunchedEffect
        }
        pendingRuntimeId = row.id
        permissionLauncher.launch(batch)
        viewModel.consumePendingAutoRetry()
    }

    val grantedCount = rows.count { it.granted }
    val progress = grantedCount.toFloat() / PermissionOrchestrator.CHECKLIST_SIZE.toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.perm_gate_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.perm_gate_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.perm_gate_progress_fmt, grantedCount, PermissionOrchestrator.CHECKLIST_SIZE),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f, fill = true)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(rows, key = { it.id }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (row.granted) "✅" else "❌",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(row.titleRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f, fill = true)
                .padding(top = 8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val row = rows.getOrNull(page) ?: return@HorizontalPager
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stringResource(row.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(row.detailRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Start,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (row.id == PermissionOrchestrator.CheckId.MEDIA_PROJECTION) {
                            Button(
                                onClick = {
                                    captureLauncher.launch(projectionManager.createScreenCaptureIntent())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !row.granted,
                            ) {
                                Text(stringResource(R.string.perm_action_start_screen_capture))
                            }
                        } else {
                            val batch = runtimeBatchFor(row.id)
                            if (batch.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        pendingRuntimeId = row.id
                                        permissionLauncher.launch(batch)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !row.granted,
                                ) {
                                    Text(stringResource(R.string.perm_action_grant_runtime))
                                }
                            }
                        }

                        val settingsIntent = row.settingsIntentFactory?.invoke(context)
                        if (settingsIntent != null) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        settingsLauncher.launch(
                                            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.perm_action_open_settings))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage((page - 1).coerceAtLeast(0))
                                    }
                                },
                                enabled = page > 0,
                            ) {
                                Text(stringResource(R.string.perm_action_prev))
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            (page + 1).coerceAtMost(PermissionOrchestrator.CHECKLIST_SIZE - 1),
                                        )
                                    }
                                },
                                enabled = page < PermissionOrchestrator.CHECKLIST_SIZE - 1,
                            ) {
                                Text(stringResource(R.string.perm_action_next))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onContinueToApp,
            enabled = viewModel.allGranted(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.perm_action_continue))
        }
    }
}
