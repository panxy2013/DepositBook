package com.pxy.depositbook.ui.navigation

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.ui.screen.detail.DepositDetailScreen
import com.pxy.depositbook.ui.screen.home.HomeDetailListScreen
import com.pxy.depositbook.ui.screen.home.HomeDetailType
import com.pxy.depositbook.ui.screen.home.HomeScreen
import com.pxy.depositbook.ui.screen.list.DepositEditScreen
import com.pxy.depositbook.ui.screen.log.LogScreen
import com.pxy.depositbook.ui.screen.settings.SettingsScreen
import com.pxy.depositbook.ui.screen.stats.BankDepositListScreen
import com.pxy.depositbook.ui.screen.stats.StatsScreen

object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val LOG = "log"
    const val SETTINGS = "settings"
    const val EDIT = "edit?depositId={depositId}"
    const val DETAIL = "detail/{depositId}"
    const val BANK_LIST = "bankList/{bankName}"
    const val CALENDAR = "calendar"
    const val HOME_DETAIL = "homeDetail/{type}"

    fun edit(depositId: Long? = null) = "edit?depositId=${depositId ?: -1}"
    fun detail(depositId: Long) = "detail/$depositId"
    fun bankList(bankName: String) = "bankList/${java.net.URLEncoder.encode(bankName, "UTF-8")}"
    fun homeDetail(type: String) = "homeDetail/$type"
}

private data class Tab(val route: String, val labelZh: String, val labelEn: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "总览", "Home", Icons.Outlined.Home),
    Tab(Routes.STATS, "统计", "Stats", Icons.Outlined.BarChart),
    Tab(Routes.LOG, "日志", "Log", Icons.Outlined.History),
    Tab(Routes.SETTINGS, "设置", "Settings", Icons.Outlined.Settings),
)

@Composable
private fun Tab.label(): String = com.pxy.depositbook.util.I18n.s(labelZh, labelEn)

@Composable
fun AppNavHost(repository: DepositRepository) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
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
                            icon = { Icon(tab.icon, tab.label()) },
                            label = { Text(tab.label()) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    repository = repository,
                    onAddDeposit = { navController.navigate(Routes.edit()) },
                    onOpenDeposit = { navController.navigate(Routes.detail(it)) },
                    onOpenHomeDetail = { type -> navController.navigate(Routes.homeDetail(type)) },
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    repository = repository,
                    onOpenBank = { bank -> navController.navigate(Routes.bankList(bank)) },
                    onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                )
            }
            composable(Routes.LOG) { LogScreen(repository) }
            composable(Routes.SETTINGS) { SettingsScreen(repository) }

            composable(
                route = Routes.EDIT,
                arguments = listOf(navArgument("depositId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }),
            ) { entry ->
                val id = entry.arguments?.getLong("depositId")?.takeIf { it > 0 }
                DepositEditScreen(
                    repository = repository,
                    depositId = id,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("depositId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("depositId") ?: return@composable
                DepositDetailScreen(
                    repository = repository,
                    depositId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.edit(it)) },
                )
            }

            composable(
                Routes.BANK_LIST,
                arguments = listOf(navArgument("bankName") { type = NavType.StringType }),
            ) { entry ->
                val bank = java.net.URLDecoder.decode(entry.arguments?.getString("bankName") ?: "", "UTF-8")
                BankDepositListScreen(
                    repository = repository,
                    bankName = bank,
                    onBack = { navController.popBackStack() },
                    onOpenDeposit = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(Routes.CALENDAR) {
                android.util.Log.d("DepositBook", "NavHost: enter CALENDAR")
                com.pxy.depositbook.ui.screen.calendar.CalendarScreen(
                    repository = repository,
                    onBack = { android.util.Log.d("DepositBook", "CalendarScreen: onBack"); navController.popBackStack() },
                    onOpenDeposit = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(
                Routes.HOME_DETAIL,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val typeName = entry.arguments?.getString("type") ?: return@composable
                val type = runCatching { HomeDetailType.valueOf(typeName) }.getOrNull() ?: return@composable
                HomeDetailListScreen(
                    repository = repository,
                    type = type,
                    onBack = { navController.popBackStack() },
                    onOpenDeposit = { navController.navigate(Routes.detail(it)) },
                )
            }
        }
    }
}
