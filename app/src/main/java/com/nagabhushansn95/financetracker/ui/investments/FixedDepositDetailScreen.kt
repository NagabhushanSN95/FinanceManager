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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.FdView
import com.nagabhushansn95.financetracker.ui.common.AccountDropdown
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.common.SHORT_DATE
import com.nagabhushansn95.financetracker.ui.common.StatCell
import java.time.LocalDate

/**
 * One fixed deposit: terms, computed maturity and today's value, the renewal chain, its ledger, and the actions
 * Renew / Matured / Close early while it is open. Press and hold a ledger row to edit the transaction.
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
    var editing by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf<FdStatus?>(null) }
    var renewing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
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
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (d == null) {
            Text(
                if (state.loaded) "Deposit not found." else "Loading…",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item { ValueCard(d) }
            item { TermsCard(d) }
            if (d.terms?.previousFdId != null || d.terms?.nextFdId != null) item {
                ChainCard(
                    d,
                    byId,
                    onOpenDeposit
                )
            }
            if (d.isOpen) item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { renewing = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Renew") }
                    OutlinedButton(
                        onClick = { closing = FdStatus.CLOSED_AT_MATURITY },
                        modifier = Modifier.weight(1f)
                    ) { Text("Matured") }
                    OutlinedButton(
                        onClick = { closing = FdStatus.CLOSED_PREMATURELY },
                        modifier = Modifier.weight(1f)
                    ) { Text("Close early") }
                }
            }
            if (d.terms == null) item {
                Text(
                    "No terms recorded yet. Use the pencil to add the bank, period and rate; the value today and the maturity amount follow from them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            item {
                Text(
                    "Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
            }
            items(
                d.rows.sortedWith(compareByDescending<com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity> { it.date }.thenByDescending { it.id }),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    if (editing && d != null) FixedDepositSheet(
        existing = d,
        defaultName = d.instrument.name,
        accounts = state.accounts,
        categories = state.categories,
        onCreate = {},
        onSaveTerms = { i, t -> viewModel.saveTerms(i, t, d.depositDate); editing = false },
        onDismiss = { editing = false },
    )
    closing?.let { status ->
        if (d != null) CloseDepositSheet(
            d,
            status,
            state.accounts,
            onClose = { date, amount, to, tds ->
                viewModel.close(d.instrument.id, date, amount, to, status, tds); closing = null
            },
            onDismiss = { closing = null })
    }
    if (renewing && d != null) RenewDepositSheet(
        d,
        state.accounts,
        viewModel.nextName(),
        onRenew = { date, matured, renewed, to, name, terms ->
            viewModel.renew(d.instrument.id, date, matured, renewed, to, name, terms); renewing =
            false
        },
        onDismiss = { renewing = false })
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
                    MoneyFormat.amount(d.position.realised),
                    d.position.realised
                )
            }
            Row {
                StatCell("Deposit date", d.depositDate?.format(SHORT_DATE) ?: "—")
                StatCell("Maturity date", d.maturityDate?.format(SHORT_DATE) ?: "—")
            }
            d.maturityAmount?.let { m ->
                Row {
                    StatCell("At maturity", MoneyFormat.amount(m))
                    StatCell("TDS @ ${d.terms?.tdsRate?.let { FdMath.plain(if (it <= 1.0) it * 100 else it) } ?: "0"}%",
                        MoneyFormat.amount(d.tdsAtMaturity))
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

@Composable
private fun ChainCard(d: FdView, byId: Map<Long, FdView>, onOpen: (Long) -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Renewal chain",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            d.terms?.previousFdId?.let { id -> ChainLink("Renewed from", byId[id], id, onOpen) }
            d.terms?.nextFdId?.let { id -> ChainLink("Renewed into", byId[id], id, onOpen) }
        }
    }
}

@Composable
private fun ChainLink(label: String, other: FdView?, id: Long, onOpen: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(id) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            other?.let {
                listOfNotNull(
                    it.instrument.name,
                    it.depositDate?.format(SHORT_DATE),
                    MoneyFormat.compact(it.principal.takeIf { p -> p > 0 }
                        ?: it.position.totalInvested)).joinToString(" · ")
            } ?: "deposit #$id",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Matured or closed early: date, amount credited, the bank account, optional TDS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloseDepositSheet(
    d: FdView, status: FdStatus, accounts: List<Account>,
    onClose: (LocalDate, Double, Long?, Double?) -> Unit, onDismiss: () -> Unit,
) {
    val matured = status == FdStatus.CLOSED_AT_MATURITY
    var date by remember {
        mutableStateOf<LocalDate?>(
            if (matured) d.maturityDate ?: LocalDate.now() else LocalDate.now()
        )
    }
    var amount by remember {
        mutableStateOf(
            FdMath.plain(
                Math.round(
                    (if (matured) d.maturityAfterTds ?: d.maturityAmount
                    ?: d.estimated else d.estimated)
                ).toDouble()
            )
        )
    }
    var tds by remember {
        mutableStateOf(
            if (matured && d.tdsAtMaturity > 0) FdMath.plain(
                Math.round(
                    d.tdsAtMaturity
                ).toDouble()
            ) else ""
        )
    }
    var account by remember { mutableStateOf(if (d.instrument.owner == Instrument.OWNER_SELF) accounts.firstOrNull { it.accountType == com.nagabhushansn95.financetracker.data.db.entity.AccountType.BANK }?.id else null) }
    val value = amount.toDoubleOrNull()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (matured) "Deposit matured" else "Close before maturity",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                if (matured) "Records the payout: the amount the bank credited after TDS. The interest becomes the deposit's realised profit."
                else "Records the early payout; banks usually pay a lower rate. Enter what was actually credited.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Date", date, { date = it }, Modifier.weight(1f))
                NumberField(amount, "Amount credited", { amount = it }, Modifier.weight(1f))
            }
            AccountDropdown(
                "Credited to",
                accounts,
                account,
                { account = it },
                none = "No account of ours"
            )
            NumberField(tds, "TDS deducted (optional)", { tds = it }, Modifier.fillMaxWidth())
            Button(
                enabled = date != null && value != null && value >= 0,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onClose(date!!, value!!, account, tds.toDoubleOrNull()) }) {
                Text(if (matured) "Record maturity" else "Close deposit")
            }
        }
    }
}

/** Renewal: matured amount, how much goes into the new deposit, where the rest goes, and the new terms. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenewDepositSheet(
    d: FdView,
    accounts: List<Account>,
    defaultName: String,
    onRenew: (LocalDate, Double, Double, Long?, String, FixedDeposit) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = d.terms
    var date by remember { mutableStateOf<LocalDate?>(d.maturityDate ?: LocalDate.now()) }
    val defaultMatured =
        Math.round(d.maturityAfterTds ?: d.maturityAmount ?: d.estimated).toDouble()
    var matured by remember { mutableStateOf(FdMath.plain(defaultMatured)) }
    var renewed by remember { mutableStateOf(FdMath.plain(defaultMatured)) }
    var account by remember { mutableStateOf(accounts.firstOrNull { it.accountType == com.nagabhushansn95.financetracker.data.db.entity.AccountType.BANK }?.id) }
    var name by remember { mutableStateOf(defaultName) }
    var months by remember { mutableStateOf(t?.periodMonths?.toString() ?: "12") }
    var days by remember { mutableStateOf(t?.periodDays?.toString() ?: "") }
    var rate by remember { mutableStateOf(t?.rate?.let(FdMath::plain) ?: "") }
    var accountNo by remember { mutableStateOf(t?.bankAccountNo ?: "") }
    var printed by remember { mutableStateOf("") }
    val maturedValue = matured.toDoubleOrNull()
    val renewedValue = renewed.toDoubleOrNull()
    val remainder =
        if (maturedValue != null && renewedValue != null) maturedValue - renewedValue else 0.0
    val newMaturity =
        date?.let { FdMath.maturityDate(it, months.toIntOrNull(), days.toIntOrNull()) }
    val ok =
        date != null && maturedValue != null && renewedValue != null && renewedValue > 0 && renewedValue <= maturedValue + 0.005 && name.isNotBlank()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Renew ${d.instrument.name}", style = MaterialTheme.typography.titleLarge)
            Text(
                "The matured amount leaves this deposit; the renewed part goes straight into the new one without touching a bank account. Anything left over is credited to the bank.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Renewal date", date, { date = it }, Modifier.weight(1f))
                NumberField(matured, "Matured amount", { matured = it }, Modifier.weight(1f))
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
                OutlinedTextField(
                    value = accountNo,
                    onValueChange = { accountNo = it },
                    label = { Text("FD account no") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(months, "Months", { months = it }, Modifier.weight(1f))
                NumberField(days, "Days", { days = it }, Modifier.weight(1f))
                NumberField(rate, "Rate %", { rate = it }, Modifier.weight(1f))
            }
            NumberField(
                printed,
                "Maturity amount (printed, optional)",
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
                    maturedValue!!,
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
                )
            }) { Text("Renew") }
        }
    }
}
