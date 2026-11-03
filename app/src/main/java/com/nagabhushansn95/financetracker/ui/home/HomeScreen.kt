package com.nagabhushansn95.financetracker.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.ui.edit.EditDataScreen
import com.nagabhushansn95.financetracker.ui.extras.ExtrasScreen
import com.nagabhushansn95.financetracker.ui.investments.InvestmentsScreen
import com.nagabhushansn95.financetracker.ui.statistics.StatisticsScreen
import com.nagabhushansn95.financetracker.ui.summary.SummaryScreen
import com.nagabhushansn95.financetracker.ui.transaction.TransactionEditorScreen
import com.nagabhushansn95.financetracker.ui.transactions.TransactionsScreen
import kotlinx.coroutines.launch

/** Top-level destinations, in drawer order (Investments sits between Transactions and Manage by design). */
enum class Destination(val route: String, val title: String) {
    SUMMARY("summary", "Summary"),
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
    val inEditor = backStackEntry?.destination?.route == ROUTE_EDITOR

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
                        selected = destination == current,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
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
                    SummaryScreen(onAccountClick = { navController.navigate(editorRoute(accountId = it)) })
                }
                composable(Destination.TRANSACTIONS.route) {
                    TransactionsScreen(
                        onEdit = { navController.navigate(editorRoute(id = it)) },
                        snackbarHostState = snackbarHostState
                    )
                }
                composable(Destination.EXTRAS.route) { ExtrasScreen(snackbarHostState) }
                composable(Destination.EDIT.route) { EditDataScreen(snackbarHostState) }
                composable(Destination.INVESTMENTS.route) { InvestmentsScreen() }
                composable(Destination.STATISTICS.route) { StatisticsScreen() }
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
                Destination.entries
                    .filter { it != Destination.SUMMARY && it != Destination.EXTRAS && it != Destination.TRANSACTIONS && it != Destination.EDIT && it != Destination.INVESTMENTS && it != Destination.STATISTICS }
                    .forEach { destination ->
                        composable(destination.route) { ComingSoon(destination.title) }
                    }
            }
        }
    }
}

@Composable
private fun ComingSoon(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "This screen is being rebuilt for 5.0.0.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
