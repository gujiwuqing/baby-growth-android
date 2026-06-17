package com.baby.growth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baby.growth.ui.home.HomeScreen
import com.baby.growth.ui.growth.GrowthScreen
import com.baby.growth.ui.vaccine.VaccineScreen
import com.baby.growth.ui.settings.SettingsScreen
import com.baby.growth.ui.records.RecordsScreen
import com.baby.growth.ui.profile.ProfileScreen
import com.baby.growth.ui.record.*
import com.baby.growth.ui.theme.BabyGrowthTheme
import com.baby.growth.utils.ThemeManager

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : Screen("home", "首页", Icons.Filled.Home, Icons.Outlined.Home)
    data object Records : Screen("records", "记录", Icons.Filled.ListAlt, Icons.Outlined.ListAlt)
    data object Growth : Screen("growth", "成长", Icons.Filled.MonitorWeight, Icons.Outlined.MonitorWeight)
    data object Vaccine : Screen("vaccine", "疫苗", Icons.Filled.Vaccines, Icons.Outlined.Vaccines)
    data object Settings : Screen("settings", "我的", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(Screen.Home, Screen.Records, Screen.Growth, Screen.Vaccine, Screen.Settings)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeKey by ThemeManager.selectedThemeKey(this)
            val darkMode by ThemeManager.darkModeState(this)
            BabyGrowthTheme(themeKey = themeKey, darkMode = darkMode) {
                val initialRoute = intent?.getStringExtra("route")
                MainScreen(initialRoute = initialRoute)
            }
        }
    }
}

@Composable
fun MainScreen(initialRoute: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            navController.navigate(initialRoute)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) },
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Records.route) { RecordsScreen(navController) }
            composable(Screen.Growth.route) { GrowthScreen(navController) }
            composable(Screen.Vaccine.route) { VaccineScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            // 子页面使用横向滑入/滑出动画
            val slideEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            }
            val slideExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            }
            val slidePopEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            }
            val slidePopExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            }
            composable("record/feeding", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { FeedingRecordScreen(navController) }
            composable("record/diaper", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { DiaperRecordScreen(navController) }
            composable("record/sleep", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { SleepRecordScreen(navController) }
            composable("record/food", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { FoodRecordScreen(navController) }
            composable("record/supplement", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { SupplementRecordScreen(navController) }
            composable("record/growth", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { GrowthRecordScreen(navController) }
            // 编辑路由
            composable("record/feeding/edit/{id}", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                FeedingRecordScreen(navController, editId = editId)
            }
            composable("record/diaper/edit/{id}", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                DiaperRecordScreen(navController, editId = editId)
            }
            composable("record/sleep/edit/{id}", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                SleepRecordScreen(navController, editId = editId)
            }
            composable("record/food/edit/{id}", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                FoodRecordScreen(navController, editId = editId)
            }
            composable("record/supplement/edit/{id}", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                SupplementRecordScreen(navController, editId = editId)
            }
            composable("record/growth/edit/{id}", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                GrowthRecordScreen(navController, editId = editId)
            }
            composable("profile", enterTransition = slideEnter, exitTransition = slideExit, popEnterTransition = slidePopEnter, popExitTransition = slidePopExit) { ProfileScreen(navController) }
        }
    }
}