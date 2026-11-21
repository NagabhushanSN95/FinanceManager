package com.nagabhushansn95.financetracker.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillSchedule
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.ui.common.AccountField
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.NumberField
import com.nagabhushansn95.financetracker.ui.transaction.TransactionEditorViewModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Add or edit a bill: what it is, how it is paid (an expense under a category, or a transfer into a card or another
 * account), what is expected, the particulars pattern, and the schedule: every so many months with the next due
 * date, or a validity that runs from each payment. A card bill follows its account's due day and needs no date.
 *
 * [nextDue] is what the bill screen shows as the next due date when editing (null for a new bill); changing it moves
 * the due day, not the start of the history. [paid] says whether the bill has payments already.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSheet(
    existing: Bill?,
    accounts: List<Account>,
    categories: List<Category>,
    onSave: (Bill) -> Unit,
    onDismiss: () -> Unit,
    nextDue: LocalDate? = null,
    paid: Boolean = false,
) {
    val today = remember { LocalDate.now() }
    val shownDue = nextDue ?: existing?.firstDue ?: today
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var kind by remember { mutableStateOf(existing?.kind ?: TransactionKind.EXPENSE) }
    var fromId by remember { mutableStateOf(existing?.fromAccountId) }
    var toId by remember { mutableStateOf(existing?.toAccountId) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var amount by remember {
        mutableStateOf(
            existing?.amount?.let(TransactionEditorViewModel::plain) ?: ""
        )
    }
    var particulars by remember { mutableStateOf(TextFieldValue(existing?.particulars ?: "")) }
    var schedule by remember { mutableStateOf(existing?.schedule ?: BillSchedule.CALENDAR) }
    var everyMonths by remember { mutableStateOf(existing?.everyMonths?.toString() ?: "1") }
    var due by remember { mutableStateOf<LocalDate?>(shownDue) }
    var previousPeriod by remember { mutableStateOf(existing?.coversPrevious ?: false) }
    var validityDays by remember { mutableStateOf(existing?.validityDays?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var alertWhenOverdue by remember { mutableStateOf(existing?.alertWhenOverdue ?: false) }

    val months = everyMonths.trim().toIntOrNull()
    val days = validityDays.trim().toIntOrNull()
    val amountValue = amount.trim().toDoubleOrNull()
    val card =
        if (kind == TransactionKind.TRANSFER) toId?.let { id -> accounts.firstOrNull { it.id == id } }
            ?.takeIf { it.accountType == AccountType.CREDIT_CARD } else null

    /** A card with a due day sets the schedule itself. */
    val cardDue = card?.dueDay
    val needsDate =
        cardDue == null && !(schedule == BillSchedule.DURATION && existing != null && paid)
    val problem: String? = when {
        name.isBlank() -> "Enter a name"
        needsDate && due == null -> "Pick the next due date"
        amount.isNotBlank() && (amountValue == null || amountValue < 0) -> "Enter a valid amount"
        schedule == BillSchedule.CALENDAR && (months == null || months < 1) -> "Enter the months between due dates"
        schedule == BillSchedule.DURATION && (days == null || days < 1) -> "Enter the validity in days"
        kind == TransactionKind.TRANSFER && fromId != null && fromId == toId -> "Choose two different accounts"
        else -> null
    }

    FormSheet(title = if (existing == null) "New bill" else "Edit bill", onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            placeholder = { Text("Home Rent") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(
                TransactionKind.EXPENSE to "Expense",
                TransactionKind.TRANSFER to "Transfer"
            ).forEachIndexed { i, (k, label) ->
                SegmentedButton(
                    selected = kind == k,
                    onClick = { kind = k },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(label) },
                )
            }
        }
        AccountField(
            "Paid from",
            accounts.filter { it.id != toId || kind != TransactionKind.TRANSFER },
            fromId,
            { fromId = it },
            Modifier.fillMaxWidth(),
            none = "Chosen each time",
            grouped = true
        )
        if (kind == TransactionKind.TRANSFER) {
            AccountField(
                "Paid into", accounts.filter { it.id != fromId }, toId,
                onPick = { picked ->
                    val was = card
                    toId = picked
                    val now = picked?.let { id -> accounts.firstOrNull { it.id == id } }
                        ?.takeIf { it.accountType == AccountType.CREDIT_CARD }
                    // A card names the bill after itself unless a name was typed.
                    if (now != null && (name.isBlank() || (was != null && name == cardBillName(was)))) name =
                        cardBillName(now)
                },
                Modifier.fillMaxWidth(), none = "Chosen each time", grouped = true,
            )
            if (card != null) Text(
                if (cardDue != null) "A card: due on the ${Bills.ordinal(cardDue)} of every month for the month before, as set under Manage › Accounts. The expected amount is what the statement asked for."
                else "A card: set its statement and due days under Manage › Accounts and the bill follows them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else CategoryField(categories, categoryId, { categoryId = it }, Modifier.fillMaxWidth())
        NumberField(amount, "Expected amount (optional)", { amount = it }, Modifier.fillMaxWidth())
        OutlinedTextField(
            value = particulars,
            onValueChange = { particulars = it },
            label = { Text("Particulars (optional)") },
            placeholder = { Text("${name.ifBlank { "Home Rent" }} - ${Bills.PERIOD_TOKEN}") },
            supportingText = {
                Text(
                    if (particulars.text.contains(Bills.PERIOD_TOKEN)) "${Bills.PERIOD_TOKEN} becomes the period paid for, as in \"October 2026\"."
                    else "Left empty, the name is used with the period: \"${name.ifBlank { "Home Rent" }} - October 2026\"."
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!particulars.text.contains(Bills.PERIOD_TOKEN)) AssistChip(
            onClick = {
                val at = particulars.selection.end.coerceIn(0, particulars.text.length)
                val before = particulars.text.substring(0, at)
                val after = particulars.text.substring(at)
                val glue = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
                val text = before + glue + Bills.PERIOD_TOKEN + after
                particulars = TextFieldValue(
                    text,
                    TextRange(before.length + glue.length + Bills.PERIOD_TOKEN.length)
                )
            },
            label = { Text("Insert ${Bills.PERIOD_TOKEN}") },
        )
        Text("Schedule", style = MaterialTheme.typography.labelLarge)
        if (cardDue == null) SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(
                BillSchedule.CALENDAR to "By the calendar",
                BillSchedule.DURATION to "By validity"
            ).forEachIndexed { i, (s, label) ->
                SegmentedButton(
                    selected = schedule == s,
                    onClick = { schedule = s },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(label) },
                )
            }
        }
        when {
            cardDue != null -> Text(
                "Monthly on the ${Bills.ordinal(cardDue)}, from the card's account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            schedule == BillSchedule.CALENDAR -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("Next due date", due, { due = it }, Modifier.weight(1f))
                    OutlinedTextField(
                        value = everyMonths,
                        onValueChange = { everyMonths = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Every (months)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Due on that day every ${months ?: "…"} month(s): 1 monthly, 3 quarterly, 12 yearly. " +
                            "A day past a short month's end falls on its last day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Each payment is for", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        false to "The current period",
                        true to "The previous period"
                    ).forEachIndexed { i, (v, label) ->
                        SegmentedButton(
                            selected = previousPeriod == v,
                            onClick = { previousPeriod = v },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            label = { Text(label) },
                        )
                    }
                }
            }

            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (needsDate) DateField(
                        "Next due date",
                        due,
                        { due = it },
                        Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = validityDays,
                        onValueChange = { validityDays = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Validity (days)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Each payment holds this long (asked again when it is recorded, since packs differ); the next falls due when it runs out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text("Alert on Home", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(
                false to "Before it is due",
                true to "Only once overdue"
            ).forEachIndexed { i, (v, label) ->
                SegmentedButton(
                    selected = alertWhenOverdue == v,
                    onClick = { alertWhenOverdue = v },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(label) },
                )
            }
        }
        Text(
            if (alertWhenOverdue) "Nothing is said until the due date has passed: for a bill that can wait a few days."
            else "Alerted in the run-up set under Settings › Alerts › Bills, and while overdue.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        problem?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            enabled = problem == null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val now = LocalDateTime.now()
                val isCard = cardDue != null
                val draft = Bill(
                    id = existing?.id ?: 0,
                    name = name.trim(),
                    kind = kind,
                    fromAccountId = fromId,
                    toAccountId = toId.takeIf { kind == TransactionKind.TRANSFER },
                    categoryId = categoryId.takeIf { kind == TransactionKind.EXPENSE },
                    amount = amountValue,
                    particulars = particulars.text.trim().ifBlank { null },
                    schedule = if (isCard) BillSchedule.CALENDAR else schedule,
                    everyMonths = if (isCard) 1 else if (schedule == BillSchedule.CALENDAR) months!! else 1,
                    firstDue = existing?.firstDue ?: today,
                    coversPrevious = isCard || (schedule == BillSchedule.CALENDAR && previousPeriod),
                    validityDays = if (!isCard && schedule == BillSchedule.DURATION) days else null,
                    alertWhenOverdue = alertWhenOverdue,
                    endedOn = existing?.endedOn,
                    notes = notes.trim().ifBlank { null },
                    createdTime = existing?.createdTime ?: now,
                    modifiedTime = now,
                )
                val firstDue = when {
                    isCard -> existing?.firstDue ?: Bills.comingDue(today, cardDue)
                    existing == null -> due!!
                    !needsDate || due == shownDue -> existing.firstDue
                    draft.schedule == BillSchedule.DURATION -> due!!
                    else -> Bills.firstDueFor(draft, shownDue, due!!)
                }
                onSave(draft.copy(firstDue = firstDue))
            },
        ) { Text(if (existing == null) "Add" else "Save") }
    }
}

/** The name a card bill gets unless one is typed: "SBI Card bill". */
fun cardBillName(card: Account): String = "${card.name} bill"

/** Expense categories (and those for both) as "Parent › Child", with a none entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryField(
    categories: List<Category>,
    selected: Long?,
    onPick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val applicable =
        categories.filter { it.appliesTo == CategoryScope.EXPENSE || it.appliesTo == CategoryScope.BOTH }
    val byId = applicable.associateBy { it.id }
    fun path(c: Category) = c.parentId?.let { byId[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
    val rows = applicable.filter { it.parentId == null }
        .flatMap { p -> listOf(p) + applicable.filter { it.parentId == p.id } }
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { byId[it] }?.let(::path) ?: "Chosen each time",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Chosen each time") },
                onClick = { onPick(null); open = false })
            rows.forEach { c ->
                DropdownMenuItem(text = { Text(path(c)) }, onClick = { onPick(c.id); open = false })
            }
        }
    }
}
