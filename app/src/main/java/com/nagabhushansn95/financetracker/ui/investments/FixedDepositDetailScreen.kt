package com.nagabhushansn95.financetracker.ui.investments

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FdType
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEvent
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEventKind
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.FdLedgerEntry
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.FdView
import com.nagabhushansn95.financetracker.data.investments.FixedDeposits
import com.nagabhushansn95.financetracker.ui.common.AccountDropdown
import com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown
import com.nagabhushansn95.financetracker.ui.common.ConfirmDeleteDialog
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.DigitsField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.LoadingOrMessage
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors
import java.time.LocalDate

/**
 * One fixed deposit: terms, computed maturity and today's value, the whole renewal chain it belongs to, the ledger
 * of every deposit in that chain (oldest first, so it reads opened → renewed → …), and the actions Renew / Matured /
 * Close early while it is open. Press and hold a ledger row to edit the transaction.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FixedDepositDetailScreen(
    instrumentId: Long,
    onBack: () -> Unit,
    onOpenDeposit: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: FixedDepositsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }
    val d = state.deposits.firstOrNull { it.instrument.id == instrumentId }
    val byId = state.deposits.associateBy { it.instrument.id }
    val chain = FixedDeposits.chain(instrumentId, byId)
    var editing by remember { mutableStateOf(false) }

    /** The outcome sheet is open when set: RENEWED, CLOSED_AT_MATURITY or CLOSED_PREMATURELY to start from. */
    var outcome by remember { mutableStateOf<FdStatus?>(null) }
    var renewalToEdit by remember { mutableStateOf<FdLedgerEntry.Renewal?>(null) }
    var addingEvent by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<HoldingEvent?>(null) }
    var confirmDeleteEvent by remember { mutableStateOf<HoldingEvent?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text(d?.instrument?.name ?: "Fixed deposit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (d != null) IconButton(onClick = {
                        editing = true
                    }) { Icon(Icons.Filled.Edit, contentDescription = "Edit terms") }
                    if (d != null) IconButton(onClick = { confirmingDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete deposit")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (d != null) FloatingActionButton(onClick = { addingEvent = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add ledger entry")
            }
        },
    ) { padding ->
        if (d == null) {
            LoadingOrMessage(
                state.loaded,
                "Deposit not found.",
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item { ValueCard(d) }
            item { TermsCard(d) }
            if (chain.size > 1) item { ChainCard(d, chain, onOpenDeposit) }
            if (d.isOpen) item {
                // One flow for the end of a deposit; the sheet starts on what the dates suggest.
                Button(
                    onClick = {
                        outcome =
                            if (d.matured) FdStatus.CLOSED_AT_MATURITY else FdStatus.CLOSED_PREMATURELY
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) { Text(if (d.matured) "Record the maturity…" else "Close or renew…") }
            }
            if (d.terms == null) item {
                Text(
                    "No terms recorded yet. Use the pencil to add the bank, period and rate; the value today and the maturity amount follow from them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            val chained = chain.size > 1
            item {
                Text(
                    if (chained) "Ledger of the chain" else "Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
            }
            val ledger = FixedDeposits.ledger(if (chained) chain else listOf(d))
            items(ledger, key = { e ->
                when (e) {
                    is FdLedgerEntry.Row -> "t${e.row.id}"
                    is FdLedgerEntry.Renewal -> "r${e.redeem.id}"
                    is FdLedgerEntry.Derived -> "d${e.deposit.instrument.id}/${e.tax}"
                    is FdLedgerEntry.Event -> "e${e.event.id}"
                    is FdLedgerEntry.SweepIn -> "s${e.deposit.instrument.id}"
                }
            }) { e ->
                val own = e.deposit.instrument.id == instrumentId
                val name = if (chained) e.deposit.instrument.name else null
                val title: String
                val detail: String
                var amount: Double? = null
                var muted = false
                var tax = false
                // Second line: the date, the deposit's name when the chain is shown, and only what the first line and
                // the amount do not already say — the bank account the money came from or went to.
                when (e) {
                    is FdLedgerEntry.Row -> {
                        title = e.row.particulars
                        val account = when (e.row.kind) {
                            TransactionKind.INVEST, TransactionKind.EXPENSE -> e.row.fromAccountId?.let {
                                "from ${
                                    state.lookups?.accountName(
                                        it
                                    )
                                }"
                            }

                            TransactionKind.REDEEM, TransactionKind.INCOME -> e.row.toAccountId?.let {
                                "to ${
                                    state.lookups?.accountName(
                                        it
                                    )
                                }"
                            }

                            TransactionKind.TRANSFER -> null
                        }
                        detail = listOfNotNull(name, account).joinToString(" · ")
                        amount = e.row.amount
                        tax = e.row.kind == TransactionKind.EXPENSE
                    }

                    is FdLedgerEntry.Renewal -> {
                        val terms = e.into.terms
                        title =
                            "Renewed ${e.deposit.instrument.name} into ${e.into.instrument.name}"
                        detail = listOfNotNull(
                            terms?.bankName?.takeIf { it != e.deposit.bank },
                            listOfNotNull(
                                e.into.periodLabel,
                                terms?.rate?.let { "@ ${FdMath.plain(it)}%" })
                                .joinToString(" ").ifEmpty { null },
                            e.into.maturityDate?.let { "matures ${it.format(SHORT_DATE)}" },
                        ).joinToString(" · ")
                        amount = e.amount
                    }

                    is FdLedgerEntry.Derived -> {
                        title = e.label
                        detail = listOfNotNull(
                            name,
                            if (e.tax) "as reported at closing" else "estimated from the amount paid out"
                        ).joinToString(" · ")
                        amount = e.amount
                        muted = true
                        tax = e.tax
                    }

                    is FdLedgerEntry.Event -> {
                        title = if (e.tax) "Tax deducted (TDS)" else "Interest credited"
                        detail = listOfNotNull(
                            name,
                            e.event.notes?.takeIf { it.isNotBlank() }).joinToString(" · ")
                        amount = e.event.amount
                        tax = e.tax
                    }

                    is FdLedgerEntry.SweepIn -> {
                        title = "Converted to sweep-in"
                        detail = listOfNotNull(
                            name,
                            "the bank breaks it in parts to cover debits"
                        ).joinToString(" · ")
                        muted = true
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = {}, onLongClick = {
                            when (e) {
                                is FdLedgerEntry.Row -> onEditTransaction(e.row.id)
                                is FdLedgerEntry.Renewal -> renewalToEdit = e
                                is FdLedgerEntry.Derived, is FdLedgerEntry.SweepIn -> Unit
                                is FdLedgerEntry.Event -> eventToEdit = e.event
                            }
                        })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (chained && own && !muted) FontWeight.Medium else null,
                            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            listOf(e.date.format(SHORT_DATE), detail).filter { it.isNotEmpty() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (amount != null) Text(
                        (if (tax) "− " else "") + MoneyFormat.amount(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (muted) null else FontWeight.Medium,
                        color = when {
                            tax -> MaterialTheme.colorScheme.error
                            e is FdLedgerEntry.Derived || e is FdLedgerEntry.Event -> MaterialTheme.colorScheme.tertiary
                            e is FdLedgerEntry.Row && e.row.kind == TransactionKind.INCOME -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                HorizontalDivider()
            }
        }
    }

    renewalToEdit?.let { r ->
        ModalBottomSheet(onDismissRequest = { renewalToEdit = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "A renewal is two rows. Which one?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ListItem(
                    headlineContent = { Text("The payout of ${r.deposit.instrument.name}") },
                    supportingContent = { Text(r.redeem.particulars) },
                    modifier = Modifier.clickable {
                        onEditTransaction(r.redeem.id); renewalToEdit = null
                    },
                )
                ListItem(
                    headlineContent = { Text("The deposit into ${r.into.instrument.name}") },
                    supportingContent = { Text(r.invest.particulars) },
                    modifier = Modifier.clickable {
                        onEditTransaction(r.invest.id); renewalToEdit = null
                    },
                )
            }
        }
    }

    if ((addingEvent || eventToEdit != null) && d != null) FdEventSheet(
        deposit = d,
        existing = eventToEdit,
        onSave = { viewModel.saveEvent(it); addingEvent = false; eventToEdit = null },
        onDelete = eventToEdit?.let { ev -> { confirmDeleteEvent = ev } },
        onDismiss = { addingEvent = false; eventToEdit = null },
    )
    confirmDeleteEvent?.let { ev ->
        ConfirmDeleteDialog(
            title = "Delete this ${ev.kind.name.lowercase().replace('_', ' ')}?",
            text = "${ev.date.format(SHORT_DATE)}\nThe deposit's figures are recomputed without it.",
            onConfirm = {
                viewModel.deleteEvent(ev.id); eventToEdit = null; confirmDeleteEvent = null
            },
            onDismiss = { confirmDeleteEvent = null },
        )
    }
    if (editing && d != null) FixedDepositSheet(
        existing = d,
        defaultName = d.instrument.name,
        accounts = state.accounts,
        suggestions = remember(state.deposits) { FdSuggestions.of(state.deposits) },
        onCreate = {},
        onSaveTerms = { i, t -> viewModel.saveTerms(i, t, d.depositDate); editing = false },
        onDismiss = { editing = false },
        onSaveTemplate = { viewModel.saveTemplate(it) },
    )
    if (confirmingDelete && d != null) {
        val next = d.terms?.nextFdId?.let { byId[it] }
        val previous = d.terms?.previousFdId?.let { byId[it] }
        if (next != null) AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Cannot delete ${d.instrument.name}") },
            text = { Text("It was renewed into ${next.instrument.name}. Delete that deposit first; this one then becomes active again and can be deleted.") },
            confirmButton = { TextButton(onClick = { confirmingDelete = false }) { Text("OK") } },
        ) else AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${d.instrument.name}?") },
            text = {
                Text(
                    buildString {
                        val what = listOfNotNull(
                            if (d.rows.size == 1) "its transaction" else "its ${d.rows.size} transactions",
                            when (d.events.size) {
                                0 -> null
                                1 -> "its ledger entry"
                                else -> "its ${d.events.size} ledger entries"
                            },
                        ).joinToString(" and ")
                        append("The deposit goes along with ${what}; the account balances are put back.")
                        if (previous != null) append(" ${previous.instrument.name}, which was renewed into it, becomes active again and its payout rows are removed.")
                        append(" This cannot be undone.")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    viewModel.delete(d.instrument.id)
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                }) { Text("Cancel") }
            },
        )
    }
    outcome?.let { start ->
        if (d != null) DepositOutcomeSheet(
            d,
            state.accounts,
            viewModel.nextName(),
            initial = start,
            // The bank account the money came from: this deposit's opening row, or the first deposit of the chain's.
            sourceAccountId = (chain.ifEmpty { listOf(d) }).flatMap { it.rows }
                .filter { it.kind == TransactionKind.INVEST }
                .sortedWith(compareBy({ it.date }, { it.id }))
                .firstNotNullOfOrNull { it.fromAccountId },
            onClose = { date, amount, to, status, tds ->
                viewModel.close(d.instrument.id, date, amount, to, status, tds); outcome = null
            },
            onRenew = { date, matured, renewed, to, name, terms, tds ->
                viewModel.renew(
                    d.instrument.id,
                    date,
                    matured,
                    renewed,
                    to,
                    name,
                    terms,
                    tds
                ); outcome = null
            },
            onDismiss = { outcome = null })
    }
}

@Composable
private fun ValueCard(d: FdView) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    MoneyFormat.amount(if (d.isOpen) d.estimated else d.position.totalRedeemed),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    fdStatusText(d),
                    style = MaterialTheme.typography.labelLarge,
                    color = fdStatusColor(d)
                )
            }
            Text(
                if (d.isOpen) "Estimated value today" else "Amount received",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                StatCell(
                    "Deposited",
                    MoneyFormat.amount(d.principal.takeIf { it > 0 } ?: d.position.totalInvested))
                if (d.isOpen) StatCell(
                    "Interest so far",
                    MoneyFormat.amount(d.interestSoFar),
                    d.interestSoFar
                )
                else StatCell(
                    "Interest earned",
                    MoneyFormat.amount(d.interestEarned),
                    d.interestEarned
                )
            }
            Row {
                StatCell("Deposit date", d.depositDate?.format(SHORT_DATE) ?: "—")
                StatCell("Maturity date", d.maturityDate?.format(SHORT_DATE) ?: "—")
            }
            d.maturityAmount?.let { m ->
                Row {
                    StatCell("At maturity", MoneyFormat.amount(m))
                    StatCell("TDS (estimated)", MoneyFormat.amount(d.tdsAtMaturity))
                    StatCell("After TDS", MoneyFormat.amount(d.maturityAfterTds ?: m))
                }
            }
            if (d.terms?.maturityAmountPrinted != null || d.terms?.tdsReported != null) Row {
                StatCell(
                    "Printed on receipt",
                    d.terms.maturityAmountPrinted?.let { MoneyFormat.amount(it) } ?: "—")
                StatCell(
                    "TDS reported by bank",
                    d.terms.tdsReported?.let { MoneyFormat.amount(it) } ?: "—")
            }
        }
    }
}

@Composable
private fun TermsCard(d: FdView) {
    val t = d.terms ?: return
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row {
                StatCell(
                    "Bank",
                    listOfNotNull(t.bankName, t.branch).joinToString(", ").ifEmpty { "—" })
                StatCell("Rate", t.rate?.let { "${FdMath.plain(it)}% p.a." } ?: "—")
                StatCell("Period", d.periodLabel ?: "—")
            }
            Row {
                StatCell("Compounds every", t.compoundingMonths?.let { "$it months" } ?: "—")
                StatCell("Holder", t.holderName ?: "—")
                StatCell("Owner", d.instrument.owner)
            }
            Row {
                StatCell("FD account no", t.bankAccountNo ?: "—")
                StatCell("Customer ID", t.custId ?: "—")
            }
            if (t.depositType == FdType.SWEEP_IN) Row {
                StatCell("Type", "Sweep-in")
                StatCell("Sweep-in from", t.sweepInDate?.format(SHORT_DATE) ?: "—")
            }
            if (t.certificateNo != null) Row { StatCell("Certificate", t.certificateNo) }
            (d.instrument.notes ?: t.notes)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Every deposit of the chain, oldest first; the one on screen is marked, the others open on tap. */
@Composable
private fun ChainCard(d: FdView, chain: List<FdView>, onOpen: (Long) -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Renewal chain · ${chain.size} deposits",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            chain.forEachIndexed { i, link ->
                ChainLink(i + 1, link, current = link.instrument.id == d.instrument.id, onOpen)
            }
        }
    }
}

@Composable
private fun ChainLink(index: Int, link: FdView, current: Boolean, onOpen: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (current) it else it.clickable { onOpen(link.instrument.id) } }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$index",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                listOfNotNull(
                    link.instrument.name,
                    MoneyFormat.compact(link.principal.takeIf { p -> p > 0 }
                        ?: link.position.totalInvested),
                    link.rate?.let { "${FdMath.plain(it)}%" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (current) FontWeight.Medium else null,
                color = if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            )
            Text(
                listOfNotNull(
                    link.depositDate?.format(SHORT_DATE),
                    link.maturityDate?.format(SHORT_DATE)
                )
                    .joinToString(" → ").ifEmpty { "no dates" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (current) "This one" else fdStatusText(link),
            style = MaterialTheme.typography.bodySmall,
            color = if (current) MaterialTheme.colorScheme.onSurfaceVariant else fdStatusColor(link),
        )
    }
}

/** Interest credited into the deposit or TDS deducted from it, on a date, with a note; neither moves cash. */
@Composable
private fun FdEventSheet(
    deposit: FdView,
    existing: HoldingEvent?,
    onSave: (HoldingEvent) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(existing?.kind ?: HoldingEventKind.INTEREST_CREDITED) }
    var date by remember { mutableStateOf<LocalDate?>(existing?.date ?: LocalDate.now()) }
    var amount by remember { mutableStateOf(existing?.amount?.let(FdMath::plain) ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    val value = amount.toDoubleOrNull()
    FormSheet(
        title = if (existing == null) "Add ledger entry" else "Edit ledger entry",
        onDismiss = onDismiss
    ) {
        Text(
            "What the bank did inside the deposit, as its statement or interest certificate shows it. Interest credited stays in the deposit; tax deducted leaves it. Neither touches a bank account of yours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChoiceDropdown(
            "Entry",
            listOf(HoldingEventKind.INTEREST_CREDITED, HoldingEventKind.TDS_DEDUCTED),
            kind,
            { it.title },
        ) { kind = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Date", date, { date = it }, Modifier.weight(1f))
            NumberField(amount, "Amount", { amount = it }, Modifier.weight(1f))
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = date != null && value != null && value > 0,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    (existing ?: HoldingEvent(
                        instrumentId = deposit.instrument.id,
                        date = date!!,
                        kind = kind,
                        amount = value!!
                    ))
                        .copy(
                            date = date!!,
                            kind = kind,
                            amount = value!!,
                            notes = notes.trim().ifBlank { null })
                )
            },
        ) { Text(if (existing == null) "Add" else "Save") }
        if (onDelete != null) OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete entry", color = MaterialTheme.colorScheme.error)
        }
    }
}

/** What to renew: everything the bank paid, or the principal alone with the interest going to a bank account. */
private enum class RenewMode(val title: String) {
    ALL("The whole matured amount"), PRINCIPAL("Only the principal; the interest to the bank")
}

/**
 * The end of a deposit in one sheet: Renewed, Matured or Closed early as a segmented choice (starting from
 * [initial]), the date and amount the bank paid, optional TDS, then either the account it was credited to or the
 * renewal: how much goes into the new deposit (all of it, or the principal with the interest paid out) and the new
 * terms. The two bottom buttons read as the record being made: "Record renewal", "Record maturity", "Record closure".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepositOutcomeSheet(
    d: FdView,
    accounts: List<Account>,
    defaultName: String,
    initial: FdStatus,
    sourceAccountId: Long?,
    onClose: (LocalDate, Double, Long?, FdStatus, Double?) -> Unit,
    onRenew: (LocalDate, Double, Double, Long?, String, FixedDeposit, Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = d.terms
    // Credited back to the account the deposit was paid from; failing that, the first bank account.
    val bankAccount = sourceAccountId?.takeIf { id -> accounts.any { it.id == id } }
        ?: accounts.firstOrNull { it.accountType == com.nagabhushansn95.financetracker.data.db.entity.AccountType.BANK }?.id
    val principal = d.principal.takeIf { it > 0 } ?: d.position.totalInvested
    val atMaturityAmount =
        Math.round(d.maturityAfterTds ?: d.maturityAmount ?: d.estimated).toDouble()
    val todayAmount = Math.round(d.estimated).toDouble()
    var outcome by remember { mutableStateOf(initial) }
    var date by remember {
        mutableStateOf<LocalDate?>(
            if (initial == FdStatus.CLOSED_PREMATURELY) LocalDate.now() else d.maturityDate
                ?: LocalDate.now()
        )
    }
    var amount by remember {
        mutableStateOf(FdMath.plain(if (initial == FdStatus.CLOSED_PREMATURELY) todayAmount else atMaturityAmount))
    }
    var tds by remember {
        mutableStateOf(
            if (initial != FdStatus.CLOSED_PREMATURELY && d.tdsAtMaturity > 0) FdMath.plain(
                Math.round(d.tdsAtMaturity).toDouble()
            ) else ""
        )
    }
    var account by remember { mutableStateOf(if (d.instrument.owner == Instrument.OWNER_SELF) bankAccount else null) }
    var renewMode by remember { mutableStateOf(RenewMode.ALL) }
    var renewed by remember { mutableStateOf(FdMath.plain(atMaturityAmount)) }
    var name by remember { mutableStateOf(defaultName) }
    var months by remember { mutableStateOf(t?.periodMonths?.toString() ?: "12") }
    var days by remember { mutableStateOf(t?.periodDays?.toString() ?: "") }
    var rate by remember { mutableStateOf(t?.rate?.let(FdMath::plain) ?: "") }
    var accountNo by remember { mutableStateOf(t?.bankAccountNo ?: "") }
    var printed by remember { mutableStateOf("") }

    fun choose(o: FdStatus) {
        val wasEarly = outcome == FdStatus.CLOSED_PREMATURELY
        val isEarly = o == FdStatus.CLOSED_PREMATURELY
        if (wasEarly != isEarly) {
            date = if (isEarly) LocalDate.now() else d.maturityDate ?: LocalDate.now()
            amount = FdMath.plain(if (isEarly) todayAmount else atMaturityAmount)
            tds = if (!isEarly && d.tdsAtMaturity > 0) FdMath.plain(
                Math.round(d.tdsAtMaturity).toDouble()
            ) else ""
            if (renewMode == RenewMode.ALL) renewed = amount
        }
        outcome = o
    }

    val amountValue = amount.toDoubleOrNull()
    val renewedValue = renewed.toDoubleOrNull()
    val remainder =
        if (amountValue != null && renewedValue != null) amountValue - renewedValue else 0.0
    val newMaturity =
        date?.let { FdMath.maturityDate(it, months.toIntOrNull(), days.toIntOrNull()) }
    val renewing = outcome == FdStatus.RENEWED
    val ok = date != null && amountValue != null && amountValue >= 0 && (!renewing ||
            (renewedValue != null && renewedValue > 0 && renewedValue <= amountValue + 0.005 && name.isNotBlank()))

    FormSheet(title = "What happened to ${d.instrument.name}?", onDismiss = onDismiss) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(
                FdStatus.RENEWED to "Renewed",
                FdStatus.CLOSED_AT_MATURITY to "Matured",
                FdStatus.CLOSED_PREMATURELY to "Closed early",
            ).forEachIndexed { i, (status, label) ->
                SegmentedButton(
                    selected = outcome == status,
                    onClick = { choose(status) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                ) { Text(label) }
            }
        }
        Text(
            when (outcome) {
                FdStatus.RENEWED -> "The matured amount leaves this deposit. What is renewed goes straight into the new deposit without touching a bank account; the rest is credited to the bank."
                FdStatus.CLOSED_AT_MATURITY -> "Records the payout: the amount the bank credited after TDS. The interest becomes the deposit's realised profit."
                else -> "Records the early payout; banks usually pay a lower rate. Enter what was actually credited."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(
                if (renewing) "Renewal date" else "Date",
                date,
                { date = it },
                Modifier.weight(1f)
            )
            NumberField(
                amount,
                if (renewing) "Matured amount" else "Amount credited",
                { amount = it; if (renewing && renewMode == RenewMode.ALL) renewed = it },
                Modifier.weight(1f)
            )
        }
        NumberField(tds, "TDS deducted (optional)", { tds = it }, Modifier.fillMaxWidth())
        if (!renewing) {
            AccountDropdown(
                "Credited to",
                accounts,
                account,
                { account = it },
                none = "No account of ours"
            )
            Button(
                enabled = ok,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onClose(
                        date!!,
                        amountValue!!,
                        account,
                        outcome,
                        tds.toDoubleOrNull()
                    )
                },
            ) { Text(if (outcome == FdStatus.CLOSED_AT_MATURITY) "Record maturity" else "Record closure") }
        } else {
            ChoiceDropdown("Renew", RenewMode.entries, renewMode, { it.title }) { mode ->
                renewMode = mode
                renewed = FdMath.plain(
                    if (mode == RenewMode.ALL) amountValue ?: atMaturityAmount else principal
                )
            }
            NumberField(
                renewed,
                "Renewed into the new deposit",
                { renewed = it },
                Modifier.fillMaxWidth()
            )
            if (remainder > 0.005) AccountDropdown(
                "${MoneyFormat.amount(remainder)} to",
                accounts,
                account,
                { account = it },
                none = "No account of ours"
            )
            Text("New deposit", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                DigitsField(accountNo, "FD account no", { accountNo = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(months, "Months", { months = it }, Modifier.weight(1f))
                NumberField(days, "Days", { days = it }, Modifier.weight(1f))
                NumberField(rate, "Rate %", { rate = it }, Modifier.weight(1f))
            }
            NumberField(
                printed,
                "Printed maturity amount (optional)",
                { printed = it },
                Modifier.fillMaxWidth()
            )
            Text(
                listOfNotNull(
                    newMaturity?.let { "Matures ${it.format(SHORT_DATE)}" },
                    t?.bankName?.let { "bank $it" },
                    "other details carried over"
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(enabled = ok, modifier = Modifier.fillMaxWidth(), onClick = {
                onRenew(
                    date!!,
                    amountValue!!,
                    renewedValue!!,
                    account.takeIf { remainder > 0.005 },
                    name.trim(),
                    FixedDeposit(
                        instrumentId = 0,
                        periodMonths = months.toIntOrNull()?.takeIf { it > 0 },
                        periodDays = days.toIntOrNull()?.takeIf { it > 0 },
                        rate = rate.toDoubleOrNull(),
                        compoundingMonths = t?.compoundingMonths,
                        bankAccountNo = accountNo.ifBlank { null },
                        maturityAmountPrinted = printed.toDoubleOrNull(),
                    ),
                    tds.toDoubleOrNull(),
                )
            }) { Text("Record renewal") }
        }
    }
}
