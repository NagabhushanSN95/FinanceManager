package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The entries a provident-fund account takes, each a plain ledger row on the account: credits are INCOME rows into
 * the account tagged with a contributor, a PPF deposit and a withdrawal are TRANSFER rows against a bank account.
 * They are recorded and edited only from the account's screen under Investments; the editor never offers a
 * provident-fund account.
 */
enum class PfEntryKind(
    val title: String,
    val contributor: String?,
    /** Money moves against a bank account (a TRANSFER row) rather than arriving from outside the ledger. */
    val transfer: Boolean = false,
    /** Money leaves the account. */
    val outflow: Boolean = false,
    /** Offered on EPF accounts, PPF accounts, or both. */
    val epf: Boolean = true,
    val ppf: Boolean = true,
) {
    EMPLOYEE_CONTRIBUTION("Employee contribution", PfContributor.EMPLOYEE, ppf = false),
    EMPLOYER_CONTRIBUTION("Employer contribution", PfContributor.EMPLOYER, ppf = false),
    VOLUNTARY_CONTRIBUTION("Voluntary contribution (VPF)", PfContributor.EMPLOYEE, ppf = false),
    SELF_CONTRIBUTION("Self contribution", PfContributor.SELF, transfer = true, epf = false),
    INTEREST_CREDIT("Interest credit", PfContributor.INTEREST),
    TRANSFER_IN("Transfer in", PfContributor.TRANSFER, ppf = false),
    WITHDRAWAL("Withdrawal", null, transfer = true, outflow = true);

    /** The bank-account field's label when the entry needs one. */
    val bankLabel: String?
        get() = when {
            !transfer -> null
            outflow -> "Paid to"
            else -> "Paid from"
        }
}

/** Contributor tags on provident-fund rows (the `contributor` column). */
object PfContributor {
    const val EMPLOYEE = "Employee"
    const val EMPLOYER = "Employer"
    const val SELF = "Self"
    const val INTEREST = "Interest"

    /** Balance moved in from a previous employer's account on a job change: a contribution made earlier. */
    const val TRANSFER = "Transfer"
}

object ProvidentFunds {
    private const val VPF_MARK = "VPF"

    /** Income category of contributions and transfers in; interest credits go under [INTEREST_CATEGORY]. */
    const val CATEGORY = "Provident Fund"
    const val INTEREST_CATEGORY = "Interest & Dividends"

    /** Which contributor tags count as money put in (everything but interest). */
    val CONTRIBUTION_TAGS = setOf(
        PfContributor.EMPLOYEE,
        PfContributor.EMPLOYER,
        PfContributor.SELF,
        PfContributor.TRANSFER
    )

    /** The income category for an entry: [CATEGORY] for contributions, [INTEREST_CATEGORY] for interest, none for a withdrawal. */
    fun categoryNameFor(kind: PfEntryKind): String? = when {
        kind.contributor == PfContributor.INTEREST -> INTEREST_CATEGORY
        kind.contributor != null -> CATEGORY
        else -> null
    }

    private fun List<Category>.incomeCategory(name: String): Category? =
        firstOrNull {
            !it.deleted && it.appliesTo != CategoryScope.EXPENSE && it.name.equals(
                name,
                ignoreCase = true
            )
        }

    /**
     * Makes sure the [CATEGORY] income category exists and files the provident-fund credits under the categories
     * the entry sheet uses: contributions that carry no category or "Others" go under [CATEGORY], interest credits
     * without a category under [INTEREST_CATEGORY]. A category chosen by hand is left alone. Safe to run on every
     * open; returns how many rows were filed.
     */
    suspend fun fileCategories(db: AppDatabase, now: LocalDateTime = LocalDateTime.now()): Int {
        val categories = db.categoryDao().getAll()
        val pfId = categories.incomeCategory(CATEGORY)?.id ?: db.categoryDao().insert(
            Category(
                name = CATEGORY,
                appliesTo = CategoryScope.INCOME,
                displayOrder = db.categoryDao().maxDisplayOrder() + 1,
            )
        )
        val interestId = categories.incomeCategory(INTEREST_CATEGORY)?.id
        val others =
            categories.filter { it.name.equals("Others", ignoreCase = true) }.map { it.id }.toSet()
        var filed = 0
        val pfAccounts =
            db.accountDao().getAll().filter { it.accountType == AccountType.PROVIDENT_FUND }
        for (account in pfAccounts) {
            for (t in db.transactionDao().getForAccount(account.id)) {
                if (t.kind != TransactionKind.INCOME || t.toAccountId != account.id) continue
                val target = when {
                    t.contributor in CONTRIBUTION_TAGS && (t.categoryId == null || t.categoryId in others) -> pfId
                    t.contributor == PfContributor.INTEREST && t.categoryId == null -> interestId
                    else -> null
                } ?: continue
                if (target == t.categoryId) continue
                db.transactionDao().update(t.copy(categoryId = target, modifiedTime = now))
                filed++
            }
        }
        return filed
    }

    /** Public Provident Fund accounts are told from EPF by their name; everything else is treated as EPF. */
    fun isPpf(account: Account): Boolean =
        account.name.contains("PPF", ignoreCase = true) || account.name.contains(
            "Public Provident",
            ignoreCase = true
        )

    /** The entries this account's "+" offers, in menu order. */
    fun kindsFor(account: Account): List<PfEntryKind> {
        val ppf = isPpf(account)
        return PfEntryKind.entries.filter { if (ppf) it.ppf else it.epf }
    }

    /** Which entry an existing row on [accountId] is, for editing; null when the row is none of them. */
    fun entryKindOf(t: TransactionEntity, accountId: Long): PfEntryKind? = when {
        t.fromAccountId == accountId && t.kind == TransactionKind.TRANSFER -> PfEntryKind.WITHDRAWAL
        t.toAccountId == accountId && t.kind == TransactionKind.TRANSFER -> PfEntryKind.SELF_CONTRIBUTION
        t.toAccountId == accountId && t.kind == TransactionKind.INCOME -> when (t.contributor) {
            PfContributor.EMPLOYEE ->
                if (t.particulars.contains(
                        VPF_MARK,
                        ignoreCase = true
                    )
                ) PfEntryKind.VOLUNTARY_CONTRIBUTION
                else PfEntryKind.EMPLOYEE_CONTRIBUTION

            PfContributor.EMPLOYER -> PfEntryKind.EMPLOYER_CONTRIBUTION
            PfContributor.INTEREST -> PfEntryKind.INTEREST_CREDIT
            PfContributor.TRANSFER -> PfEntryKind.TRANSFER_IN
            PfContributor.SELF -> PfEntryKind.SELF_CONTRIBUTION
            else -> null
        }

        else -> null
    }

    /**
     * What the particulars say unless the user types something, in the ledger's house style (the 4.x wording, capital
     * first letters, no date: the row's date says when): "Invested In Employees Provident Fund - Employer Contribution".
     */
    fun defaultParticulars(kind: PfEntryKind, account: Account): String {
        val fund = if (isPpf(account)) "Public Provident Fund" else "Employees Provident Fund"
        return when (kind) {
            PfEntryKind.EMPLOYEE_CONTRIBUTION -> "Invested In $fund - Employee Contribution"
            PfEntryKind.EMPLOYER_CONTRIBUTION -> "Invested In $fund - Employer Contribution"
            PfEntryKind.VOLUNTARY_CONTRIBUTION -> "Invested In $fund - $VPF_MARK Contribution"
            PfEntryKind.SELF_CONTRIBUTION -> "Invested In $fund - Self"
            PfEntryKind.INTEREST_CREDIT -> "$fund Interest Credited"
            PfEntryKind.TRANSFER_IN -> "$fund Balance Transferred In"
            PfEntryKind.WITHDRAWAL -> "Withdrawn From $fund"
        }
    }

    /**
     * The particulars a new entry starts with: the wording of the last entry of the same kind on the account, so a
     * series keeps reading the same way whatever it was called, else [defaultParticulars].
     */
    fun suggestedParticulars(
        kind: PfEntryKind,
        account: Account,
        rows: List<TransactionEntity>
    ): String =
        rows.filter { entryKindOf(it, account.id) == kind }
            .maxWithOrNull(compareBy({ it.date }, { it.id }))
            ?.particulars?.trim()?.ifBlank { null } ?: defaultParticulars(kind, account)

    /** Indian financial year label for a date: April 2025 to March 2026 is "2025-26". */
    fun financialYearLabel(date: LocalDate): String {
        val start = if (date.monthValue >= 4) date.year else date.year - 1
        return "$start-${(start + 1) % 100}"
    }

    /**
     * The ledger row for an entry. [bankAccountId] is required for a transfer; [categoryId] is the income category
     * (see [categoryNameFor]), ignored on a transfer. An [existing] row keeps its id, timestamps and flags; the rest
     * is replaced.
     */
    fun toTransaction(
        kind: PfEntryKind,
        account: Account,
        date: LocalDate,
        amount: Double,
        particulars: String,
        bankAccountId: Long?,
        categoryId: Long? = null,
        existing: TransactionEntity? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): TransactionEntity {
        require(account.accountType == AccountType.PROVIDENT_FUND) { "${account.name} is not a provident-fund account" }
        require(amount > 0) { "Amount must be positive" }
        if (kind.transfer) require(bankAccountId != null) { "Choose the bank account" }
        val base = existing ?: TransactionEntity(
            createdTime = now,
            modifiedTime = now,
            date = date,
            kind = TransactionKind.INCOME,
            particulars = particulars,
            amount = amount,
        )
        return base.copy(
            date = date,
            kind = if (kind.transfer) TransactionKind.TRANSFER else TransactionKind.INCOME,
            fromAccountId = when {
                !kind.transfer -> null
                kind.outflow -> account.id
                else -> bankAccountId
            },
            toAccountId = if (kind.outflow) bankAccountId else account.id,
            categoryId = if (kind.transfer) null else categoryId,
            instrumentId = null,
            particulars = particulars.trim().ifBlank { defaultParticulars(kind, account) },
            quantity = null,
            rate = null,
            amount = amount,
            currency = account.currency,
            fxRate = null,
            contributor = kind.contributor,
        )
    }
}
