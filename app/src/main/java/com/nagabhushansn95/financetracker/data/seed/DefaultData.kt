package com.nagabhushansn95.financetracker.data.seed

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import java.time.LocalDateTime

/**
 * An account the user describes in the set-up wizard. For a credit card, [balance] is the signed balance
 * (negative = outstanding) and [creditLimit] the card's limit, if known.
 */
data class NewAccount(
    val name: String,
    val balance: Double = 0.0,
    val accountNumber: String? = null,
    val creditLimit: Double? = null,
)

/** What the first-run set-up creates. [defaults] is what "Skip set-up" uses. */
data class SetupChoices(
    val wallets: List<NewAccount>,
    val banks: List<NewAccount> = emptyList(),
    val creditCards: List<NewAccount> = emptyList(),
    val expenseCategories: List<String> = DefaultData.expenseCategories,
    val incomeCategories: List<String> = DefaultData.incomeCategories,
    val sharedCategories: List<String> = DefaultData.sharedCategories,
) {
    companion object {
        fun defaults() = SetupChoices(wallets = listOf(NewAccount("Cash")))
    }
}

/** Starting data for a user who does not import or restore: wallets, banks and categories from the set-up wizard. */
object DefaultData {
    val expenseCategories = listOf(
        "Food",
        "Groceries",
        "Transport",
        "Shopping",
        "Bills & Utilities",
        "Health",
        "Insurance",
        "Vehicle",
        "Entertainment",
        "Travel",
        "Education",
        "Gifts",
        "Others",
    )
    val incomeCategories =
        listOf("Salary", "Interest & Dividends", "Provident Fund", "Refund", "Others")

    /** Apply to income and expenses alike: money moving between the user and other people. */
    val sharedCategories = listOf("Friends & Family", "Loans & Repayments")

    suspend fun seed(
        db: AppDatabase,
        choices: SetupChoices = SetupChoices.defaults(),
        now: LocalDateTime = LocalDateTime.now()
    ) =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction { seedInTransaction(db, choices, now) }
        }

    private suspend fun seedInTransaction(
        db: AppDatabase,
        choices: SetupChoices,
        now: LocalDateTime
    ) {
        require(db.accountDao().getAll().isEmpty() && db.categoryDao().getAll().isEmpty()) {
            "Default data can only be seeded into an empty database"
        }
        require(choices.wallets.isNotEmpty()) { "At least one wallet is needed" }
        var order = 1
        val accounts = choices.wallets.map { it.toAccount(AccountType.WALLET, order++, now) } +
                choices.banks.map { it.toAccount(AccountType.BANK, order++, now) } +
                choices.creditCards.map { it.toAccount(AccountType.CREDIT_CARD, order++, now) }
        db.accountDao().insertAll(accounts)

        order = 1
        val expense = choices.expenseCategories.cleaned().ifEmpty { expenseCategories }
        val income = choices.incomeCategories.cleaned().ifEmpty { incomeCategories }
        val shared = choices.sharedCategories.cleaned()
        db.categoryDao().insertAll(
            expense.map {
                Category(
                    name = it,
                    appliesTo = CategoryScope.EXPENSE,
                    displayOrder = order++
                )
            } +
                    income.map {
                        Category(
                            name = it,
                            appliesTo = CategoryScope.INCOME,
                            displayOrder = order++
                        )
                    } +
                    shared.map {
                        Category(
                            name = it,
                            appliesTo = CategoryScope.BOTH,
                            displayOrder = order++
                        )
                    }
        )
    }

    private fun List<String>.cleaned() =
        map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    private fun NewAccount.toAccount(type: AccountType, order: Int, now: LocalDateTime) = Account(
        name = name.trim(),
        accountType = type,
        balance = balance,
        accountNumber = accountNumber?.trim()?.takeIf { it.isNotEmpty() },
        creditLimit = creditLimit?.takeIf { type == AccountType.CREDIT_CARD && it > 0 },
        displayOrder = order,
        createdTime = now,
        modifiedTime = now,
    )
}
