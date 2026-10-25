package com.utilitylab.focussense.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.utilitylab.focussense.R
import com.utilitylab.focussense.ui.screens.AnalyticsScreen
import com.utilitylab.focussense.ui.screens.BestConditionsScreen
import com.utilitylab.focussense.ui.screens.HistoryScreen
import com.utilitylab.focussense.ui.screens.HomeScreen
import com.utilitylab.focussense.ui.screens.ProfileScreen
import com.utilitylab.focussense.ui.screens.ProgressScreen
import com.utilitylab.focussense.ui.screens.SessionScreen

private data class Tab(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int
)

@Composable
fun FocusSenseRoot(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val activeSessionId by vm.activeSessionId.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val tabs = listOf(
        Tab("home", "Home", R.drawable.ic_nav_home),
        Tab("analytics", "Analisi", R.drawable.ic_nav_analytics),
        Tab("history", "Storico", R.drawable.ic_nav_history),
        Tab("progress", "Progressi", R.drawable.ic_nav_progress),
        Tab("profile", "Profilo", R.drawable.ic_nav_profile)
    )

    Scaffold(
        bottomBar = {
            if (route != "session") {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(vm) { nav.navigate("session") }
            }
            composable("analytics") {
                AnalyticsScreen(
                    vm = vm,
                    onOpenBestConditions = { nav.navigate("best_conditions") }
                )
            }
            composable("history") { HistoryScreen(vm) }
            composable("progress") { ProgressScreen(vm) }
            composable("profile") { ProfileScreen(vm) }
            composable("best_conditions") { BestConditionsScreen(vm) }
            composable("session") {
                SessionScreen(vm, activeSessionId) { nav.popBackStack() }
            }
        }
    }
}
