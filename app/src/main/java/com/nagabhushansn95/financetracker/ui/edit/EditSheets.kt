package com.nagabhushansn95.financetracker.ui.edit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.InvestmentOrder
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.summary.SummaryViewModel
import com.nagabhushansn95.financetracker.ui.transaction.TransactionEditorViewModel.Companion.plain
import com.nagabhushansn95.financetracker.ui.transaction.label
import com.nagabhushansn95.financetracker.ui.transaction.typeTitle
import java.time.LocalDateTime

@Composable
private fun NumberField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = value.isNotBlank() && value.trim().toDoubleOrNull() == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DeleteRow(label: String, note: String?, onDelete: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    note?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/** Add or edit an account. [initialType] seeds the type for a new one. */
@Composable
fun AccountSheet(
    existing: Account?,
    initialType: AccountType,
    transactionCount: Int?,
    onSave: (Account) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.accountType ?: initialType) }
    var currency by remember { mutableStateOf(existing?.currency ?: "INR") }
    var number by remember { mutableStateOf(existing?.accountNumber ?: "") }
    var balance by remember { mutableStateOf(existing?.let { plain(it.balance) } ?: "") }
    var limit by remember { mutableStateOf(existing?.creditLimit?.let { plain(it) } ?: "") }
    var statementDay by remember { mutableStateOf(existing?.statementDay?.toString() ?: "") }
    var dueDay by remember { mutableStateOf(existing?.dueDay?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    val balanceValue = if (balance.isBlank()) 0.0 else balance.trim().toDoubleOrNull()
    val valid = name.isNotBlank() && balanceValue != null && currency.trim().length == 3 &&
            (limit.isBlank() || limit.trim().toDoubleOrNull() != null) &&
            (statementDay.isBlank() || statementDay.trim()
                .toIntOrNull() in 1..31) && (dueDay.isBlank() || dueDay.trim()
        .toIntOrNull() in 1..31)

    FormSheet(
        title = if (existing == null) "New account" else "Edit account",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryViewModel.SECTION_ORDER.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(SummaryViewModel.titleOf(t)) })
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                balance,
                if (type == AccountType.CREDIT_CARD) "Balance (negative = outstanding)" else "Balance",
                { balance = it },
                Modifier.weight(2f)
            )
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                label = { Text("Currency") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (existing != null && balanceValue != null && balanceValue != existing.balance) {
            Text(
                "Changing the balance here does not create a transaction. Prefer recording the difference as income or expense.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
            )
        }
        if (type != AccountType.WALLET) {
            OutlinedTextField(
                value = number, onValueChange = { number = it },
                label = { Text(if (type == AccountType.CREDIT_CARD) "Last digits (optional)" else "Account number (optional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        if (type == AccountType.CREDIT_CARD) {
            NumberField(limit, "Credit limit (optional)", { limit = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = statementDay,
                    onValueChange = { statementDay = it },
                    label = { Text("Statement day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it },
                    label = { Text("Due day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = valid,
            onClick = {
                val now = LocalDateTime.now()
                onSave(
                    (existing ?: Account(
                        name = "",
                        accountType = type,
                        displayOrder = 0,
                        createdTime = now,
                        modifiedTime = now
                    )).copy(
                        name = name.trim(),
                        accountType = type,
                        currency = currency.trim(),
                        balance = balanceValue!!,
                        accountNumber = number.trim().takeIf { it.isNotEmpty() },
                        creditLimit = limit.trim().toDoubleOrNull()
                            .takeIf { type == AccountType.CREDIT_CARD },
                        statementDay = statementDay.trim().toIntOrNull()
                            .takeIf { type == AccountType.CREDIT_CARD },
                        dueDay = dueDay.trim().toIntOrNull()
                            .takeIf { type == AccountType.CREDIT_CARD },
                        notes = notes.trim().takeIf { it.isNotEmpty() },
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        if (onDelete != null && existing != null) {
            DeleteRow(
                label = "Delete account",
                note = buildString {
                    append("Deleted accounts are hidden from pickers and the Summary, but their ")
                    append(transactionCount?.let { "%,d ".format(it) } ?: "")
                    append("transactions stay. They can be restored later.")
                    if (existing.balance != 0.0) append(" This account still has a balance.")
                },
                onDelete = onDelete,
            )
        }
    }
}

/** Add or edit a category. [parents] are the top-level candidates for the parent field. */
@Composable
fun CategorySheet(
    existing: Category?,
    parents: List<Category>,
    initialScope: CategoryScope,
    initialParentId: Long?,
    transactionCount: Int?,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var scope by remember { mutableStateOf(existing?.appliesTo ?: initialScope) }
    var parentId by remember { mutableStateOf(existing?.parentId ?: initialParentId) }
    var parentMenu by remember { mutableStateOf(false) }
    val candidates = parents.filter { it.id != existing?.id }

    FormSheet(
        title = if (existing == null) "New category" else "Edit category",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Applies to", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryScope.entries.forEach { s ->
                FilterChip(
                    selected = scope == s,
                    onClick = { scope = s },
                    label = { Text(scopeTitle(s)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Parent",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(72.dp)
            )
            TextButton(onClick = {
                parentMenu = true
            }) { Text(candidates.firstOrNull { it.id == parentId }?.name ?: "None (top level)") }
            DropdownMenu(expanded = parentMenu, onDismissRequest = { parentMenu = false }) {
                DropdownMenuItem(
                    text = { Text("None (top level)") },
                    onClick = { parentId = null; parentMenu = false })
                candidates.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name) },
                        onClick = { parentId = c.id; parentMenu = false })
                }
            }
        }
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                onSave(
                    (existing ?: Category(
                        name = "",
                        displayOrder = 0
                    )).copy(name = name.trim(), appliesTo = scope, parentId = parentId)
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        if (onDelete != null && existing != null) {
            DeleteRow(
                label = "Delete category",
                note = "Deleted categories are hidden from pickers; their ${
                    transactionCount?.let {
                        "%,d ".format(
                            it
                        )
                    } ?: ""
                }transactions keep it. Restorable later.",
                onDelete = onDelete,
            )
        }
    }
}

fun scopeTitle(scope: CategoryScope) = when (scope) {
    CategoryScope.EXPENSE -> "Expense"
    CategoryScope.INCOME -> "Income"
    CategoryScope.BOTH -> "Both"
}

/** Add or edit an instrument's identity. Deposit and policy details come with the Investments screens. */
@Composable
fun InstrumentSheet(
    existing: Instrument?,
    initialType: InstrumentType,
    transactionCount: Int?,
    categories: List<InvestmentCategory> = emptyList(),
    onSave: (Instrument) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var categoryMenu by remember { mutableStateOf(false) }
    val categoryById = categories.associateBy { it.id }
    fun categoryLabel(id: Long?) = id?.let { categoryById[it] }?.let { c ->
        c.parentId?.let { categoryById[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
    } ?: "Choose a category"
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var shortName by remember { mutableStateOf(existing?.shortName ?: "") }
    var type by remember { mutableStateOf(existing?.instrumentType ?: initialType) }
    var symbol by remember { mutableStateOf(existing?.symbol ?: "") }
    var isin by remember { mutableStateOf(existing?.isin ?: "") }
    var exchange by remember { mutableStateOf(existing?.exchange ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: "INR") }
    var owner by remember { mutableStateOf(existing?.owner ?: Instrument.OWNER_SELF) }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    val tradable = type == InstrumentType.STOCK || type == InstrumentType.MUTUAL_FUND

    FormSheet(
        title = if (existing == null) "New instrument" else "Edit instrument",
        onDismiss = onDismiss
    ) {
        // The kind is chosen once: a deposit edited into a stock would lose its terms and its screen. A new deposit
        // or policy needs its terms, so those are opened from their own Investments screens.
        if (existing == null) Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InvestmentOrder.types(InvestmentOrder.names(categories))
                .filter { it in InvestmentOrder.CREATABLE_HERE }.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(typeTitle(t)) })
            }
        } else Text(
            typeTitle(type),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = shortName,
            onValueChange = { shortName = it },
            label = { Text("Short name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // The category stays within the kind's own group (a fund is a kind of mutual fund, never a stock type).
        // Policies and deposits are filed by their own screens, so they offer no choice here.
        val group = categories.firstOrNull {
            it.parentId == null && it.name.equals(
                com.nagabhushansn95.financetracker.data.investments.InvestmentCategories.defaultCategoryName(
                    type
                ), ignoreCase = true
            )
        }
        if (group != null && type != InstrumentType.INSURANCE && type != InstrumentType.FIXED_DEPOSIT) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(72.dp)
                )
                TextButton(onClick = { categoryMenu = true }) { Text(categoryLabel(categoryId)) }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = { categoryId = group.id; categoryMenu = false })
                    categories.filter { it.parentId == group.id }.forEach { child ->
                        DropdownMenuItem(
                            text = { Text("    ${child.name}") },
                            onClick = { categoryId = child.id; categoryMenu = false })
                    }
                }
            }
        }
        if (tradable) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text(if (type == InstrumentType.STOCK) "Symbol" else "Scheme code") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = exchange,
                    onValueChange = { exchange = it.uppercase() },
                    label = { Text("Exchange") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = isin,
                onValueChange = { isin = it.uppercase() },
                label = { Text("ISIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                label = { Text("Currency") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = owner,
                onValueChange = { owner = it },
                label = { Text("Owner") },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
        }
        if (owner.trim() != Instrument.OWNER_SELF) {
            Text(
                "Owned by someone else: excluded from your totals and statistics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        if (existing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Active",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(checked = active, onCheckedChange = { active = it })
            }
            Text(
                "Inactive instruments (sold out, matured) leave the pickers; their ${
                    transactionCount?.let {
                        "%,d ".format(
                            it
                        )
                    } ?: ""
                }transactions stay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            enabled = name.isNotBlank() && currency.trim().length == 3 && owner.isNotBlank(),
            onClick = {
                val now = LocalDateTime.now()
                onSave(
                    (existing ?: Instrument(
                        name = "",
                        instrumentType = type,
                        displayOrder = 0,
                        createdTime = now,
                        modifiedTime = now
                    )).copy(
                        name = name.trim(),
                        shortName = shortName.trim().takeIf { it.isNotEmpty() },
                        instrumentType = type,
                        symbol = symbol.trim().takeIf { it.isNotEmpty() && tradable },
                        isin = isin.trim().takeIf { it.isNotEmpty() && tradable },
                        exchange = exchange.trim().takeIf { it.isNotEmpty() && tradable },
                        currency = currency.trim(),
                        owner = owner.trim(),
                        categoryId = categoryId,
                        active = active,
                        notes = notes.trim().takeIf { it.isNotEmpty() },
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}

/** Add or edit an investment category: name, parent, risk bucket and the Investment Summary targets. */
@Composable
fun InvestmentCategorySheet(
    existing: InvestmentCategory?,
    parents: List<InvestmentCategory>,
    riskLevels: List<RiskLevel>,
    onSave: (InvestmentCategory) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var parentId by remember { mutableStateOf(existing?.parentId) }
    var parentMenu by remember { mutableStateOf(false) }
    var riskId by remember { mutableStateOf(existing?.riskLevelId) }
    var notes by remember { mutableStateOf(existing?.rebalanceNotes ?: "") }
    val candidates = parents.filter { it.id != existing?.id }

    FormSheet(
        title = if (existing == null) "New investment category" else "Edit investment category",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Parent",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(72.dp)
            )
            TextButton(onClick = {
                parentMenu = true
            }) { Text(candidates.firstOrNull { it.id == parentId }?.name ?: "None (top level)") }
            DropdownMenu(expanded = parentMenu, onDismissRequest = { parentMenu = false }) {
                DropdownMenuItem(
                    text = { Text("None (top level)") },
                    onClick = { parentId = null; parentMenu = false })
                candidates.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name) },
                        onClick = { parentId = c.id; parentMenu = false })
                }
            }
        }
        Text("Risk level", style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            riskLevels.forEach { r ->
                FilterChip(
                    selected = riskId == r.id,
                    onClick = { riskId = if (riskId == r.id) null else r.id },
                    label = { Text(r.name) })
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Rebalance notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "The risk level and the goal, wealth and tolerance percentages can also be set on Investments › Targets.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                onSave(
                    (existing ?: InvestmentCategory(name = "", displayOrder = 0)).copy(
                        name = name.trim(),
                        parentId = parentId,
                        riskLevelId = riskId,
                        rebalanceNotes = notes.trim().takeIf { it.isNotEmpty() },
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        if (onDelete != null && existing != null) DeleteRow(
            "Delete category",
            "Only possible when no instrument or sub-category uses it.",
            onDelete
        )
    }
}

/** Add or rename a risk level. Levels are ordered least to most risky by dragging them in the list. */
@Composable
fun RiskLevelSheet(
    existing: RiskLevel?,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    FormSheet(
        title = if (existing == null) "New risk level" else "Edit risk level",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = name.isNotBlank(),
            onClick = { onSave(name.trim()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
        if (onDelete != null && existing != null) DeleteRow(
            "Delete risk level",
            "Only possible when no investment category uses it.",
            onDelete
        )
    }
}

/**
 * Add or edit a template: what the editor fills in when the template is picked. Every field but the particulars
 * and kind is optional. A template carries a rate and a quantity, never an amount: a rate alone fills only the
 * rate, rate and quantity together fill the amount as well.
 */
@Composable
fun TemplateSheet(
    existing: com.nagabhushansn95.financetracker.data.db.entity.Template?,
    accounts: List<Account>,
    categories: List<Category>,
    instruments: List<Instrument>,
    onSave: (com.nagabhushansn95.financetracker.data.db.entity.Template) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var particulars by remember { mutableStateOf(existing?.particulars ?: "") }
    var kind by remember { mutableStateOf(existing?.kind ?: TransactionKind.EXPENSE) }
    var fromId by remember { mutableStateOf(existing?.fromAccountId) }
    var toId by remember { mutableStateOf(existing?.toAccountId) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var instrumentId by remember { mutableStateOf(existing?.instrumentId) }
    var rate by remember { mutableStateOf(existing?.rate?.let { plain(it) } ?: "") }
    var quantity by remember { mutableStateOf(existing?.quantity?.let { plain(it) } ?: "") }
    var hidden by remember { mutableStateOf(existing?.hidden ?: false) }
    val showsFrom =
        kind == TransactionKind.EXPENSE || kind == TransactionKind.TRANSFER || kind == TransactionKind.INVEST
    val showsTo =
        kind == TransactionKind.INCOME || kind == TransactionKind.TRANSFER || kind == TransactionKind.REDEEM
    val showsCategory = kind == TransactionKind.EXPENSE || kind == TransactionKind.INCOME
    val showsInstrument = kind != TransactionKind.TRANSFER
    val scope =
        if (kind == TransactionKind.INCOME) CategoryScope.INCOME else CategoryScope.EXPENSE
    val applicable =
        categories.filter { it.appliesTo == scope || it.appliesTo == CategoryScope.BOTH }
    val categoryById = applicable.associateBy { it.id }
    fun categoryPath(id: Long?): String? = id?.let { categoryById[it] }?.let { c ->
        c.parentId?.let { categoryById[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
    }

    val categoryChoices: List<Long?> =
        listOf<Long?>(null) + applicable.filter { it.parentId == null }
            .flatMap { p -> listOf(p.id) + applicable.filter { it.parentId == p.id }.map { it.id } }
    val instrumentChoices: List<Long?> = listOf<Long?>(null) + instruments.map { it.id }
    val rateValue = rate.trim().toDoubleOrNull()
    val quantityValue = quantity.trim().toDoubleOrNull()
    val valid =
        particulars.isNotBlank() && (rate.isBlank() || rateValue != null && rateValue >= 0) &&
                (quantity.isBlank() || quantityValue != null && quantityValue > 0)

    FormSheet(
        title = if (existing == null) "New template" else "Edit template",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k },
                    label = { Text(k.label) })
            }
        }
        OutlinedTextField(
            value = particulars,
            onValueChange = { particulars = it },
            label = { Text("Particulars") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        if (showsFrom) com.nagabhushansn95.financetracker.ui.common.AccountDropdown(
            if (kind == TransactionKind.INVEST) "Paid from" else "From",
            accounts, fromId, { fromId = it }, none = "Not set"
        )
        if (showsTo) com.nagabhushansn95.financetracker.ui.common.AccountDropdown(
            if (kind == TransactionKind.REDEEM) "Credited to" else "To",
            accounts, toId, { toId = it }, none = "Not set"
        )
        if (showsCategory) com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown(
            "Category",
            categoryChoices,
            categoryId.takeIf { it in categoryById },
            { categoryPath(it) ?: "Not set" }) { categoryId = it }
        if (showsInstrument) com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown(
            "Instrument",
            instrumentChoices,
            instrumentId.takeIf { id -> instruments.any { it.id == id } },
            { id -> instruments.firstOrNull { it.id == id }?.name ?: "Not set" }) {
            instrumentId = it
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(rate, "Rate (optional)", { rate = it }, Modifier.weight(1f))
            NumberField(quantity, "Quantity (optional)", { quantity = it }, Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Hidden from suggestions",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(checked = hidden, onCheckedChange = { hidden = it })
        }
        Text(
            "Picking the template in the editor fills these in; fields left \"Not set\" or empty keep what is already typed. " +
                    "With only a rate the quantity is yours to type (one if left empty); with both, the amount is filled too.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = valid,
            onClick = {
                onSave(
                    com.nagabhushansn95.financetracker.data.db.entity.Template(
                        id = existing?.id ?: 0,
                        particulars = particulars.trim(),
                        kind = kind,
                        fromAccountId = fromId.takeIf { showsFrom },
                        toAccountId = toId.takeIf { showsTo },
                        categoryId = categoryId.takeIf { showsCategory && it in categoryById },
                        instrumentId = instrumentId.takeIf { showsInstrument },
                        rate = rateValue,
                        quantity = quantityValue,
                        hidden = hidden,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        if (onDelete != null && existing != null) DeleteRow("Delete template", null, onDelete)
    }
}
