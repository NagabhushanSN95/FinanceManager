package com.nagabhushansn95.financetracker.ui.investments

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import com.nagabhushansn95.financetracker.data.repository.EditDataRepository
import com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.transaction.LedgerLookups
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A policy as the screens show it: instrument, details, premiums paid (INVEST or EXPENSE rows tagged with it). */
data class PolicyView(
    val instrument: Instrument,
    val policy: InsurancePolicy?,
    val rows: List<TransactionEntity>,
    val asOf: LocalDate,
) {
    val type: InsuranceType get() = policy?.insuranceType ?: InsuranceType.OTHER
    val premiums: List<TransactionEntity> get() = rows.filter { it.kind == TransactionKind.INVEST || it.kind == TransactionKind.EXPENSE }
    val paid: Double get() = premiums.sumOf { it.amountInr }
    val received: Double
        get() = rows.filter { it.kind == TransactionKind.REDEEM || it.kind == TransactionKind.INCOME }
            .sumOf { it.amountInr }
    val lastPremium: LocalDate? get() = premiums.maxOfOrNull { it.date }
    val countsAsInvestment: Boolean
        get() = policy?.countsAsInvestment ?: (type == InsuranceType.LIFE)

    /** The stored due date when it is still ahead of the last premium, else the last premium plus the frequency. */
    val nextDue: LocalDate?
        get() {
            val stored = policy?.nextDueDate
            val last = lastPremium
            val months = policy?.premiumFrequencyMonths
            return when {
                stored != null && (last == null || stored.isAfter(last)) -> stored
                last != null && months != null && months > 0 -> last.plusMonths(months.toLong())
                else -> stored
            }
        }
    val dueIn: Long? get() = nextDue?.let { java.time.temporal.ChronoUnit.DAYS.between(asOf, it) }
    val expired: Boolean get() = policy?.maturityDate?.isBefore(asOf) == true
    val title: String
        get() = listOfNotNull(
            policy?.provider,
            policy?.policyName
        ).joinToString(" · ").ifEmpty { instrument.name }
}

data class InsuranceState(
    val policies: List<PolicyView> = emptyList(),
    val categories: List<InvestmentCategory> = emptyList(),
    val lookups: LedgerLookups? = null,
    val loaded: Boolean = false,
)

class InsuranceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)

    val state: StateFlow<InsuranceState> = combine(
        db.instrumentDao().observeAll(),
        db.instrumentDao().observeInsurancePolicies(),
        db.transactionDao().observeWithInstrument(),
        db.instrumentDao().observeCategories(),
        db.accountDao().observeAll(),
    ) { instruments, policies, ledger, categories, accounts ->
        val byId = policies.associateBy { it.instrumentId }
        val rows = ledger.groupBy { it.instrumentId }
        val today = LocalDate.now()
        InsuranceState(
            policies = instruments.filter { it.instrumentType == InstrumentType.INSURANCE }
                .map {
                    PolicyView(
                        it,
                        byId[it.id],
                        rows[it.id].orEmpty().sortedWith(compareBy({ r -> r.date }, { r -> r.id })),
                        today
                    )
                },
            categories = categories,
            lookups = LedgerLookups(accounts, db.categoryDao().getAll(), instruments),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsuranceState())

    fun save(instrument: Instrument, policy: InsurancePolicy) {
        viewModelScope.launch { EditDataRepository(db).saveInsurancePolicy(instrument, policy) }
    }
}

fun insuranceTypeTitle(type: InsuranceType) = when (type) {
    InsuranceType.LIFE -> "Life"
    InsuranceType.HEALTH -> "Health"
    InsuranceType.MOTOR -> "Motor"
    InsuranceType.OTHER -> "Other"
}

/** Policies grouped by type with premiums paid, next due date and whether they count as investments. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceScreen(
    onBack: () -> Unit,
    onOpenPolicy: (Long) -> Unit,
    viewModel: InsuranceViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val own = state.policies.filter { it.instrument.owner == Instrument.OWNER_SELF }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insurance") },
                navigationIcon = { com.nagabhushansn95.financetracker.ui.common.BackAndMenu(onBack) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                creating = true
            }) { Icon(Icons.Filled.Add, contentDescription = "New policy") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                val active = own.filter { it.instrument.active && !it.expired }
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(MoneyFormat.amount(active.filter { it.countsAsInvestment }
                            .sumOf { it.paid }), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Premiums paid into policies that count as investments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            StatCell(
                                "Sum assured",
                                MoneyFormat.amount(active.sumOf { it.policy?.sumAssured ?: 0.0 })
                            )
                            StatCell(
                                "Per year",
                                MoneyFormat.amount(active.sumOf { v ->
                                    v.policy?.let { p ->
                                        (p.premiumAmount ?: 0.0) * 12.0 / (p.premiumFrequencyMonths
                                            ?: 12).coerceAtLeast(1)
                                    } ?: 0.0
                                })
                            )
                            StatCell("Policies", "${active.size}")
                        }
                        active.mapNotNull { v -> v.nextDue?.let { it to v } }
                            .minByOrNull { it.first }?.let { (date, v) ->
                            Text(
                                "Next premium ${date.format(SHORT_DATE)} · ${v.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (state.policies.isEmpty()) item {
                Text(
                    if (state.loaded) "No policies yet. Tap + to add one." else "Loading…",
                    modifier = Modifier.padding(16.dp)
                )
            }
            val groups = listOf(
                "Active" to state.policies.filter { it.instrument.active && !it.expired },
                "Matured or lapsed" to state.policies.filter { !it.instrument.active || it.expired },
            ).filter { it.second.isNotEmpty() }
            groups.forEach { (title, list) ->
                item(key = "h:$title") {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HorizontalDivider()
                }
                InsuranceType.entries.forEach { type ->
                    val ofType = list.filter { it.type == type }
                    if (ofType.isNotEmpty()) {
                        item(key = "t:$title:$type") {
                            Text(
                                insuranceTypeTitle(type),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        items(
                            ofType,
                            key = { it.instrument.id }) { v -> PolicyRow(v) { onOpenPolicy(v.instrument.id) } }
                    }
                }
            }
        }
    }
    if (creating) PolicySheet(
        existing = null,
        categories = state.categories,
        onSave = { i, p -> viewModel.save(i, p); creating = false },
        onDismiss = { creating = false })
}

@Composable
private fun PolicyRow(v: PolicyView, onClick: () -> Unit) {
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
            Text(
                MoneyFormat.amount(v.paid),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            listOfNotNull(
                v.policy?.premiumAmount?.let { "${MoneyFormat.amount(it)} ${premiumFrequencyLabel(v.policy.premiumFrequencyMonths)}" },
                v.policy?.sumAssured?.let { "assured ${MoneyFormat.amount(it)}" },
                if (v.countsAsInvestment) "investment" else "not an investment",
                v.instrument.owner.takeIf { it != Instrument.OWNER_SELF }?.let { "owner $it" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            Text(
                listOfNotNull(
                    v.policy?.startDate?.format(SHORT_DATE),
                    v.policy?.maturityDate?.format(SHORT_DATE)
                ).joinToString(" → ").ifEmpty { "${v.premiums.size} premiums paid" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            v.dueIn?.let { days ->
                Text(
                    if (days < 0) "overdue ${-days} days" else "due in $days days",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (days <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    HorizontalDivider()
}

fun premiumFrequencyLabel(months: Int?) = when (months) {
    null -> ""
    1 -> "monthly"
    3 -> "quarterly"
    6 -> "half-yearly"
    12 -> "yearly"
    else -> "every $months months"
}

/** One policy: details, totals, the premium ledger and "Pay premium" (opens the editor with the policy chosen). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InsurancePolicyScreen(
    instrumentId: Long,
    onBack: () -> Unit,
    onPayPremium: (instrumentId: Long, kind: TransactionKind) -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: InsuranceViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val v = state.policies.firstOrNull { it.instrument.id == instrumentId }
    var editing by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(v?.title ?: "Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (v != null) IconButton(onClick = {
                        editing = true
                    }) { Icon(Icons.Filled.Edit, contentDescription = "Edit policy") }
                },
            )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            MoneyFormat.amount(v.paid),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Premiums paid (${v.premiums.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            StatCell(
                                "Premium",
                                p?.premiumAmount?.let {
                                    "${MoneyFormat.amount(it)} ${
                                        premiumFrequencyLabel(p.premiumFrequencyMonths)
                                    }"
                                } ?: "—")
                            StatCell(
                                "Next due",
                                v.nextDue?.format(SHORT_DATE) ?: "—",
                                v.dueIn?.let { if (it <= 30) -1.0 else null })
                        }
                        Row {
                            StatCell(
                                "Sum assured",
                                p?.sumAssured?.let { MoneyFormat.amount(it) } ?: "—")
                            StatCell("Received back", MoneyFormat.amount(v.received))
                        }
                        Row {
                            StatCell("Start", p?.startDate?.format(SHORT_DATE) ?: "—")
                            StatCell("Maturity", p?.maturityDate?.format(SHORT_DATE) ?: "—")
                        }
                        Row {
                            StatCell("Type", insuranceTypeTitle(v.type))
                            StatCell("Policy no", p?.policyNumber ?: "—")
                        }
                        Text(
                            if (v.countsAsInvestment) "Counts as an investment: premiums appear in the Investment Summary and the savings tables."
                            else "Not an investment: premiums are expenses and stay out of the investment totals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        v.instrument.notes?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = {
                                onPayPremium(
                                    v.instrument.id,
                                    if (v.countsAsInvestment) TransactionKind.INVEST else TransactionKind.EXPENSE
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Pay premium") }
                    }
                }
                Text(
                    "Premiums and payouts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                HorizontalDivider()
            }
            items(
                v.rows.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }),
                key = { it.id }) { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {}, onLongClick = { onEditTransaction(t.id) })
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
                    }
                    Text(
                        MoneyFormat.amount(t.amount, t.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider()
            }
        }
    }
    if (editing && v != null) PolicySheet(
        existing = v,
        categories = state.categories,
        onSave = { i, p -> viewModel.save(i, p); editing = false },
        onDismiss = { editing = false })
}

/** New or edited policy: the instrument (name, owner, category, notes) and the policy details. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicySheet(
    existing: PolicyView?,
    categories: List<InvestmentCategory>,
    onSave: (Instrument, InsurancePolicy) -> Unit,
    onDismiss: () -> Unit
) {
    val p = existing?.policy
    var type by remember { mutableStateOf(p?.insuranceType ?: InsuranceType.LIFE) }
    var provider by remember { mutableStateOf(p?.provider ?: "") }
    var policyName by remember { mutableStateOf(p?.policyName ?: "") }
    var number by remember { mutableStateOf(p?.policyNumber ?: "") }
    var sumAssured by remember {
        mutableStateOf(
            p?.sumAssured?.let(com.nagabhushansn95.financetracker.data.investments.FdMath::plain)
                ?: ""
        )
    }
    var premium by remember {
        mutableStateOf(
            p?.premiumAmount?.let(com.nagabhushansn95.financetracker.data.investments.FdMath::plain)
                ?: ""
        )
    }
    var frequency by remember { mutableStateOf(p?.premiumFrequencyMonths ?: 12) }
    var start by remember { mutableStateOf(p?.startDate) }
    var maturity by remember { mutableStateOf(p?.maturityDate) }
    var nextDue by remember { mutableStateOf(p?.nextDueDate) }
    var counts by remember { mutableStateOf(p?.countsAsInvestment ?: (type == InsuranceType.LIFE)) }
    var countsTouched by remember { mutableStateOf(p != null) }
    var owner by remember { mutableStateOf(existing?.instrument?.owner ?: Instrument.OWNER_SELF) }
    var active by remember { mutableStateOf(existing?.instrument?.active ?: true) }
    var categoryId by remember { mutableStateOf(existing?.instrument?.categoryId) }
    var notes by remember { mutableStateOf(existing?.instrument?.notes ?: "") }
    val name =
        listOf(provider.trim(), policyName.trim()).filter { it.isNotEmpty() }.joinToString(" - ")
    val frequencies = listOf(1, 3, 6, 12)

    FormSheet(
        title = if (existing == null) "New policy" else "Edit policy",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsuranceType.entries.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = {
                        type = t; if (!countsTouched) counts = t == InsuranceType.LIFE
                    },
                    label = { Text(insuranceTypeTitle(t)) })
            }
        }
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
            NumberField(premium, "Premium", { premium = it }, Modifier.weight(1f))
            NumberField(sumAssured, "Sum assured", { sumAssured = it }, Modifier.weight(1f))
        }
        ChoiceDropdown(
            "Paid",
            frequencies,
            frequency,
            { premiumFrequencyLabel(it) }) { frequency = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Start", start, { start = it }, Modifier.weight(1f), clearable = true)
            DateField(
                "Maturity",
                maturity,
                { maturity = it },
                Modifier.weight(1f),
                clearable = true
            )
        }
        DateField(
            "Next premium due",
            nextDue,
            { nextDue = it },
            Modifier.fillMaxWidth(),
            clearable = true
        )
        Text(
            "Leave the due date empty to take the last premium plus the payment interval.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = counts, onCheckedChange = { counts = it; countsTouched = true })
            Text("Counts as an investment (premiums are savings, not expenses)")
        }
        if (categories.isNotEmpty()) {
            val choices =
                listOf<InvestmentCategory?>(null) + categories.filter { it.parentId == null }
                    .flatMap { c -> listOf(c) + categories.filter { it.parentId == c.id } }
            val byId = categories.associateBy { it.id }
            ChoiceDropdown(
                "Category",
                choices,
                choices.firstOrNull { it?.id == categoryId },
                { c ->
                    c?.let { x ->
                        x.parentId?.let { byId[it]?.name }?.let { "$it › ${x.name}" } ?: x.name
                    } ?: "By type (${InvestmentCategories.LIFE_INSURANCE})"
                }) { categoryId = it?.id }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = owner,
                onValueChange = { owner = it },
                label = { Text("Owner") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (existing != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = active, onCheckedChange = { active = it })
            Text("Active (untick when matured or lapsed)")
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            val now = java.time.LocalDateTime.now()
            val instrument = (existing?.instrument ?: Instrument(
                name = name,
                instrumentType = InstrumentType.INSURANCE,
                displayOrder = 0,
                createdTime = now,
                modifiedTime = now
            ))
                .copy(
                    name = name,
                    owner = owner.trim().ifBlank { Instrument.OWNER_SELF },
                    categoryId = categoryId,
                    active = active,
                    notes = notes.ifBlank { null })
            onSave(
                instrument,
                InsurancePolicy(
                    instrumentId = instrument.id,
                    insuranceType = type,
                    provider = provider.trim().ifBlank { null },
                    policyName = policyName.trim().ifBlank { null },
                    policyNumber = number.trim().ifBlank { null },
                    sumAssured = sumAssured.toDoubleOrNull(),
                    premiumAmount = premium.toDoubleOrNull(),
                    premiumFrequencyMonths = frequency,
                    startDate = start,
                    maturityDate = maturity,
                    nextDueDate = nextDue,
                    countsAsInvestment = counts,
                ),
            )
        }) { Text("Save") }
    }
}
