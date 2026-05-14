package com.ansangha.craxxjxbdbf.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.ui.screens.AchievementsScreen
import com.ansangha.craxxjxbdbf.ui.screens.AddHabitScreen
import com.ansangha.craxxjxbdbf.ui.screens.HabitDetailScreen
import com.ansangha.craxxjxbdbf.ui.screens.HabitListScreen
import com.ansangha.craxxjxbdbf.ui.screens.InsightsScreen
import com.ansangha.craxxjxbdbf.ui.screens.OnboardingScreen
import com.ansangha.craxxjxbdbf.ui.screens.ProfileScreen
import com.ansangha.craxxjxbdbf.MainActivity
import com.ansangha.craxxjxbdbf.ui.screens.RoutineCompleteScreen
import com.ansangha.craxxjxbdbf.ui.screens.RoutineEditScreen
import com.ansangha.craxxjxbdbf.ui.screens.RoutineListScreen
import com.ansangha.craxxjxbdbf.ui.screens.SettingsScreen
import com.ansangha.craxxjxbdbf.ui.screens.SplashDestination
import com.ansangha.craxxjxbdbf.ui.screens.SplashScreen
import com.ansangha.craxxjxbdbf.ui.theme.AppDimens
import com.ansangha.craxxjxbdbf.ui.viewmodel.OnboardingViewModel

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val HABITS = "habit_list"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val ADD_HABIT = "add_habit"
    const val HABIT_DETAIL = "habit_detail"
    const val ROUTINES = "daily_routines"
    const val PROFILE = "profile"
    const val ACHIEVEMENTS = "achievements"
}

@Composable
fun HabitTrackerRootNavigation() {
    val rootNav = rememberNavController()
    val activity = LocalContext.current as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(rootNav, activity, lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val id = activity?.consumeRoutineDeepLink() ?: return@LifecycleEventObserver
                if (id >= 0L) {
                    rootNav.navigate("routine_complete/$id") { launchSingleTop = true }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val onboardingVm: OnboardingViewModel = hiltViewModel()
    val onboarded by onboardingVm.onboardingComplete.collectAsStateWithLifecycle()

    NavHost(
        navController = rootNav,
        startDestination = Routes.SPLASH,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(
            route = Routes.SPLASH,
            enterTransition = { fadeIn(tween(320)) },
            exitTransition = { fadeOut(tween(240)) },
        ) {
            SplashScreen(
                onboardingComplete = onboarded,
                onFinished = { dest ->
                    when (dest) {
                        SplashDestination.Main -> rootNav.navigate(Routes.MAIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                            launchSingleTop = true
                        }

                        SplashDestination.Onboarding -> rootNav.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(
            route = Routes.ONBOARDING,
            enterTransition = { slideInHorizontally { it / 6 } + fadeIn(tween(280)) },
            exitTransition = { slideOutHorizontally { -it / 8 } + fadeOut(tween(220)) },
        ) {
            OnboardingScreen(
                onFinished = {
                    rootNav.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.MAIN,
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(200)) },
        ) {
            HabitMainShell(rootNav = rootNav)
        }

        composable(
            route = "routine_edit/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType; defaultValue = 0L }),
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(260)) },
            popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn(tween(260)) },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) },
        ) {
            RoutineEditScreen(
                onBack = { rootNav.popBackStack() },
                onSaved = { rootNav.popBackStack() },
            )
        }

        composable(
            route = "routine_complete/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(260)) },
            popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn(tween(260)) },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) },
        ) {
            RoutineCompleteScreen(onDone = { rootNav.popBackStack() })
        }

        composable(
            route = Routes.ADD_HABIT,
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(260)) },
            popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn(tween(260)) },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) },
        ) {
            AddHabitScreen(
                onBack = { rootNav.popBackStack() },
                onSave = { rootNav.popBackStack() },
            )
        }

        composable(
            route = "${Routes.HABIT_DETAIL}/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(280)) },
            popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn(tween(260)) },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) },
        ) { entry ->
            val habitId = entry.arguments?.getString("habitId").orEmpty()
            HabitDetailScreen(
                habitId = habitId,
                onBack = { rootNav.popBackStack() },
            )
        }

        composable(
            route = Routes.PROFILE,
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(280)) },
            popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn(tween(260)) },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) },
        ) {
            ProfileScreen(onBack = { rootNav.popBackStack() })
        }

        composable(
            route = Routes.ACHIEVEMENTS,
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(280)) },
            popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn(tween(260)) },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) },
        ) {
            AchievementsScreen(onBack = { rootNav.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitMainShell(rootNav: NavHostController) {
    val tabNav = rememberNavController()
    val backStackEntry by tabNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HABITS
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (currentRoute == Routes.HABITS) {
                ExtendedFloatingActionButton(
                    onClick = { rootNav.navigate(Routes.ADD_HABIT) },
                    shape = RoundedCornerShape(AppDimens.fabCorner),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    text = { Text("New habit") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                        )
                    },
                )
            } else if (currentRoute == Routes.ROUTINES) {
                ExtendedFloatingActionButton(
                    onClick = { rootNav.navigate("routine_edit/0") },
                    shape = RoundedCornerShape(AppDimens.fabCorner),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    text = { Text("New routine") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                        )
                    },
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                tonalElevation = 0.dp,
            ) {
                val items = listOf(
                    Triple(Routes.HABITS, Icons.Rounded.AutoAwesome, R.string.nav_flow),
                    Triple(Routes.ROUTINES, Icons.Rounded.EventAvailable, R.string.nav_routines),
                    Triple(Routes.INSIGHTS, Icons.Rounded.QueryStats, R.string.nav_pulse),
                    Triple(Routes.SETTINGS, Icons.Rounded.Tune, R.string.nav_studio),
                )
                items.forEach { (route, icon, labelRes) ->
                    val selected = currentRoute == route
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = tween(220),
                        label = "navIcon",
                    )
                    val lift = with(density) { 2.dp.toPx() }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNav.navigate(route) {
                                popUpTo(tabNav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = if (selected) -lift else 0f
                                },
                            )
                        },
                        label = { Text(stringResource(labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNav,
            startDestination = Routes.HABITS,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Routes.HABITS) {
                HabitListScreen(
                    onOpenHabit = { id ->
                        rootNav.navigate("${Routes.HABIT_DETAIL}/$id")
                    },
                )
            }
            composable(Routes.ROUTINES) {
                RoutineListScreen(
                    onAddRoutine = { rootNav.navigate("routine_edit/0") },
                    onOpenComplete = { id -> rootNav.navigate("routine_complete/$id") },
                )
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen(
                    onOpenAchievements = { rootNav.navigate(Routes.ACHIEVEMENTS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenProfile = { rootNav.navigate(Routes.PROFILE) },
                    onOpenAchievements = { rootNav.navigate(Routes.ACHIEVEMENTS) },
                )
            }
        }
    }
}

/** Entry for legacy call sites (permissions bootstrap). */
@Composable
fun HabitTrackerNavigation() {
    HabitTrackerRootNavigation()
}
