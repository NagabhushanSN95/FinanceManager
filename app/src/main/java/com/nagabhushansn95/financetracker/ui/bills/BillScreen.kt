package com.nagabhushansn95.financetracker.ui.bills

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nagabhushansn95.financetracker.data.bills.BillPaymentView
import com.nagabhushansn95.financetracker.data.bills.BillPeriod
import com.nagabhushansn95.financetracker.data.bills.BillView
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillAlert
import com.nagabhushansn95.financetracker.data.db.entity.BillSchedule
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.ui.common.ConfirmDeleteDialog
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.LoadingOrMessage
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors
import java.time.LocalDate

/**
 * One bill: what falls due next and what it will cost, how it is paid and how often, then the history. The "+"
 * offers to record the payment (the transaction editor, filled in for the period), to link rows already in the
 * ledger, or to skip the period with nothing paid. Press and hold a history row to open, unlink or delete it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillScreen(
    billId: Long,
    onBack: () -> Unit,
    /** Opens the transaction editor to record the payment for the period starting on the date. */
    onRecordPayment: (Long, LocalDate) -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: BillsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val v = state.view(billId)
    val next = v?.next
    var editing by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var endOn by remember { mutableStateOf<LocalDate?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var skipping by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var unlinking by remember { mutableStateOf(false) }
    var actions by remember { mutableStateOf(false) }
    var rowMenu by remember { mutableStateOf<BillPaymentView?>(null) }
    var confirmDeletePayment by remember { mutableStateOf<BillPaymentView?>(null) }
    var changingPeriod by remember { mutableStateOf<BillPaymentView?>(null) }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (v != null) FloatingActionButton(onClick = { actions = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Record, link or skip")
            }
        },
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text(v?.bill?.name ?: "Bill") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (v != null) {
                        IconButton(onClick = { editing = true }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit"
                            )
                        }
                        IconButton(onClick = { menu = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More"
                            )
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (v.bill.ended) DropdownMenuItem(
                                text = { Text("Reopen") },
                                onClick = { menu = false; viewModel.reopen(v.bill.id) })
                            else DropdownMenuItem(
                                text = { Text("End this bill") },
                                onClick = { menu = false; confirmEnd = true })
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; confirmDelete = true })
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (v == null) {
            LoadingOrMessage(state.loaded, "Not found.", Modifier
                .fillMaxSize()
                .padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
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
                        val days = v.daysLeft(state.today)
                        Text(
                            v.expected?.let { MoneyFormat.amount(it) } ?: "Amount varies",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            Bills.status(v, state.today),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (days != null && days < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row {
                            StatCell("Next due", next?.due?.format(SHORT_DATE) ?: "—")
                            StatCell(
                                if (v.bill.income) "Last received" else "Last paid",
                                v.lastPaidOn?.format(SHORT_DATE) ?: "—"
                            )
                            StatCell(
                                if (v.bill.income) "Received so far" else "Paid so far",
                                v.payments.filter { !it.skipped }
                                    .distinctBy { it.payment.periodStart }.size.toString()
                            )
                        }
                        v.statementAmount?.let {
                            Text(
                                "Statement: ${MoneyFormat.amount(it)} (the card's balance on the statement day)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            Bills.scheduleText(v.bill, v.dueDay),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            paidHow(v, state),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (v.bill.alert != BillAlert.BEFORE_DUE) Text(
                            when {
                                v.bill.alert == BillAlert.NEVER -> "No alert on Home"
                                v.bill.income -> "Alerted only once late"
                                else -> "Alerted only once overdue"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        v.bill.endedOn?.let {
                            Text(
                                "Ended ${it.format(SHORT_DATE)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        v.bill.notes?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider()
            }
            if (v.payments.isEmpty()) item {
                Text(
                    if (v.bill.income) "No receipts yet." else "No payments yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(v.payments.asReversed(), key = { it.payment.id }) { p ->
                PaymentRow(p, state, onLongPress = { rowMenu = p })
            }
        }
    }

    if (editing && v != null) BillSheet(
        existing = v.bill,
        accounts = state.cashAccounts,
        categories = state.categories,
        onSave = { editing = false; viewModel.save(it) },
        onDismiss = { editing = false },
        nextDue = v.next?.due,
        paid = v.payments.isNotEmpty(),
    )
    if (actions && v != null) ModalBottomSheet(onDismissRequest = { actions = false }) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            if (next != null) ListItem(
                headlineContent = { Text(if (v.bill.income) "Record the receipt" else "Record a payment") },
                supportingContent = {
                    Text(
                        if (v.bill.schedule == BillSchedule.DURATION) "Opens the transaction editor filled in from the bill."
                        else "For ${next.label}; the editor lets you pick another period."
                    )
                },
                modifier = Modifier.clickable {
                    actions = false; onRecordPayment(
                    v.bill.id,
                    next.start
                )
                },
            )
            if (next != null) ListItem(
                headlineContent = { Text("Skip a period") },
                supportingContent = { Text("Periods that count as settled with nothing paid; past ones too.") },
                modifier = Modifier.clickable { actions = false; skipping = true },
            )
            ListItem(
                headlineContent = { Text("Link transactions") },
                supportingContent = { Text("Rows already in the ledger that paid this bill, past months included.") },
                modifier = Modifier.clickable { actions = false; linking = true },
            )
            if (v.payments.isNotEmpty()) ListItem(
                headlineContent = { Text("Unlink transactions") },
                supportingContent = { Text("Take rows out of the history; they stay in the ledger.") },
                modifier = Modifier.clickable { actions = false; unlinking = true },
            )
        }
    }
    if (unlinking && v != null) UnlinkTransactionsSheet(
        view = v,
        state = state,
        onUnlink = { unlinking = false; viewModel.unlinkAll(it) },
        onDismiss = { unlinking = false },
    )
    if (confirmEnd && v != null) AlertDialog(
        onDismissRequest = { confirmEnd = false },
        title = { Text("End ${v.bill.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nothing falls due after the date chosen; earlier periods stay as they are. What was paid stays in the history, and the bill can be reopened.")
                DateField("Ended on", endOn ?: state.today, { endOn = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                confirmEnd = false
                viewModel.end(v.bill.id, endOn ?: state.today)
                endOn = null
            }) { Text("End") }
        },
        dismissButton = {
            TextButton(onClick = {
                confirmEnd = false; endOn = null
            }) { Text("Cancel") }
        },
    )
    if (confirmDelete && v != null) ConfirmDeleteDialog(
        title = "Delete ${v.bill.name}?",
        text = if (v.payments.isEmpty()) "It has no history." else "Only a bill with no history can be deleted; end it instead so the ledger keeps its meaning.",
        onConfirm = { confirmDelete = false; viewModel.delete(v.bill.id, onBack) },
        onDismiss = { confirmDelete = false },
    )
    if (skipping && v?.next != null) SkipPeriodsSheet(
        view = v,
        onSkip = { periods, note -> skipping = false; viewModel.skipAll(v.bill.id, periods, note) },
        onDismiss = { skipping = false },
    )
    if (linking && v != null) LinkTransactionsSheet(
        view = v,
        state = state,
        onLink = { linking = false; viewModel.linkAll(v.bill, it) },
        onDismiss = { linking = false },
    )
    rowMenu?.let { p ->
        ModalBottomSheet(onDismissRequest = { rowMenu = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    p.period.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                val t = p.transaction
                if (t != null) {
                    ListItem(
                        headlineContent = { Text("Open the transaction") },
                        supportingContent = { Text("Change its date, amount or particulars; the period stays settled.") },
                        modifier = Modifier.clickable { rowMenu = null; onEditTransaction(t.id) },
                    )
                    ListItem(
                        headlineContent = { Text("Change the period") },
                        supportingContent = {
                            Text(
                                if (v?.bill?.schedule == BillSchedule.DURATION) "Set the dates this payment covers."
                                else "File this payment under another month."
                            )
                        },
                        modifier = Modifier.clickable { rowMenu = null; changingPeriod = p },
                    )
                    if (v?.bill?.schedule == BillSchedule.DURATION) ListItem(
                        headlineContent = { Text("Re-chain the later payments") },
                        supportingContent = { Text("Every payment made after this one runs again from its own date, each queued behind the one before.") },
                        modifier = Modifier.clickable {
                            rowMenu = null; viewModel.rechainAfter(p.payment.id)
                        },
                    )
                    ListItem(
                        headlineContent = { Text("Unlink from this bill") },
                        supportingContent = { Text("The transaction stays in the ledger; the period reads as unpaid again.") },
                        modifier = Modifier.clickable {
                            rowMenu = null; viewModel.unlink(p.payment.id)
                        },
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                "Delete the payment",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = { Text("The transaction goes too, with the balances put back.") },
                        modifier = Modifier.clickable { rowMenu = null; confirmDeletePayment = p },
                    )
                } else ListItem(
                    headlineContent = { Text("Remove the skip") },
                    supportingContent = { Text("The period reads as unpaid again.") },
                    modifier = Modifier.clickable {
                        rowMenu = null; viewModel.unlink(p.payment.id)
                    },
                )
            }
        }
    }
    changingPeriod?.let { p ->
        if (v != null && v.bill.schedule == BillSchedule.DURATION) ValidityPeriodDialog(
            title = "What does this payment cover?",
            initial = p.period,
            choices = Bills.validityChoices(v.bill, v.payments.map { it.payment }),
            onDismiss = { changingPeriod = null },
            onConfirm = { period ->
                changingPeriod = null
                viewModel.changePeriod(p.payment.id, period)
            },
        )
        else if (v != null) {
            val others = v.payments.filter { it.payment.id != p.payment.id }.map { it.payment }
            val choices = Bills.periodChoices(v.bill, others, state.today, v.dueDay)
            AlertDialog(
                onDismissRequest = { changingPeriod = null },
                title = { Text("Which period does it settle?") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        choices.forEach { period ->
                            val current = period.start == p.period.start
                            Text(
                                period.label + if (current) "  ✓" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        changingPeriod = null; if (!current) viewModel.changePeriod(
                                        p.payment.id,
                                        period
                                    )
                                    }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        changingPeriod = null
                    }) { Text("Cancel") }
                },
            )
        }
    }
    confirmDeletePayment?.let { p ->
        val t = p.transaction
        ConfirmDeleteDialog(
            title = "Delete this payment?",
            text = listOfNotNull(
                p.period.label,
                t?.date?.format(SHORT_DATE),
                t?.particulars?.takeIf { it.isNotBlank() },
                t?.let { MoneyFormat.amount(it.amount) },
            ).joinToString(" · ") + "\nThe transaction is deleted and the account balances adjusted back.",
            onConfirm = { confirmDeletePayment = null; viewModel.deletePayment(p.payment.id) },
            onDismiss = { confirmDeletePayment = null },
        )
    }
}

private fun paidHow(v: BillView, state: BillsState): String {
    val b = v.bill
    val parts = ArrayList<String>()
    parts += when (b.kind) {
        TransactionKind.TRANSFER -> "Transfer"
        TransactionKind.INCOME -> "Income"
        else -> "Expense"
    }
    b.fromAccountId?.let { parts += "from ${state.accountName(it)}" }
    b.toAccountId?.let { parts += "into ${state.accountName(it)}" }
    state.categoryPath(b.categoryId)?.let { parts += "under $it" }
    return parts.joinToString(" ")
}

/** The period, then the date, amount and account of the payment, or that it was skipped. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaymentRow(p: BillPaymentView, state: BillsState, onLongPress: () -> Unit) {
    val t = p.transaction
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(p.period.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                when {
                    t != null -> "${t.date.format(SHORT_DATE)} · ${state.accountName(t.fromAccountId ?: t.toAccountId)}" +
                            (t.particulars.takeIf { it.isNotBlank() && it != p.period.label }
                                ?.let { " · $it" } ?: "")

                    else -> "Skipped" + (p.payment.note?.let { " · $it" } ?: "")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            t?.let { MoneyFormat.amount(it.amount) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (t == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
    HorizontalDivider()
}

/** The unsettled periods with a box each, the upcoming one ticked; "Skip" settles the ticked ones with nothing paid. */
@Composable
private fun SkipPeriodsSheet(
    view: BillView,
    onSkip: (List<BillPeriod>, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val periods = view.unsettled
    var selected by remember { mutableStateOf(setOfNotNull(view.next?.start)) }
    var note by remember { mutableStateOf("") }
    FormSheet(title = "Skip a period", onDismiss = onDismiss, scrollable = false) {
        Text(
            "A skipped period counts as settled with nothing paid. Every period of ${view.bill.name} not settled yet is listed, the upcoming one ticked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${selected.size} of ${periods.size} ticked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                selected = periods.map { it.start }.toSet()
            }) { Text("Tick all") }
            TextButton(onClick = { selected = emptySet() }) { Text("Untick all") }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
        ) {
            items(periods, key = { it.start.toString() }) { p ->
                val on = p.start in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = if (on) selected - p.start else selected + p.start }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = {
                            selected = if (it) selected + p.start else selected - p.start
                        })
                    Text(
                        p.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "due ${p.due.format(SHORT_DATE)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optional)") },
            placeholder = { Text("Stayed with family") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val picked = periods.filter { it.start in selected }
        Button(
            enabled = picked.isNotEmpty(),
            onClick = { onSkip(picked, note.trim().ifBlank { null }) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (picked.size == 1) "Skip ${picked.single().label}" else "Skip ${picked.size} periods") }
    }
}

/** The bill's history with a box on each row; "Unlink" removes the ticked links (and skips), the ledger rows staying. */
@Composable
private fun UnlinkTransactionsSheet(
    view: BillView,
    state: BillsState,
    onUnlink: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = view.payments.asReversed()
    var selected by remember { mutableStateOf(setOf<Long>()) }
    FormSheet(title = "Unlink transactions", onDismiss = onDismiss, scrollable = false) {
        Text(
            "Ticked rows leave the history of ${view.bill.name} and their periods read as ${if (view.bill.income) "not received" else "unpaid"} again. The transactions stay in the ledger.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${selected.size} of ${rows.size} ticked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                selected = rows.map { it.payment.id }.toSet()
            }) { Text("Tick all") }
            TextButton(onClick = { selected = emptySet() }) { Text("Untick all") }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            items(rows, key = { it.payment.id }) { p ->
                val on = p.payment.id in selected
                val t = p.transaction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (on) selected - p.payment.id else selected + p.payment.id
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = {
                            selected = if (it) selected + p.payment.id else selected - p.payment.id
                        })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.period.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (t != null) "${t.date.format(SHORT_DATE)} · ${MoneyFormat.amount(t.amount)} · ${
                                t.particulars.ifBlank {
                                    state.accountName(
                                        t.fromAccountId ?: t.toAccountId
                                    )
                                }
                            }"
                            else "Skipped" + (p.payment.note?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
        Button(
            enabled = selected.isNotEmpty(),
            onClick = { onUnlink(selected.toList()) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (selected.size == 1) "Unlink 1 transaction" else "Unlink ${selected.size} transactions") }
    }
}

/** The search that opens the link sheet: the fixed start of the particulars pattern, else the bill's name; a card's is empty. */
internal fun defaultLinkQuery(bill: Bill, card: Boolean): String {
    val typed = bill.particulars?.trim().orEmpty()
    if (typed.isNotEmpty()) return typed.substringBefore(Bills.PERIOD_TOKEN)
        .trimEnd { !it.isLetterOrDigit() && it != ')' }
    return if (card) "" else bill.name
}

/**
 * Rows of the bill's kind that pay no bill yet, newest first, narrowed by the search (the last six months when it
 * is empty). Rows written from the bill's particulars pattern come ticked; each shows the period it would settle,
 * the one its particulars name or the one nearest its date. "Link" files every ticked row.
 */
@Composable
private fun LinkTransactionsSheet(
    view: BillView,
    state: BillsState,
    onLink: (List<Pair<BillPeriod, Long>>) -> Unit,
    onDismiss: () -> Unit,
) {
    val bill = view.bill
    val card = bill.toAccountId?.let { state.accounts[it] }?.accountType == AccountType.CREDIT_CARD
    var query by rememberSaveable { mutableStateOf(defaultLinkQuery(bill, card)) }
    val q = query.trim()
    val since = state.today.minusMonths(6)
    // Rows already in this bill's history are out; one that pays another bill may pay this one too (rent and
    // maintenance paid together), and says so.
    val linkedHere =
        remember(view.payments) { view.payments.mapNotNull { it.payment.transactionId }.toSet() }
    val candidates = remember(state.ledger, linkedHere, q, bill) {
        // A typed search reaches every expense and transfer row, whatever its account: older history may have been
        // written differently. An empty one shows the bill's own kind and account over the last six months.
        state.ledger.asSequence()
            .filter { it.id !in linkedHere }
            .filter {
                if (q.isEmpty()) it.kind == bill.kind && (bill.toAccountId == null || it.toAccountId == bill.toAccountId) && (bill.toAccountId != null || !it.date.isBefore(
                    since
                ))
                else (if (bill.income) it.kind == TransactionKind.INCOME else it.kind == TransactionKind.EXPENSE || it.kind == TransactionKind.TRANSFER) &&
                        it.particulars.contains(q, ignoreCase = true)
            }
            .take(400)
            .toList()
    }
    val matching = remember(candidates) {
        candidates.filter { Bills.matchesPattern(bill, it) }.map { it.id }.toSet()
    }
    var selected by remember(matching) { mutableStateOf(matching) }
    val payments = view.payments.map { it.payment }
    val periods: Map<Long, BillPeriod> = remember(candidates, selected) {
        val rows =
            if (bill.schedule == BillSchedule.CALENDAR) candidates else candidates.filter { it.id in selected }
        Bills.assignPeriods(bill, rows, payments, view.dueDay)
            .associate { it.first.id to it.second }
    }
    val links = candidates.filter { it.id in selected }
        .mapNotNull { t -> periods[t.id]?.let { it to t.id } }

    /** Periods with more than one ticked row, or one already settled: worth a look before linking. */
    val settled = payments.map { it.periodStart }.toSet()
    val crowded = links.groupBy { it.first.start }
        .filter { (start, rows) -> rows.size > 1 || start in settled }.keys

    FormSheet(title = "Link transactions", onDismiss = onDismiss, scrollable = false) {
        Text(
            "Rows that ${if (bill.income) "brought in" else "paid"} ${bill.name} but are not linked to it yet. Those written from its particulars pattern are ticked; " +
                    "each is filed under the period its particulars name, else the one nearest its date. A search covers every ${if (bill.income) "income" else "expense and transfer"} row; " +
                    (if (bill.toAccountId != null) "cleared, the list is every transfer into ${
                        state.accountName(
                            bill.toAccountId
                        )
                    }." else "cleared, the list is the bill's own kind over the last six months."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search particulars") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (candidates.isNotEmpty()) Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${links.size} of ${candidates.size} ticked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                selected = candidates.map { it.id }.toSet()
            }) { Text("Tick all") }
            TextButton(onClick = { selected = emptySet() }) { Text("Untick all") }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
        ) {
            if (candidates.isEmpty()) item {
                Text(
                    "Nothing to link.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            items(candidates, key = { it.id }) { t ->
                val on = t.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = if (on) selected - t.id else selected + t.id }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = {
                            selected = if (it) selected + t.id else selected - t.id
                        })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            t.particulars.ifBlank { "(no particulars)" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${t.date.format(SHORT_DATE)} · ${MoneyFormat.amount(t.amount)} · ${
                                state.accountName(
                                    t.fromAccountId ?: t.toAccountId
                                )
                            }" + (if (t.id in state.linkedTransactionIds) " · also pays another bill" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    periods[t.id]?.let {
                        Text(
                            it.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                on && it.start in crowded -> MaterialTheme.colorScheme.error
                                on -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                HorizontalDivider()
            }
        }
        // Always present so ticking a row never moves the layout; it only changes colour when something needs a look.
        Text(
            if (crowded.isNotEmpty()) "In red: a period with more than one row ticked, or one already paid. Check those rows' particulars before linking."
            else "A period shows in red when more than one ticked row lands on it, or it is already paid.",
            style = MaterialTheme.typography.bodySmall,
            color = if (crowded.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2,
        )
        Button(
            enabled = links.isNotEmpty(),
            onClick = { onLink(links) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (links.size == 1) "Link 1 transaction" else "Link ${links.size} transactions") }
    }
}
