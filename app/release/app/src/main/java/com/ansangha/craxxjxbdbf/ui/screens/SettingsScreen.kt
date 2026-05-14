package com.ansangha.craxxjxbdbf.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.data.preferences.DarkThemePreference
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.ui.components.AuroraBackground
import com.ansangha.craxxjxbdbf.ui.components.GradientGlassBox
import com.ansangha.craxxjxbdbf.ui.theme.CustomTypography
import com.ansangha.craxxjxbdbf.ui.theme.Typography
import com.ansangha.craxxjxbdbf.ui.viewmodel.FamilySafetyViewModel
import com.ansangha.craxxjxbdbf.ui.viewmodel.ThemeViewModel
import com.ansangha.craxxjxbdbf.ui.viewmodel.WellnessViewModel

@Composable
fun SettingsScreen(
    onOpenProfile: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    viewModel: ThemeViewModel = hiltViewModel(),
    familySafetyViewModel: FamilySafetyViewModel = hiltViewModel(),
    wellnessViewModel: WellnessViewModel = hiltViewModel(),
) {
    val darkPref by viewModel.darkThemePreference.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
    val routineReminders by viewModel.routineRemindersEnabled.collectAsStateWithLifecycle()
    val shareLocationWithParent by familySafetyViewModel.shareLocationWithParent.collectAsStateWithLifecycle()
    val usageTodayText by familySafetyViewModel.usageTodayText.collectAsStateWithLifecycle()
    val sleepBed by wellnessViewModel.sleepBedHour.collectAsStateWithLifecycle()
    val sleepWake by wellnessViewModel.sleepWakeHour.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        familySafetyViewModel.onPostNotificationsPermissionResult(granted)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        familySafetyViewModel.onFineLocationPermissionResult(granted) {
            postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                familySafetyViewModel.syncLocationServiceWithPreference()
                familySafetyViewModel.refreshUsageTodayText()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* optional: denied — worker still runs quietly */ }

    val needsPostNotifications = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Studio",
                style = CustomTypography.HeroTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = context.getString(R.string.settings_subtitle),
                style = Typography.bodyLarge,
                color = Color.White.copy(alpha = 0.88f),
            )

            GradientGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfile),
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Profile & XP",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Momentum ring, level path, and quiet bragging rights.",
                            style = Typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            GradientGlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAchievements),
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Achievements",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Trophy shelf with unlock bounce animations.",
                            style = Typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = "Sleep window (optional)",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Used for sleep summaries in analytics/Telegram (guidance only).",
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { wellnessViewModel.setSleepWindow(23, 7) }) {
                            Text("Typical 23h → 7h")
                        }
                        Button(onClick = { wellnessViewModel.setSleepWindow(null, null) }) {
                            Text("Clear")
                        }
                    }
                    Text(
                        text = "Current: bed=${if (sleepBed < 0) "unset" else "${sleepBed}h"}, wake=${if (sleepWake < 0) "unset" else "${sleepWake}h"}",
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            text = "Appearance",
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Dark mode",
                        style = Typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FilterChip(
                            selected = darkPref == DarkThemePreference.FollowSystem,
                            onClick = { viewModel.setDarkThemePreference(DarkThemePreference.FollowSystem) },
                            label = { Text("System") },
                        )
                        FilterChip(
                            selected = darkPref == DarkThemePreference.On,
                            onClick = { viewModel.setDarkThemePreference(DarkThemePreference.On) },
                            label = { Text("Dark") },
                        )
                        FilterChip(
                            selected = darkPref == DarkThemePreference.Off,
                            onClick = { viewModel.setDarkThemePreference(DarkThemePreference.Off) },
                            label = { Text("Light") },
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Material You colors",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Uses wallpaper palette on Android 12+ when available.",
                                style = Typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                        )
                    }
                }
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            text = context.getString(R.string.settings_routines_section_title),
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = context.getString(R.string.settings_routines_section_body),
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.settings_routine_reminders_title),
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = context.getString(R.string.settings_routine_reminders_hint),
                                style = Typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = routineReminders,
                            onCheckedChange = { enabled ->
                                viewModel.setRoutineRemindersEnabled(enabled)
                                if (enabled && needsPostNotifications) {
                                    notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        )
                    }
                }
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            text = stringResource(R.string.settings_family_section_title),
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_family_section_body),
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_family_share_location_title),
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.settings_family_share_location_hint),
                                style = Typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = shareLocationWithParent,
                            onCheckedChange = { enabled ->
                                familySafetyViewModel.onShareLocationToggle(
                                    enabled = enabled,
                                    onRequestFineLocation = {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    },
                                    onRequestPostNotifications = {
                                        postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                )
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.settings_family_usage_heading),
                        style = Typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_family_usage_access_button))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { familySafetyViewModel.refreshUsageTodayText() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_family_usage_refresh))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = usageTodayText.ifBlank {
                            if (familySafetyViewModel.usageAccessGranted) {
                                stringResource(R.string.settings_family_usage_empty_hint)
                            } else {
                                stringResource(R.string.settings_family_usage_need_access_hint)
                            }
                        },
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                }
            }

            GradientGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                containerAlpha = 0.55f,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "HabitPro",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Build streaks, track categories, and keep momentum in one cinematic flow.",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}
