package com.utilitylab.focussense.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.utilitylab.focussense.R
import com.utilitylab.focussense.ui.components.FocuSenseBackdrop
import com.utilitylab.focussense.ui.screens.AnalyticsScreen
import com.utilitylab.focussense.ui.screens.BestConditionsScreen
import com.utilitylab.focussense.ui.screens.HistoryScreen
import com.utilitylab.focussense.ui.screens.HomeScreen
import com.utilitylab.focussense.ui.screens.ProfileScreen
import com.utilitylab.focussense.ui.screens.ProgressScreen
import com.utilitylab.focussense.ui.screens.SessionScreen
import com.utilitylab.focussense.ui.theme.FocuSenseCardBottom
import com.utilitylab.focussense.ui.theme.FocuSenseCyan
import com.utilitylab.focussense.ui.theme.FocuSenseOutline
import com.utilitylab.focussense.ui.theme.FocuSenseTextMuted

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

    FocuSenseBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = FocuSenseTextMuted,
            bottomBar = {
                if (route != "session") {
                    FocuSenseBottomBar(
                        tabs = tabs,
                        selectedRoute = route,
                        onSelect = { target ->
                            nav.navigate(target) {
                                popUpTo(nav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
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
}

@Composable
private fun FocuSenseBottomBar(
    tabs: List<Tab>,
    selectedRoute: String?,
    onSelect: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(FocuSenseCardBottom.copy(alpha = 0.96f))
            .border(1.dp, FocuSenseOutline.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            tabs.forEach { tab ->
                val selected = selectedRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(tab.route) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) FocuSenseCyan.copy(alpha = 0.14f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = tab.label,
                            tint = if (selected) FocuSenseCyan else FocuSenseTextMuted
                        )
                    }
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) FocuSenseCyan else FocuSenseTextMuted
                    )
                }
            }
        }
    }
}
