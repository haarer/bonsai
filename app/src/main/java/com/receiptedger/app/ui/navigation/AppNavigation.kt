package com.receiptedger.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.receiptedger.app.R
import com.receiptedger.app.ui.ledger.LedgerScreen
import com.receiptedger.app.ui.review.ReviewScreen
import com.receiptedger.app.ui.scan.ScanScreen
import com.receiptedger.app.ui.settings.SettingsScreen

object AppRoutes {
    const val SCAN = "scan"
    const val LEDGER = "ledger"
    const val SETTINGS = "settings"

    const val REVIEW_NEW = "review/new"
    const val REVIEW = "review/{receiptId}"

    fun review(receiptId: Long): String = "review/$receiptId"
}

private enum class BottomTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    SCAN(AppRoutes.SCAN, R.string.nav_scan, Icons.Filled.Add),
    LEDGER(AppRoutes.LEDGER, R.string.nav_ledger, Icons.Filled.ShoppingCart),
    SETTINGS(AppRoutes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(AppRoutes.SCAN, AppRoutes.LEDGER, AppRoutes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.SCAN,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppRoutes.SCAN) {
                ScanScreen(
                    onNewReceipt = {
                        navController.navigate(AppRoutes.REVIEW_NEW)
                    },
                )
            }
            composable(AppRoutes.REVIEW_NEW) {
                ReviewScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate(AppRoutes.LEDGER) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                route = AppRoutes.REVIEW,
                arguments = listOf(
                    navArgument("receiptId") { type = NavType.LongType }
                ),
            ) {
                ReviewScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.LEDGER) {
                LedgerScreen(onOpenReceipt = { receiptId ->
                    navController.navigate(AppRoutes.review(receiptId))
                })
            }
            composable(AppRoutes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
