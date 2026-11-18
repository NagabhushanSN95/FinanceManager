package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.Insurance
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntry
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntryKind
import com.nagabhushansn95.financetracker.data.investments.PolicyView
import com.nagabhushansn95.financetracker.data.repository.InsuranceRepository
import com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown
import com.nagabhushansn95.financetracker.ui.common.ConfirmDeleteEntryDialog
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors
import com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class InsuranceState(
    val policies: List<PolicyView> = emptyList(),
    val lookups: LedgerLookups? = null,
    /** Accounts money moves against: everything but provident funds. */
    val banks: List<Account> = emptyList(),
    /** Expense categories by name (lower case), for the category an entry is filed under. */
    val expenseCategoryIds: Map<String, Long> = emptyMap(),
    val loaded: Boolean = false,
)

class InsuranceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = InsuranceRepository(db)

    /** One-off outcome of a save or delete, shown in a snackbar. */
    val message = MutableStateFlow<String?>(null)
    fun messageShown() {
        message.value = null
    }

    init {
        // The expense categories entries are filed under, whichever way the screen was reached.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                Insurance.fileCategories(
                    db
                )
            }
        }
    }

    val state: StateFlow<InsuranceState> = combine(
        db.instrumentDao().observeAll(),
        db.instrumentDao().observeInsurancePolicies(),
        db.transactionDao().observeWithInstrument(),
        combine(
            com.nagabhushansn95.financetracker.data.investments.Validity.observeAll(db),
            db.vehicleDao().observeVehicles(),
        ) { p, v -> p to v },
        combine(db.accountDao().observeAll(), db.categoryDao().observeAll()) { a, c -> a to c },
    ) { instruments, policies, ledger, (periods, vehicles), (accounts, categories) ->
        InsuranceState(
            policies = Insurance.build(
                instruments,
                policies,
                ledger,
                periods,
                LocalDate.now(),
                vehicles
            ),
            lookups = LedgerLookups(accounts, categories, instruments),
            banks = accounts.filter { it.accountType != AccountType.PROVIDENT_FUND && !it.deleted },
            expenseCategoryIds = categories.filter { !it.deleted && it.appliesTo != CategoryScope.INCOME }
                .associate { it.name.lowercase() to it.id },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsuranceState())

    fun savePolicy(
        instrument: Instrument,
        policy: InsurancePolicy?,
        vehicle: com.nagabhushansn95.financetracker.data.db.entity.Vehicle?,
        manual: Map<ValidityKind, LocalDate?>
    ) {
        viewModelScope.launch {
            runCatching { repository.savePolicy(instrument, policy, manual, vehicle = vehicle) }
                .onFailure { message.value = it.message ?: it.toString() }
        }
    }

    /** Records a new entry on [view] or rewrites [existing] as the sheet describes it. */
    fun saveEntry(view: PolicyView, entry: InsuranceEntry, existing: TransactionEntity?) {
        viewModelScope.launch {
            runCatching {
                val categoryId = Insurance.categoryNameFor(view.type, entry.kind)
                    ?.let { state.value.expenseCategoryIds[it.lowercase()] }
                repository.saveEntry(view, entry, categoryId, existing)
            }.onSuccess {
                message.value =
                    if (existing == null) "${entry.kind.title} recorded" else "Entry saved"
            }.onFailure { message.value = it.message ?: it.toString() }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteEntry(id) }
                .onSuccess { message.value = "Entry deleted" }
                .onFailure { message.value = it.message ?: it.toString() }
        }
    }

    /** Removes the policy or vehicle with everything on it; [onDone] runs once it is gone. */
    fun deletePolicy(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.deletePolicy(id) }
                .onSuccess { onDone() }
                .onFailure { message.value = it.message ?: it.toString() }
        }
    }
}

private fun LocalDate.inDays(asOf: LocalDate): Long = ChronoUnit.DAYS.between(asOf, this)

/** "to 12 Nov 2027 (in 40 days)" for a validity end, or null when none is set. */
private fun untilText(end: LocalDate?, asOf: LocalDate): String? = end?.let {
    val days = it.inDays(asOf)
    "${it.format(SHORT_DATE)} (${if (days < 0) "${-days} days ago" else "in $days days"})"
}

private fun monthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

/** The detail lines of a policy row, by type: what the policy is, then the dates it runs on. */
private fun rowLines(v: PolicyView): List<String> {
    val p = v.policy
    val asOf = v.asOf
    fun until(kind: ValidityKind, label: String) =
        v.validUntil(kind)?.let { "$label ${it.format(SHORT_DATE)}" }
    return when (v.type) {
        InsuranceType.LIFE, InsuranceType.TERM -> listOf(
            listOfNotNull(
                p?.premiumAmount?.let { "${MoneyFormat.amount(it)} ${Insurance.frequencyLabel(p.premiumFrequencyMonths)}" },
                p?.sumAssured?.let { "assured ${MoneyFormat.amount(it)}" },
                v.instrument.owner.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
            ).joinToString(" · "),
            listOfNotNull(
                p?.startDate?.format(SHORT_DATE),
                p?.maturityDate?.format(SHORT_DATE)
            ).joinToString(" → ").ifEmpty { "${v.premiums.size} premiums paid" },
        )

        InsuranceType.HEALTH -> listOf(
            listOfNotNull(
                (p?.premiumAmount ?: v.premiums.lastOrNull()?.amount)?.let {
                    "${
                        MoneyFormat.amount(
                            it
                        )
                    } last premium"
                },
                v.sumInsured?.let { "insured ${MoneyFormat.amount(it)}" },
                v.instrument.owner.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
            ).joinToString(" · "),
            untilText(v.validUntil(ValidityKind.COVER), asOf)?.let { "Covered until $it" }
                ?: "${v.premiums.size} premiums paid",
        )

        InsuranceType.MOTOR -> listOf(
            listOfNotNull(
                v.vehicle?.registrationNo,
                v.vehicle?.makeModel,
                v.current(ValidityKind.THIRD_PARTY)?.provider
                    ?: v.current(ValidityKind.OWN_DAMAGE)?.provider,
                v.instrument.owner.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
            ).joinToString(" · "),
            listOfNotNull(
                until(ValidityKind.THIRD_PARTY, "Third party until"),
                until(ValidityKind.OWN_DAMAGE, "Own damage until"),
            ).joinToString(" · ").ifEmpty { "No cover recorded yet" },
            listOfNotNull(
                until(ValidityKind.SERVICE, "Service"),
                until(ValidityKind.EMISSION_TEST, "Emission test"),
            ).joinToString(" · "),
        )
    }.filter { it.isNotEmpty() }
}

/** What a row is waiting on: the thing, how far off it is, and whether it is a cover that ends or a task that falls due. */
private data class Due(val what: String, val days: Long, val cover: Boolean) {
    val near: Boolean get() = days <= 30

    /** "Own damage cover ends in 6 days", "Emission test overdue 11 days", "Premium due in 19 days". */
    fun text(): String = when {
        cover && days < 0 -> "$what expired ${-days} days ago"
        cover -> "$what ends in $days days"
        days < 0 -> "$what overdue ${-days} days"
        else -> "$what due in $days days"
    }
}

/** The soonest date the row is waiting on: the next premium, or the earliest validity end. */
private fun PolicyView.soonest(): Due? = when {
    type.scheduled -> dueIn?.let { Due("Premium", it, cover = false) }
    type == InsuranceType.HEALTH -> validUntil(ValidityKind.COVER)?.let {
        Due(
            "Cover",
            it.inDays(asOf),
            cover = true
        )
    }

    else -> listOf(
        Triple(ValidityKind.THIRD_PARTY, "Third party cover", true),
        Triple(ValidityKind.OWN_DAMAGE, "Own damage cover", true),
        Triple(ValidityKind.SERVICE, "Service", false),
        Triple(ValidityKind.EMISSION_TEST, "Emission test", false)
    ).mapNotNull { (k, label, cover) -> validUntil(k)?.let { Due(label, it.inDays(asOf), cover) } }
        .minByOrNull { it.days }
}

/** The heading of a type's card and screen: "Life insurance", "Vehicles". */
fun InsuranceType.heading(): String =
    if (this == InsuranceType.MOTOR) "Vehicles" else "$title insurance"

/** A type's card: one key figure and, when something is due, the soonest date with the policy it belongs to. */
private data class TypeCard(val figure: String, val soonest: Due?, val soonestOf: String?)

private fun typeCard(type: InsuranceType, active: List<PolicyView>): TypeCard {
    if (active.isEmpty()) return TypeCard("None yet", null, null)
    val soonest =
        active.mapNotNull { v -> v.soonest()?.let { it to v } }.minByOrNull { it.first.days }
    val figure = when (type) {
        InsuranceType.LIFE -> "${MoneyFormat.compact(active.sumOf { it.paid })} paid in"
        InsuranceType.TERM -> "${MoneyFormat.compact(active.sumOf { it.policy?.sumAssured ?: 0.0 })} assured"
        InsuranceType.HEALTH -> "${MoneyFormat.compact(active.sumOf { it.sumInsured ?: 0.0 })} insured"
        InsuranceType.MOTOR -> "${MoneyFormat.compact(active.sumOf { it.paid + it.upkeep })} spent"
    }
    return TypeCard(
        figure,
        soonest?.first,
        soonest?.second?.let { if (type == InsuranceType.MOTOR) it.instrument.name else it.title })
}

/**
 * The Insurance overview: four cards, two by two, one per kind of cover (life, term, health, vehicles), each with
 * its count, one key figure and the soonest date due; a card opens that kind's own list. Life premiums count as
 * investments; term, health and motor are protection and stay out of the investment totals, but their premiums,
 * cover and (for vehicles) service and emission tests are tracked here, and Home's Alerts card reads their dates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceScreen(
    onBack: () -> Unit,
    onOpenType: (InsuranceType) -> Unit,
    viewModel: InsuranceViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text("Insurance") },
                navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New policy or vehicle")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (!state.loaded) item {
                Text(
                    "Loading…",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(InsuranceType.entries.chunked(2), key = { it.first().name }) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { type ->
                        val ofType = state.policies.filter { it.type == type }
                        val active = ofType.filter { it.active }
                        val card = typeCard(type, active)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onOpenType(type) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(type.heading(), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    when {
                                        ofType.isEmpty() -> "—"
                                        active.size == ofType.size -> "${active.size} active"
                                        else -> "${active.size} of ${ofType.size} active"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    card.figure,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                card.soonest?.let { due ->
                                    Text(
                                        due.text(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (due.near) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    card.soonestOf?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            }
            item {
                Text(
                    "Life premiums count as investments; term, health and motor are protection and stay out of the investment totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
    if (creating) PolicySheet(
        existing = null,
        onSave = { i, p, veh, m -> viewModel.savePolicy(i, p, veh, m); creating = false },
        onDismiss = { creating = false })
}

/** The policies or vehicles of one kind: active first, then the matured, lapsed or sold ones. "+" adds one of this kind. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceTypeScreen(
    type: InsuranceType,
    onBack: () -> Unit,
    onOpenPolicy: (Long) -> Unit,
    viewModel: InsuranceViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val ofType = state.policies.filter { it.type == type }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text(type.heading()) },
                navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = if (type == InsuranceType.MOTOR) "New vehicle" else "New policy"
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            if (ofType.isEmpty()) item {
                Text(
                    if (state.loaded) "Nothing here yet. Tap + to add ${if (type == InsuranceType.MOTOR) "a vehicle" else "a policy"}." else "Loading…",
                    modifier = Modifier.padding(16.dp)
                )
            }
            val groups = listOf(
                "Active" to ofType.filter { it.active },
                (if (type == InsuranceType.MOTOR) "Sold" else "Matured or lapsed") to ofType.filter { !it.active },
            ).filter { it.second.isNotEmpty() }
            groups.forEach { (title, list) ->
                if (groups.size > 1 || title != "Active") item(key = "h:$title") {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HorizontalDivider()
                }
                items(
                    list,
                    key = { it.instrument.id }) { v -> PolicyRow(v) { onOpenPolicy(v.instrument.id) } }
            }
        }
    }
    if (creating) PolicySheet(
        existing = null,
        initialType = type,
        onSave = { i, p, veh, m -> viewModel.savePolicy(i, p, veh, m); creating = false },
        onDismiss = { creating = false })
}

/**
 * One policy or vehicle in a kind's list. Only life rows carry the amount paid in: a life policy is a saving, so paid-in
 * against assured matters; what has gone into term, health or motor cover over the years is not worth a column.
 * Anything within 30 days, or already past, is called out in red under the details.
 */
@Composable
private fun PolicyRow(v: PolicyView, onClick: () -> Unit) {
    val lines = rowLines(v)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                v.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (v.type == InsuranceType.LIFE) Text(
                MoneyFormat.amount(v.paid),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        lines.forEach {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (v.active) v.soonest()?.takeIf { it.near }?.let { due ->
            Text(
                due.text(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    HorizontalDivider()
}

/**
 * One policy or vehicle: details, totals and the ledger. The "+" offers the entries its type takes (premium, payout,
 * service, emission test); press-and-hold on a ledger row edits it in the same sheet. Insurance rows are touched
 * only here: the transaction editor never offers these instruments.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InsurancePolicyScreen(
    instrumentId: Long,
    onBack: () -> Unit,
    viewModel: InsuranceViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val v = state.policies.firstOrNull { it.instrument.id == instrumentId }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editingPolicy by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<InsuranceEntryKind?>(null) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var confirmDeleteEntry by remember { mutableStateOf<TransactionEntity?>(null) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text(v?.title ?: "Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (v != null) IconButton(onClick = { editingPolicy = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit policy")
                    }
                    if (v != null) IconButton(onClick = { deleting = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = if (v.type == InsuranceType.MOTOR) "Delete vehicle" else "Delete policy"
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (v != null) Box {
                FloatingActionButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Record an entry")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    Insurance.kindsFor(v.type).forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(kind.title) },
                            onClick = { menuOpen = false; adding = kind })
                    }
                }
            }
        },
    ) { padding ->
        if (v == null) {
            Text(
                if (state.loaded) "Policy not found." else "Loading…",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        val p = v.policy
        val asOf = v.asOf
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            MoneyFormat.amount(v.paid),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Premiums paid (${v.premiums.size})" +
                                    if (v.type == InsuranceType.MOTOR && v.upkeep > 0) " · services and tests ${
                                        MoneyFormat.amount(
                                            v.upkeep
                                        )
                                    }" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        when (v.type) {
                            InsuranceType.LIFE, InsuranceType.TERM -> {
                                Row {
                                    StatCell(
                                        "Premium",
                                        p?.premiumAmount?.let {
                                            "${MoneyFormat.amount(it)} ${
                                                Insurance.frequencyLabel(
                                                    p.premiumFrequencyMonths
                                                )
                                            }"
                                        }
                                            ?: "—")
                                    StatCell(
                                        "Next due",
                                        v.nextDue?.format(SHORT_DATE) ?: "—",
                                        v.dueIn?.let { if (it <= 30) -1.0 else null })
                                }
                                Row {
                                    StatCell(
                                        "Sum assured",
                                        p?.sumAssured?.let { MoneyFormat.amount(it) } ?: "—")
                                    if (v.type == InsuranceType.LIFE) StatCell(
                                        "Received back",
                                        MoneyFormat.amount(v.received)
                                    )
                                    else StatCell("Policy no", p?.policyNumber ?: "—")
                                }
                                Row {
                                    StatCell("Start", p?.startDate?.format(SHORT_DATE) ?: "—")
                                    StatCell(
                                        if (v.type == InsuranceType.LIFE) "Maturity" else "Cover ends",
                                        p?.maturityDate?.format(SHORT_DATE) ?: "—"
                                    )
                                }
                                if (v.type == InsuranceType.LIFE) Row {
                                    StatCell("Policy no", p?.policyNumber ?: "—")
                                    StatCell("Due on", p?.dueDay?.let { d ->
                                        if ((p.premiumFrequencyMonths
                                                ?: 12) > 1 && p.dueMonth != null
                                        ) "$d ${monthName(p.dueMonth)}" else "day $d"
                                    } ?: "—")
                                }
                            }

                            InsuranceType.HEALTH -> {
                                val end = v.validUntil(ValidityKind.COVER)
                                Row {
                                    StatCell(
                                        "Last premium",
                                        (p?.premiumAmount ?: v.premiums.lastOrNull()?.amount)?.let {
                                            MoneyFormat.amount(
                                                it
                                            )
                                        } ?: "—")
                                    StatCell(
                                        "Covered until",
                                        end?.format(SHORT_DATE) ?: "—",
                                        end?.let { if (it.inDays(asOf) <= 30) -1.0 else null })
                                }
                                Row {
                                    // The cover in force says the sum insured and the policy number; a renewal may change both.
                                    StatCell(
                                        "Sum insured",
                                        v.sumInsured?.let { MoneyFormat.amount(it) } ?: "—")
                                    StatCell("Policy no", v.policyNumber ?: "—")
                                }
                                Row {
                                    StatCell("Start", p?.startDate?.format(SHORT_DATE) ?: "—")
                                    StatCell("Premiums", "${v.premiums.size}")
                                }
                            }

                            InsuranceType.MOTOR -> {
                                Row {
                                    val tp = v.validUntil(ValidityKind.THIRD_PARTY)
                                    val od = v.validUntil(ValidityKind.OWN_DAMAGE)
                                    StatCell(
                                        "Third party until",
                                        tp?.format(SHORT_DATE) ?: "—",
                                        tp?.let { if (it.inDays(asOf) <= 30) -1.0 else null })
                                    StatCell(
                                        "Own damage until",
                                        od?.format(SHORT_DATE) ?: "—",
                                        od?.let { if (it.inDays(asOf) <= 30) -1.0 else null })
                                }
                                Row {
                                    val service = v.validUntil(ValidityKind.SERVICE)
                                    val emission = v.validUntil(ValidityKind.EMISSION_TEST)
                                    StatCell(
                                        "Service due",
                                        service?.format(SHORT_DATE) ?: "—",
                                        service?.let { if (it.inDays(asOf) <= 30) -1.0 else null })
                                    StatCell(
                                        "Emission test until",
                                        emission?.format(SHORT_DATE) ?: "—",
                                        emission?.let { if (it.inDays(asOf) <= 30) -1.0 else null })
                                }
                                Row {
                                    // The cover in force that names its insurer: a hand-set third-party period may not.
                                    val covers = listOfNotNull(
                                        v.current(ValidityKind.OWN_DAMAGE),
                                        v.current(ValidityKind.THIRD_PARTY)
                                    )
                                    val cover =
                                        covers.firstOrNull { it.provider != null || it.reference != null }
                                            ?: covers.firstOrNull()
                                    StatCell("Insurer", cover?.provider ?: "—")
                                    StatCell("Policy no", cover?.reference ?: "—")
                                }
                                Row {
                                    StatCell("Vehicle no", v.vehicle?.registrationNo ?: "—")
                                    StatCell("Premiums", "${v.premiums.size}")
                                }
                                if (v.vehicle?.makeModel != null || v.vehicle?.boughtOn != null) Row {
                                    StatCell("Make and model", v.vehicle?.makeModel ?: "—")
                                    StatCell(
                                        "Bought on",
                                        v.vehicle?.boughtOn?.format(SHORT_DATE) ?: "—"
                                    )
                                }
                            }
                        }
                        Text(
                            if (v.type.investment) "Counts as an investment: premiums appear in the Investment Summary and the savings tables."
                            else "Protection, not an investment: premiums are expenses and stay out of the investment totals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        v.instrument.notes?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Text(
                    "Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                HorizontalDivider()
            }
            if (v.rows.isEmpty()) item {
                Text(
                    "Nothing recorded yet. Tap + to record a ${
                        Insurance.kindsFor(v.type).first().title.lowercase()
                    }.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(
                v.rows.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }),
                key = { it.id }) { t ->
                val bought = v.periodsOf(t)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {}, onLongClick = { editing = t })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.particulars, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${t.date.format(SHORT_DATE)} · ${state.lookups?.describe(t) ?: t.kind.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (bought.isNotEmpty()) Text(
                            bought.joinToString(" · ") {
                                "${it.kind.title.lowercase()} to ${
                                    it.endDate.format(
                                        SHORT_DATE
                                    )
                                }"
                            } +
                                    listOfNotNull(
                                        bought.firstNotNullOfOrNull { it.sumInsured }
                                            ?.let { "insured ${MoneyFormat.amount(it)}" },
                                        bought.firstNotNullOfOrNull { it.provider },
                                        bought.firstNotNullOfOrNull { it.reference },
                                    ).joinToString("") { " · $it" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        (if (v.entryKindOf(t) == InsuranceEntryKind.PAYOUT) "−" else "") + MoneyFormat.amount(
                            t.amount,
                            state.lookups?.currencyOf(t) ?: "INR"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider()
            }
        }
    }
    if (deleting && v != null) {
        val what = if (v.type == InsuranceType.MOTOR) "vehicle" else "policy"
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete ${v.title}?") },
            text = {
                Text(
                    if (v.rows.isEmpty()) "The $what has no entries; it is removed with its dates."
                    else "Its ${v.rows.size} ${if (v.rows.size == 1) "entry goes" else "entries go"} too, with the account balances put back. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false; viewModel.deletePolicy(
                    v.instrument.id,
                    onBack
                )
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } },
        )
    }
    if (editingPolicy && v != null) PolicySheet(
        existing = v,
        onSave = { i, p, veh, m -> viewModel.savePolicy(i, p, veh, m); editingPolicy = false },
        onDismiss = { editingPolicy = false })
    val sheetKind = adding ?: editing?.let { v?.entryKindOf(it) }
    if (v != null && sheetKind != null) InsuranceEntrySheet(
        view = v,
        banks = state.banks,
        defaultBankId = v.rows.maxWithOrNull(compareBy({ it.date }, { it.id }))
            ?.let { it.fromAccountId ?: it.toAccountId }
            ?: state.banks.firstOrNull { it.accountType == AccountType.BANK }?.id,
        places = state.policies.flatMap { p -> p.periods.filter { it.kind == ValidityKind.SERVICE || it.kind == ValidityKind.EMISSION_TEST } }
            .mapNotNull { it.provider?.trim()?.ifBlank { null } }.distinct().sorted(),
        initialKind = sheetKind,
        existing = editing,
        onSave = { entry -> viewModel.saveEntry(v, entry, editing); adding = null; editing = null },
        onDelete = editing?.let { row -> { confirmDeleteEntry = row } },
        onDismiss = { adding = null; editing = null },
    )
    confirmDeleteEntry?.let { row ->
        ConfirmDeleteEntryDialog(
            row,
            onConfirm = {
                viewModel.deleteEntry(row.id); editing = null; confirmDeleteEntry = null
            },
            onDismiss = { confirmDeleteEntry = null },
        )
    }
    // A row the sheet cannot describe (an old import, say) is left alone rather than edited blind.
    LaunchedEffect(editing) {
        val row = editing ?: return@LaunchedEffect
        if (v?.entryKindOf(row) == null) {
            editing = null
            snackbar.showSnackbar("This row is not an entry the sheet knows; it stays as it is.")
        }
    }
}

/**
 * New or edited policy or vehicle: the type, what identifies it, the premium schedule (life and term), the sums,
 * and, for health and motor, the validity in force when the last entry predates the app. Entries keep that validity
 * up to date afterwards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicySheet(
    existing: PolicyView?,
    onSave: (Instrument, InsurancePolicy?, com.nagabhushansn95.financetracker.data.db.entity.Vehicle?, Map<ValidityKind, LocalDate?>) -> Unit,
    onDismiss: () -> Unit,
    initialType: InsuranceType = InsuranceType.LIFE,
) {
    val p = existing?.policy
    var type by remember { mutableStateOf(existing?.type ?: initialType) }
    var provider by remember { mutableStateOf(p?.provider ?: "") }
    var policyName by remember { mutableStateOf(p?.policyName ?: "") }
    var vehicleName by remember {
        mutableStateOf(existing?.instrument?.name?.takeIf { existing.isVehicle } ?: "")
    }
    var vehicleNumber by remember { mutableStateOf(existing?.vehicle?.registrationNo ?: "") }
    var makeModel by remember { mutableStateOf(existing?.vehicle?.makeModel ?: "") }
    var boughtOn by remember { mutableStateOf(existing?.vehicle?.boughtOn) }
    var number by remember { mutableStateOf(p?.policyNumber ?: "") }
    var sumAssured by remember { mutableStateOf(p?.sumAssured?.let(FdMath::plain) ?: "") }
    var premium by remember { mutableStateOf(p?.premiumAmount?.let(FdMath::plain) ?: "") }
    var frequency by remember { mutableStateOf(p?.premiumFrequencyMonths ?: 12) }
    var dueDay by remember { mutableStateOf(p?.dueDay?.toString() ?: "") }
    var dueMonth by remember { mutableStateOf(p?.dueMonth ?: LocalDate.now().monthValue) }
    var start by remember { mutableStateOf(p?.startDate) }
    var maturity by remember { mutableStateOf(p?.maturityDate) }
    var owner by remember { mutableStateOf(existing?.instrument?.owner ?: Instrument.OWNER_SELF) }
    var active by remember { mutableStateOf(existing?.instrument?.active ?: true) }
    var notes by remember { mutableStateOf(existing?.instrument?.notes ?: "") }
    val manual = remember {
        androidx.compose.runtime.mutableStateMapOf<ValidityKind, LocalDate?>().also { m ->
            existing?.let { v -> ValidityKind.entries.forEach { k -> m[k] = v.validUntil(k) } }
        }
    }
    val name = if (type == InsuranceType.MOTOR) vehicleName.trim()
    else listOf(provider.trim(), policyName.trim()).filter { it.isNotEmpty() }.joinToString(" - ")
    val day = dueDay.toIntOrNull()
    val dayOk = !type.scheduled || dueDay.isBlank() || (day != null && day in 1..31)

    FormSheet(
        title = if (existing == null) "New policy" else if (type == InsuranceType.MOTOR) "Edit vehicle" else "Edit policy",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsuranceType.entries.forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.title) })
            }
        }
        if (type == InsuranceType.MOTOR) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vehicleName,
                    onValueChange = { vehicleName = it },
                    label = { Text("Vehicle") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it },
                    label = { Text("Vehicle number") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = makeModel,
                    onValueChange = { makeModel = it },
                    label = { Text("Make and model (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                DateField(
                    "Bought on",
                    boughtOn,
                    { boughtOn = it },
                    Modifier.weight(1f),
                    clearable = true
                )
            }
            Text(
                "Each year's premium carries its own insurer, policy number and cover dates; record them from the vehicle's + button.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = policyName,
                    onValueChange = { policyName = it },
                    label = { Text("Plan name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Policy number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    premium,
                    if (type == InsuranceType.HEALTH) "Premium (last paid)" else "Premium",
                    { premium = it },
                    Modifier.weight(1f)
                )
                NumberField(
                    sumAssured,
                    if (type == InsuranceType.HEALTH) "Sum insured" else "Sum assured",
                    { sumAssured = it },
                    Modifier.weight(1f)
                )
            }
            if (type == InsuranceType.HEALTH) Text(
                "Each premium carries its own policy number, sum insured and cover dates; the latest ones show here and follow the premiums recorded from the policy's + button.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (type.scheduled) {
            ChoiceDropdown(
                "Frequency",
                Insurance.FREQUENCIES,
                frequency,
                { Insurance.frequencyLabel(it) }) { frequency = it }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberField(dueDay, "Due day (1–31)", { dueDay = it }, Modifier.weight(1f))
                if (frequency > 1) Box(modifier = Modifier.weight(1f)) {
                    ChoiceDropdown(
                        "Due month",
                        (1..12).toList(),
                        dueMonth,
                        { monthName(it) }) { dueMonth = it }
                }
            }
            Text(
                if (frequency == 1) "The premium falls due on this day every month; the next due date follows the last premium paid."
                else "The premium falls due on this day of this month and every ${
                    Insurance.frequencyLabel(
                        frequency
                    ).removeSuffix("ly")
                } step after it; the next due date follows the last premium paid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (type != InsuranceType.MOTOR) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Start", start, { start = it }, Modifier.weight(1f), clearable = true)
            if (type.scheduled) DateField(
                if (type == InsuranceType.LIFE) "Maturity" else "Cover ends",
                maturity,
                { maturity = it },
                Modifier.weight(1f),
                clearable = true
            )
        }
        val manualKinds = Insurance.manualKindsFor(type)
        if (manualKinds.isNotEmpty()) {
            Text(
                if (existing == null) "Validity in force now (for cover bought before using this app; entries keep it up to date afterwards):"
                else "Validity in force (entries keep it up to date; change it here only for cover bought before using this app):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            manualKinds.forEach { k ->
                DateField(
                    validityLabel(k),
                    manual[k],
                    { manual[k] = it },
                    Modifier.fillMaxWidth(),
                    clearable = true
                )
            }
        }
        OutlinedTextField(
            value = owner,
            onValueChange = { owner = it },
            label = { Text("Owner") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (existing != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = active, onCheckedChange = { active = it })
            Text(if (type == InsuranceType.MOTOR) "Active (untick when the vehicle is sold)" else "Active (untick when matured or lapsed)")
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(enabled = name.isNotBlank() && dayOk, modifier = Modifier.fillMaxWidth(), onClick = {
            val now = java.time.LocalDateTime.now()
            val motor = type == InsuranceType.MOTOR
            val instrument = (existing?.instrument ?: Instrument(
                name = name,
                instrumentType = if (motor) InstrumentType.VEHICLE else InstrumentType.INSURANCE,
                createdTime = now,
                modifiedTime = now
            )).copy(
                name = name,
                instrumentType = if (motor) InstrumentType.VEHICLE else InstrumentType.INSURANCE,
                owner = owner.trim().ifBlank { Instrument.OWNER_SELF },
                categoryId = null,
                active = active,
                notes = notes.ifBlank { null })
            onSave(
                instrument,
                if (motor) null else InsurancePolicy(
                    instrumentId = instrument.id,
                    insuranceType = type,
                    provider = provider.trim().ifBlank { null },
                    policyName = policyName.trim().ifBlank { null },
                    policyNumber = number.trim().ifBlank { null },
                    sumAssured = sumAssured.toDoubleOrNull(),
                    premiumAmount = premium.toDoubleOrNull(),
                    premiumFrequencyMonths = frequency.takeIf { type.scheduled },
                    dueDay = day.takeIf { type.scheduled },
                    dueMonth = dueMonth.takeIf { type.scheduled && frequency > 1 },
                    startDate = start,
                    maturityDate = maturity.takeIf { type.scheduled },
                ),
                if (motor) com.nagabhushansn95.financetracker.data.db.entity.Vehicle(
                    instrumentId = instrument.id,
                    registrationNo = vehicleNumber.trim().ifBlank { null },
                    makeModel = makeModel.trim().ifBlank { null },
                    boughtOn = boughtOn,
                ) else null,
                manualKinds.associateWith { manual[it] },
            )
        }) { Text("Save") }
    }
}
