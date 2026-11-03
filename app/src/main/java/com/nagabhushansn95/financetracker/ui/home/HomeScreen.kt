package com.nagabhushansn95.financetracker.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nagabhushansn95.financetracker.ui.extras.ExtrasScreen
import com.nagabhushansn95.financetracker.ui.summary.SummaryScreen
import kotlinx.coroutines.launch

/** Top-level destinations, in drawer order (Investments sits between Transactions and Edit Data by design). */
enum class Destination(val route: String, val title: String) {
    SUMMARY("summary", "Summary"),
    TRANSACTIONS("transactions", "Transactions"),
    INVESTMENTS("investments", "Investments"),
    EDIT("edit", "Edit Data"),
    STATISTICS("statistics", "Statistics"),
    EXTRAS("extras", "Extras"),
    SETTINGS("settings", "Settings"),
    HELP("help", "Help"),
    ABOUT("about", "About"),
}

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
                TopAppBar(
                    title = { Text(current.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Destination.SUMMARY.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(Destination.SUMMARY.route) { SummaryScreen() }
                composable(Destination.EXTRAS.route) { ExtrasScreen(snackbarHostState) }
                Destination.entries
                    .filter { it != Destination.SUMMARY && it != Destination.EXTRAS }
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
