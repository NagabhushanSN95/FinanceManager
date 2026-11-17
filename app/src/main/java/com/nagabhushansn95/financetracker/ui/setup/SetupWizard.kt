package com.nagabhushansn95.financetracker.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.seed.DefaultData
import com.nagabhushansn95.financetracker.data.seed.NewAccount
import com.nagabhushansn95.financetracker.data.seed.SetupChoices
import com.nagabhushansn95.financetracker.ui.theme.appTopBarColors

private class AccountDraft(name: String = "") {
    var name by mutableStateOf(name)
    var amount by mutableStateOf("")   // balance, or outstanding for a credit card
    var number by mutableStateOf("")
    var limit by mutableStateOf("")

    val isBlank get() = name.isBlank() && amount.isBlank() && number.isBlank() && limit.isBlank()
    val amountValue get() = amount.trim().toDoubleOrNull() ?: 0.0
    val limitValue get() = limit.trim().toDoubleOrNull()
    val amountInvalid get() = amount.isNotBlank() && amount.trim().toDoubleOrNull() == null
    val limitInvalid get() = limit.isNotBlank() && limit.trim().toDoubleOrNull() == null
    val invalid get() = amountInvalid || limitInvalid || (name.isBlank() && !isBlank)
}

private class CategoryDraft(val name: String, checked: Boolean = true) {
    var checked by mutableStateOf(checked)
}

private enum class Step(val title: String) {
    WALLETS("Wallets"), BANKS("Bank accounts"), CARDS("Credit cards"), EXPENSE("Expense categories"), INCOME(
        "Income categories"
    )
}

/** Five steps: wallets, bank accounts, credit cards, expense categories, income categories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizard(onFinish: (SetupChoices) -> Unit, onCancel: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = Step.entries[stepIndex]
    val wallets = remember { mutableStateListOf(AccountDraft(name = "Cash")) }
    val banks = remember { mutableStateListOf(AccountDraft()) }
    val cards = remember { mutableStateListOf(AccountDraft()) }
    val expense = remember {
        mutableStateListOf<CategoryDraft>().apply {
            addAll(DefaultData.expenseCategories.map {
                CategoryDraft(it)
            })
        }
    }
    val income = remember {
        mutableStateListOf<CategoryDraft>().apply {
            addAll(DefaultData.incomeCategories.map {
                CategoryDraft(it)
            })
        }
    }
    val shared = remember {
        mutableStateListOf<CategoryDraft>().apply {
            addAll(DefaultData.sharedCategories.map {
                CategoryDraft(it)
            })
        }
    }

    val stepValid = when (step) {
        Step.WALLETS -> wallets.any { it.name.isNotBlank() } && wallets.none { it.invalid }
        Step.BANKS -> banks.none { it.invalid }
        Step.CARDS -> cards.none { it.invalid }
        Step.EXPENSE -> expense.any { it.checked }
        Step.INCOME -> income.any { it.checked }
    }
    val isLast = stepIndex == Step.entries.lastIndex

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appTopBarColors(),
                title = { Text("Set up · ${step.title}") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / Step.entries.size },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (step) {
                    Step.WALLETS -> AccountsStep(
                        intro = "How much money do you have in hand? Add a wallet per currency or purpose if you like.",
                        drafts = wallets, kind = AccountKind.WALLET, addLabel = "+ Add wallet",
                    )

                    Step.BANKS -> AccountsStep(
                        intro = "Bank accounts, if any, with their current balance. Leave empty to add them later.",
                        drafts = banks, kind = AccountKind.BANK, addLabel = "+ Add bank account",
                    )

                    Step.CARDS -> AccountsStep(
                        intro = "Credit cards, if any. Enter the amount currently outstanding (it is kept as a negative balance) and the limit if you want it tracked.",
                        drafts = cards, kind = AccountKind.CARD, addLabel = "+ Add credit card",
                    )

                    Step.EXPENSE -> CategoriesStep(
                        intro = "The kinds of expenses you want to track. Categories can be nested and edited later.",
                        drafts = expense,
                    )

                    Step.INCOME -> {
                        CategoriesStep(
                            intro = "Where your money comes from. Used when recording income.",
                            drafts = income,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "For both income and expenses",
                            style = MaterialTheme.typography.titleSmall
                        )
                        CategoriesStep(
                            intro = "Money that moves between you and other people, in either direction.",
                            drafts = shared,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { if (stepIndex == 0) onCancel() else stepIndex-- }) {
                    Text(
                        if (stepIndex == 0) "Cancel" else "Back"
                    )
                }
                Button(
                    enabled = stepValid,
                    onClick = {
                        if (!isLast) stepIndex++
                        else onFinish(
                            SetupChoices(
                                wallets = wallets.filter { it.name.isNotBlank() }
                                    .map { NewAccount(it.name, it.amountValue) },
                                banks = banks.filter { it.name.isNotBlank() }
                                    .map { NewAccount(it.name, it.amountValue, it.number) },
                                creditCards = cards.filter { it.name.isNotBlank() }
                                    .map {
                                        NewAccount(
                                            it.name,
                                            -it.amountValue,
                                            it.number,
                                            it.limitValue
                                        )
                                    },
                                expenseCategories = expense.filter { it.checked }.map { it.name },
                                incomeCategories = income.filter { it.checked }.map { it.name },
                                sharedCategories = shared.filter { it.checked }.map { it.name },
                            )
                        )
                    },
                ) { Text(if (isLast) "Finish" else "Next") }
            }
        }
    }
}

private enum class AccountKind { WALLET, BANK, CARD }

@Composable
private fun AccountsStep(
    intro: String,
    drafts: SnapshotStateList<AccountDraft>,
    kind: AccountKind,
    addLabel: String
) {
    Text(intro, style = MaterialTheme.typography.bodyMedium)
    drafts.forEachIndexed { i, draft ->
        AccountRow(draft, kind, removable = drafts.size > 1, onRemove = { drafts.removeAt(i) })
    }
    TextButton(onClick = { drafts.add(AccountDraft()) }) { Text(addLabel) }
}

@Composable
private fun AccountRow(
    draft: AccountDraft,
    kind: AccountKind,
    removable: Boolean,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft.name = it },
                label = { Text("Name") },
                isError = draft.name.isBlank() && !draft.isBlank,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = draft.amount,
                onValueChange = { draft.amount = it },
                label = { Text(if (kind == AccountKind.CARD) "Outstanding" else "Balance") },
                isError = draft.amountInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            if (removable) {
                IconButton(onClick = onRemove) { Text("✕") }
            }
        }
        if (kind == AccountKind.BANK) {
            OutlinedTextField(
                value = draft.number,
                onValueChange = { draft.number = it },
                label = { Text("Account number (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (kind == AccountKind.CARD) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.number,
                    onValueChange = { draft.number = it },
                    label = { Text("Last digits (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = draft.limit,
                    onValueChange = { draft.limit = it },
                    label = { Text("Limit (optional)") },
                    isError = draft.limitInvalid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CategoriesStep(intro: String, drafts: SnapshotStateList<CategoryDraft>) {
    var newName by remember(drafts) { mutableStateOf("") }
    Text(intro, style = MaterialTheme.typography.bodyMedium)
    drafts.forEach { c ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = c.checked, onCheckedChange = { c.checked = it })
            Text(c.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Another category") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            enabled = newName.isNotBlank() && drafts.none {
                it.name.equals(
                    newName.trim(),
                    ignoreCase = true
                )
            },
            onClick = { drafts.add(CategoryDraft(newName.trim())); newName = "" },
        ) { Text("Add") }
    }
}
