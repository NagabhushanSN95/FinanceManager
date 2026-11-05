package com.nagabhushansn95.financetracker.ui.home

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.nagabhushansn95.financetracker.data.investments.InvestmentGroup
import com.nagabhushansn95.financetracker.data.investments.InvestmentOrder
import com.nagabhushansn95.financetracker.ui.common.InvestmentsLock
import com.nagabhushansn95.financetracker.ui.common.LocalOpenDrawer
import com.nagabhushansn95.financetracker.ui.edit.EditDataScreen
import com.nagabhushansn95.financetracker.ui.edit.MANAGE_TABS
import com.nagabhushansn95.financetracker.ui.extras.ExtrasScreen
import com.nagabhushansn95.financetracker.ui.help.AboutScreen
import com.nagabhushansn95.financetracker.ui.help.HelpScreen
import com.nagabhushansn95.financetracker.ui.investments.FixedDepositDetailScreen
import com.nagabhushansn95.financetracker.ui.investments.FixedDepositsScreen
import com.nagabhushansn95.financetracker.ui.investments.HoldingsScreen
import com.nagabhushansn95.financetracker.ui.investments.InstrumentDetailScreen
import com.nagabhushansn95.financetracker.ui.investments.InstrumentListScreen
import com.nagabhushansn95.financetracker.ui.investments.InsurancePolicyScreen
import com.nagabhushansn95.financetracker.ui.investments.InsuranceScreen
import com.nagabhushansn95.financetracker.ui.investments.InvestmentRoutes
import com.nagabhushansn95.financetracker.ui.investments.InvestmentsScreen
import com.nagabhushansn95.financetracker.ui.investments.ProvidentFundAccountScreen
import com.nagabhushansn95.financetracker.ui.investments.ProvidentFundsScreen
import com.nagabhushansn95.financetracker.ui.investments.SavingsScreen
import com.nagabhushansn95.financetracker.ui.investments.TargetsScreen
import com.nagabhushansn95.financetracker.ui.settings.SettingsScreen
import com.nagabhushansn95.financetracker.ui.statistics.StatisticsScreen
import com.nagabhushansn95.financetracker.ui.summary.SummaryScreen
import com.nagabhushansn95.financetracker.ui.transaction.TransactionEditorScreen
import com.nagabhushansn95.financetracker.ui.transactions.TransactionsScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

/** Manage, with the tab the drawer's sub-entries open. */
private const val ROUTE_MANAGE = "manage?tab={tab}"
private fun manageRoute(tab: Int) = "manage?tab=$tab"

/** Non-drawer routes. */
private const val ROUTE_EDITOR =
    "transaction?id={id}&kind={kind}&account={account}&instrument={instrument}"
private const val ROUTE_INSTRUMENT = "instrument/{id}"

/** Drawer entries under Investments, one per group, in the order of the categories under Manage. */
private fun investmentScreens(categoryNames: List<String>): List<Pair<String, String>> =
    InvestmentOrder.groups(categoryNames).map { group ->
        when (group) {
            InvestmentGroup.FIXED_DEPOSITS -> "Fixed deposits" to InvestmentRoutes.FIXED_DEPOSITS
            InvestmentGroup.PROVIDENT_FUNDS -> "Provident funds" to InvestmentRoutes.PROVIDENT_FUNDS
            InvestmentGroup.INSURANCE -> "Insurance" to InvestmentRoutes.INSURANCE
            InvestmentGroup.STOCKS_AND_FUNDS -> "Stocks & funds" to InvestmentRoutes.HOLDINGS
            InvestmentGroup.OTHERS -> "Others" to InvestmentRoutes.type(InstrumentType.OTHER)
        }
    } + listOf(
        "Categories & targets" to InvestmentRoutes.TARGETS,
        "Savings" to InvestmentRoutes.SAVINGS,
    )

/** The drawer's only data need: the top-level investment categories in display order. */
class DrawerViewModel(application: android.app.Application) :
    androidx.lifecycle.AndroidViewModel(application) {
    val categoryNames: kotlinx.coroutines.flow.StateFlow<List<String>> =
        com.nagabhushansn95.financetracker.data.db.AppDatabase.getInstance(application)
            .instrumentDao()
            .observeCategories()
            .map { InvestmentOrder.names(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** How long the second back press has to follow the first one for the app to close. */
private const val DOUBLE_BACK_MILLIS = 2_000L
private const val SLIDE_MILLIS = 300

/** The screen for an instrument's own ledger: deposits and policies have theirs, the rest the generic detail. */
private fun instrumentRoute(type: InstrumentType, id: Long) = when (type) {
    InstrumentType.FIXED_DEPOSIT -> InvestmentRoutes.fixedDeposit(id)
    InstrumentType.INSURANCE -> InvestmentRoutes.policy(id)
    else -> "instrument/$id"
}

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun editorRoute(
    id: Long? = null,
    kind: TransactionKind? = null,
    accountId: Long? = null,
    instrumentId: Long? = null
) =
    "transaction?id=${id ?: -1}&kind=${kind?.name ?: ""}&account=${accountId ?: -1}&instrument=${instrumentId ?: -1}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current.findActivity()
    val drawerViewModel: DrawerViewModel = viewModel()
    val categoryNames by drawerViewModel.categoryNames.collectAsStateWithLifecycle()
    val investmentScreens = investmentScreens(categoryNames)
    val investmentsUnlocked = InvestmentsLock.unlocked
    var investmentsExpanded by rememberSaveable { mutableStateOf(false) }
    var manageExpanded by rememberSaveable { mutableStateOf(false) }
    var lastBackPress by remember { mutableStateOf(0L) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.entries.firstOrNull { d ->
        backStackEntry?.destination?.hierarchy?.any { it.route?.substringBefore('?') == d.route } == true
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

    /** Runs [then] once the investments are unlocked, asking for the phone's unlock when they are not yet. */
    fun withInvestments(then: () -> Unit) {
        if (investmentsUnlocked || activity == null) then() else InvestmentsLock.unlock(
            activity,
            then
        )
    }

    /**
     * Press and hold on Home's "Cash & bank" card or on the drawer's title: shows the investments after the phone's
     * unlock, or hides them. [onUnlocked] runs once they are shown.
     */
    fun toggleInvestments(onUnlocked: () -> Unit = {}) {
        if (investmentsUnlocked) InvestmentsLock.lock()
        else activity?.let { a -> InvestmentsLock.unlock(a, onUnlocked) }
    }

    fun openInvestments(subRoute: String? = null) = withInvestments {
        goTo(Destination.INVESTMENTS)
        if (subRoute != null) navController.navigate(subRoute) { launchSingleTop = true }
    }

    /** An instrument's screen on top of the current one (from the ledger), so back returns to where it was opened. */
    fun openInstrument(route: String) =
        withInvestments { navController.navigate(route) { launchSingleTop = true } }

    // Leaving the app from Home takes two back presses within two seconds, as Finance Manager did.
    val atHome = backStackEntry?.destination?.route == Destination.SUMMARY.route
    BackHandler(enabled = atHome && drawerState.isClosed) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < DOUBLE_BACK_MILLIS) activity?.finish()
        else {
            lastBackPress = now
            scope.launch { snackbarHostState.showSnackbar("Press back again to exit") }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Opens only from the menu button; the swipe closes it (and the swipes are free for tabbed screens).
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                // Scrolls: with the Investments sub-screens the list is taller than small phones.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        // Press and hold reveals (or hides) the investments from any screen, not only from Home.
                        Text(
                            "Finance Tracker",
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        toggleInvestments {
                                            investmentsExpanded = true
                                        }
                                    },
                                ),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close menu")
                        }
                    }
                    Destination.entries.forEach { destination ->
                        if (destination == Destination.INVESTMENTS) {
                            // No trace of the investments until they are unlocked from Home.
                            if (investmentsUnlocked) DrawerGroup(
                                title = destination.title,
                                expanded = investmentsExpanded,
                                selected = current == destination && !inEditor,
                                children = investmentScreens,
                                selectedChild = investmentScreens.map { it.second }
                                    .firstOrNull { subRoute ->
                                        route == subRoute || (subRoute.startsWith("investments/type/") && route == InvestmentRoutes.TYPE && backStackEntry?.arguments?.getString(
                                            "type"
                                        )
                                            ?.let { InvestmentRoutes.type(InstrumentType.valueOf(it)) } == subRoute)
                                    },
                                onToggleExpanded = { investmentsExpanded = !investmentsExpanded },
                                onOpen = { subRoute ->
                                    scope.launch { drawerState.close() }
                                    openInvestments(subRoute)
                                },
                            )
                        } else if (destination == Destination.EDIT) {
                            val manageTabs =
                                MANAGE_TABS.mapIndexed { i, title -> title to manageRoute(i) }
                            val currentTab = backStackEntry?.arguments?.getInt("tab")
                                ?.takeIf { route == ROUTE_MANAGE }
                            DrawerGroup(
                                title = destination.title,
                                expanded = manageExpanded,
                                selected = current == destination && !inEditor,
                                children = manageTabs,
                                selectedChild = currentTab?.let { manageRoute(it) },
                                onToggleExpanded = { manageExpanded = !manageExpanded },
                                onOpen = { subRoute ->
                                    scope.launch { drawerState.close() }
                                    if (subRoute == null) goTo(destination)
                                    else navController.navigate(subRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                            )
                        } else NavigationDrawerItem(
                            label = { Text(destination.title) },
                            selected = destination == current && !inEditor,
                            onClick = {
                                scope.launch { drawerState.close() }
                                goTo(destination)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
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
                    actions = {
                        // The most used screen, one tap from Home.
                        if (current == Destination.SUMMARY) TextButton(onClick = { goTo(Destination.TRANSACTIONS) }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Transactions")
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
            CompositionLocalProvider(LocalOpenDrawer provides { scope.launch { drawerState.open() } }) {
            NavHost(
                navController = navController,
                startDestination = Destination.SUMMARY.route,
                modifier = Modifier.padding(padding),
                // Screens slide in from the right and back out to it, so going forward and back feel different.
                enterTransition = { slideInHorizontally(tween(SLIDE_MILLIS)) { it } },
                exitTransition = { slideOutHorizontally(tween(SLIDE_MILLIS)) { -it / 3 } },
                popEnterTransition = { slideInHorizontally(tween(SLIDE_MILLIS)) { -it / 3 } },
                popExitTransition = { slideOutHorizontally(tween(SLIDE_MILLIS)) { it } },
            ) {
                composable(Destination.SUMMARY.route) {
                    SummaryScreen(
                        onAccountLongPress = { navController.navigate(editorRoute(accountId = it)) },
                        investmentsUnlocked = investmentsUnlocked,
                        onToggleInvestments = { toggleInvestments() },
                    )
                }
                composable(Destination.TRANSACTIONS.route) {
                    TransactionsScreen(
                        onEdit = { navController.navigate(editorRoute(id = it)) },
                        onOpenInstrument = { type, id ->
                            openInstrument(
                                instrumentRoute(
                                    type,
                                    id
                                )
                            )
                        },
                        snackbarHostState = snackbarHostState
                    )
                }
                composable(Destination.EXTRAS.route) { ExtrasScreen(snackbarHostState) }
                composable(
                    ROUTE_MANAGE,
                    arguments = listOf(navArgument("tab") {
                        type = NavType.IntType; defaultValue = 0
                    })
                ) { entry ->
                    EditDataScreen(
                        snackbarHostState,
                        initialTab = entry.arguments?.getInt("tab") ?: 0
                    )
                }
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
                        onAddToAccount = { navController.navigate(editorRoute(accountId = it)) },
                        onOpenAccount = { navController.navigate(InvestmentRoutes.pfAccount(it)) })
                }
                composable(
                    InvestmentRoutes.PF_ACCOUNT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    ProvidentFundAccountScreen(
                        accountId = entry.arguments!!.getLong("id"),
                        onBack = { navController.popBackStack() },
                        onAdd = { navController.navigate(editorRoute(accountId = it)) },
                        onEditTransaction = { navController.navigate(editorRoute(id = it)) },
                    )
                }
                composable(InvestmentRoutes.FIXED_DEPOSITS) {
                    FixedDepositsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDeposit = { navController.navigate(InvestmentRoutes.fixedDeposit(it)) })
                }
                composable(
                    InvestmentRoutes.FIXED_DEPOSIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    FixedDepositDetailScreen(
                        instrumentId = entry.arguments!!.getLong("id"),
                        onBack = { navController.popBackStack() },
                        onOpenDeposit = { navController.navigate(InvestmentRoutes.fixedDeposit(it)) },
                        onEditTransaction = { navController.navigate(editorRoute(id = it)) },
                    )
                }
                composable(InvestmentRoutes.INSURANCE) {
                    InsuranceScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPolicy = { navController.navigate(InvestmentRoutes.policy(it)) })
                }
                composable(
                    InvestmentRoutes.POLICY,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    InsurancePolicyScreen(
                        instrumentId = entry.arguments!!.getLong("id"),
                        onBack = { navController.popBackStack() },
                        onPayPremium = { instrument, kind ->
                            navController.navigate(
                                editorRoute(
                                    kind = kind,
                                    instrumentId = instrument
                                )
                            )
                        },
                        onEditTransaction = { navController.navigate(editorRoute(id = it)) },
                    )
                }
                composable(InvestmentRoutes.SAVINGS) { SavingsScreen(onBack = { navController.popBackStack() }) }
                composable(InvestmentRoutes.TARGETS) { TargetsScreen(onBack = { navController.popBackStack() }) }
                composable(
                    ROUTE_INSTRUMENT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val instrumentId = entry.arguments!!.getLong("id")
                    InstrumentDetailScreen(
                        instrumentId = instrumentId,
                        onBack = { navController.popBackStack() },
                        onEditTransaction = { navController.navigate(editorRoute(id = it)) },
                        onAddTransaction = { kind ->
                            navController.navigate(
                                editorRoute(
                                    kind = kind,
                                    instrumentId = instrumentId
                                )
                            )
                        },
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
                        navArgument("instrument") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                    val kind = entry.arguments?.getString("kind")?.takeIf { it.isNotEmpty() }
                        ?.let { TransactionKind.valueOf(it) }
                    val account = entry.arguments?.getLong("account")?.takeIf { it > 0 }
                    val instrument = entry.arguments?.getLong("instrument")?.takeIf { it > 0 }
                    TransactionEditorScreen(
                        id,
                        kind,
                        account,
                        onDone = { navController.popBackStack() },
                        initialInstrumentId = instrument
                    )
                }
            }
            }
        }
    }
}

/**
 * A drawer entry with screens under it: the row opens the main screen, the chevron shows or hides the children
 * (hidden by default so the drawer stays short).
 */
@Composable
private fun DrawerGroup(
    title: String,
    expanded: Boolean,
    selected: Boolean,
    children: List<Pair<String, String>>,
    selectedChild: String?,
    onToggleExpanded: () -> Unit,
    onOpen: (String?) -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(title) },
        selected = selected && selectedChild == null,
        onClick = { onOpen(null) },
        badge = {
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide the $title screens" else "Show the $title screens",
                )
            }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
    if (expanded) children.forEach { (childTitle, subRoute) ->
        NavigationDrawerItem(
            label = { Text(childTitle, style = MaterialTheme.typography.bodyMedium) },
            selected = subRoute == selectedChild,
            onClick = { onOpen(subRoute) },
            modifier = Modifier
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .padding(start = 24.dp),
        )
    }
}
