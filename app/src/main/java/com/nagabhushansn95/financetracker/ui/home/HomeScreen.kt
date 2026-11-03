package com.nagabhushansn95.financetracker.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.ui.edit.EditDataScreen
import com.nagabhushansn95.financetracker.ui.extras.ExtrasScreen
import com.nagabhushansn95.financetracker.ui.help.AboutScreen
import com.nagabhushansn95.financetracker.ui.help.HelpScreen
import com.nagabhushansn95.financetracker.ui.investments.HoldingsScreen
import com.nagabhushansn95.financetracker.ui.investments.InstrumentDetailScreen
import com.nagabhushansn95.financetracker.ui.investments.InstrumentListScreen
import com.nagabhushansn95.financetracker.ui.investments.InvestmentRoutes
import com.nagabhushansn95.financetracker.ui.investments.InvestmentsScreen
import com.nagabhushansn95.financetracker.ui.investments.ProvidentFundsScreen
import com.nagabhushansn95.financetracker.ui.investments.TargetsScreen
import com.nagabhushansn95.financetracker.ui.settings.SettingsScreen
import com.nagabhushansn95.financetracker.ui.statistics.StatisticsScreen
import com.nagabhushansn95.financetracker.ui.summary.SummaryScreen
import com.nagabhushansn95.financetracker.ui.transaction.TransactionEditorScreen
import com.nagabhushansn95.financetracker.ui.transactions.TransactionsScreen
import kotlinx.coroutines.launch

/** Top-level destinations, in drawer order (Investments sits between Transactions and Manage by design). */
enum class Destination(val route: String, val title: String) {
    SUMMARY("home", "Home"),
    TRANSACTIONS("transactions", "Transactions"),
    INVESTMENTS("investments", "Investments"),
    EDIT("manage", "Manage"),
    STATISTICS("statistics", "Statistics"),
    EXTRAS("extras", "Extras"),
    SETTINGS("settings", "Settings"),
    HELP("help", "Help"),
    ABOUT("about", "About"),
}

/** Non-drawer routes. */
private const val ROUTE_EDITOR = "transaction?id={id}&kind={kind}&account={account}"
private const val ROUTE_INSTRUMENT = "instrument/{id}"

/** Drawer entries under Investments, each opening one of its screens. */
private val INVESTMENT_SCREENS = listOf(
    "Stocks & funds" to InvestmentRoutes.HOLDINGS,
    "Fixed deposits" to InvestmentRoutes.type(InstrumentType.FIXED_DEPOSIT),
    "Provident funds" to InvestmentRoutes.PROVIDENT_FUNDS,
    "Insurance" to InvestmentRoutes.type(InstrumentType.INSURANCE),
    "Allocation targets" to InvestmentRoutes.TARGETS,
)

private fun editorRoute(id: Long? = null, kind: TransactionKind? = null, accountId: Long? = null) =
    "transaction?id=${id ?: -1}&kind=${kind?.name ?: ""}&account=${accountId ?: -1}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.entries.firstOrNull { d ->
        backStackEntry?.destination?.hierarchy?.any { it.route == d.route } == true
    } ?: Destination.SUMMARY
    val route = backStackEntry?.destination?.route
    // Screens with their own top bar (editor, instrument detail, the investment sub-screens).
    val inEditor =
        route == ROUTE_EDITOR || route == ROUTE_INSTRUMENT || InvestmentRoutes.isInvestmentRoute(
            route
        )

    fun goTo(destination: Destination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Finance Tracker",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                Destination.entries.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.title) },
                        selected = destination == current && !inEditor,
                        onClick = {
                            scope.launch { drawerState.close() }
                            goTo(destination)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    if (destination == Destination.INVESTMENTS) INVESTMENT_SCREENS.forEach { (title, subRoute) ->
                        NavigationDrawerItem(
                            label = { Text(title, style = MaterialTheme.typography.bodyMedium) },
                            selected = route == subRoute || (subRoute.startsWith("investments/type/") && route == InvestmentRoutes.TYPE && backStackEntry?.arguments?.getString(
                                "type"
                            )
                                ?.let { InvestmentRoutes.type(InstrumentType.valueOf(it)) } == subRoute),
                            onClick = {
                                scope.launch { drawerState.close() }
                                goTo(Destination.INVESTMENTS)
                                navController.navigate(subRoute) { launchSingleTop = true }
                            },
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .padding(start = 24.dp),
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (!inEditor) TopAppBar(
                    title = { Text(current.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                )
            },
            floatingActionButton = {
                if (!inEditor && (current == Destination.SUMMARY || current == Destination.TRANSACTIONS)) {
                    FloatingActionButton(onClick = { navController.navigate(editorRoute()) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add transaction")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Destination.SUMMARY.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(Destination.SUMMARY.route) {
                    SummaryScreen(
                        onAccountClick = { navController.navigate(editorRoute(accountId = it)) },
                        onOpenInvestments = { goTo(Destination.INVESTMENTS) },
                    )
                }
                composable(Destination.TRANSACTIONS.route) {
                    TransactionsScreen(
                        onEdit = { navController.navigate(editorRoute(id = it)) },
                        snackbarHostState = snackbarHostState
                    )
                }
                composable(Destination.EXTRAS.route) { ExtrasScreen(snackbarHostState) }
                composable(Destination.EDIT.route) { EditDataScreen(snackbarHostState) }
                composable(Destination.INVESTMENTS.route) {
                    InvestmentsScreen(onNavigate = {
                        navController.navigate(
                            it
                        )
                    })
                }
                composable(InvestmentRoutes.HOLDINGS) {
                    HoldingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenInstrument = { navController.navigate("instrument/$it") })
                }
                composable(
                    InvestmentRoutes.TYPE,
                    arguments = listOf(navArgument("type") { type = NavType.StringType })
                ) { entry ->
                    InstrumentListScreen(
                        type = InstrumentType.valueOf(entry.arguments!!.getString("type")!!),
                        onBack = { navController.popBackStack() },
                        onOpenInstrument = { navController.navigate("instrument/$it") },
                    )
                }
                composable(InvestmentRoutes.PROVIDENT_FUNDS) {
                    ProvidentFundsScreen(
                        onBack = { navController.popBackStack() },
                        onAddToAccount = { navController.navigate(editorRoute(accountId = it)) })
                }
                composable(InvestmentRoutes.TARGETS) { TargetsScreen(onBack = { navController.popBackStack() }) }
                composable(
                    ROUTE_INSTRUMENT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    InstrumentDetailScreen(
                        instrumentId = entry.arguments!!.getLong("id"),
                        onBack = { navController.popBackStack() },
                        onEditTransaction = { navController.navigate(editorRoute(id = it)) },
                    )
                }
                composable(Destination.STATISTICS.route) { StatisticsScreen() }
                composable(Destination.SETTINGS.route) { SettingsScreen() }
                composable(Destination.HELP.route) { HelpScreen() }
                composable(Destination.ABOUT.route) { AboutScreen() }
                composable(
                    ROUTE_EDITOR,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("kind") { type = NavType.StringType; defaultValue = "" },
                        navArgument("account") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                    val kind = entry.arguments?.getString("kind")?.takeIf { it.isNotEmpty() }
                        ?.let { TransactionKind.valueOf(it) }
                    val account = entry.arguments?.getLong("account")?.takeIf { it > 0 }
                    TransactionEditorScreen(
                        id,
                        kind,
                        account,
                        onDone = { navController.popBackStack() })
                }
            }
        }
    }
}
