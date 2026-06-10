package com.baby.growth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Filled.Home)
    data object Records : Screen("records", "记录", Icons.Filled.ListAlt)
    data object Growth : Screen("growth", "身高体重", Icons.Filled.MonitorWeight)
    data object Vaccine : Screen("vaccine", "疫苗接种", Icons.Filled.Vaccines)
    data object Settings : Screen("settings", "我的", Icons.Filled.Settings)
}

val bottomNavItems = listOf(Screen.Home, Screen.Records, Screen.Growth, Screen.Vaccine, Screen.Settings)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeKey by ThemeManager.selectedThemeKey(this)
            BabyGrowthTheme(themeKey = themeKey) {
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

    // 从通知点击进入时，导航到对应记录页面
    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            navController.navigate(initialRoute)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Records.route) { RecordsScreen(navController) }
            composable(Screen.Growth.route) { GrowthScreen(navController) }
            composable(Screen.Vaccine.route) { VaccineScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable("record/feeding") { FeedingRecordScreen(navController) }
            composable("record/diaper") { DiaperRecordScreen(navController) }
            composable("record/sleep") { SleepRecordScreen(navController) }
            composable("record/food") { FoodRecordScreen(navController) }
            composable("record/supplement") { SupplementRecordScreen(navController) }
            composable("record/growth") { GrowthRecordScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
        }
    }
}