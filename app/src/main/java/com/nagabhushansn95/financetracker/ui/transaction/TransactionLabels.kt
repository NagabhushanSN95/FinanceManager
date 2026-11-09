package com.nagabhushansn95.financetracker.ui.transaction

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod

/** Lookup tables for rendering ledger rows without joins; small enough to keep in memory. */
class LedgerLookups(
    accounts: List<Account>,
    categories: List<Category>,
    instruments: List<Instrument>,
    /** Validity rows, so a service or emission test can say where it was done without that being in its particulars. */
    periods: List<ValidityPeriod> = emptyList(),
) {
    val accounts = accounts.associateBy { it.id }
    val categories = categories.associateBy { it.id }
    val instruments = instruments.associateBy { it.id }
    private val places = periods
        .filter { (it.kind == ValidityKind.SERVICE || it.kind == ValidityKind.EMISSION_TEST) && it.transactionId != null && it.provider != null }
        .associate { it.transactionId!! to it.provider!! }

    /** The service or testing centre of a vehicle row, when recorded. */
    fun placeOf(transactionId: Long): String? = places[transactionId]

    fun accountName(id: Long?) = id?.let { accounts[it]?.name } ?: "—"

    /** "Parent › Child" for second-level categories. */
    fun categoryPath(id: Long?): String? {
        val c = id?.let { categories[it] } ?: return null
        val parent = c.parentId?.let { categories[it] }
        return if (parent != null) "${parent.name} › ${c.name}" else c.name
    }

    /** Short name, else the ticker (not an AMFI scheme code, which says nothing), else the name. */
    fun instrumentName(id: Long?) =
        id?.let {
            instruments[it]?.let { i ->
                i.shortName ?: i.symbol?.takeIf { s -> !s.all(Char::isDigit) } ?: i.name
            }
        }

    /** Secondary line for a ledger row, derived from kind and endpoints (plan 3.3). */
    fun describe(t: TransactionEntity): String = when (t.kind) {
        TransactionKind.INCOME -> listOfNotNull(
            "Income",
            categoryPath(t.categoryId),
            instrumentName(t.instrumentId),
            t.contributor,
        ).joinToString(" · ") + " → " + accountName(t.toAccountId)

        TransactionKind.EXPENSE -> listOfNotNull(
            "Expense",
            categoryPath(t.categoryId),
            instrumentName(t.instrumentId),
            placeOf(t.id),
        )
            .joinToString(" · ") + " ← " + accountName(t.fromAccountId)

        TransactionKind.TRANSFER -> "Transfer ${accountName(t.fromAccountId)} → ${accountName(t.toAccountId)}"

        TransactionKind.INVEST -> "Invested in ${instrumentName(t.instrumentId) ?: "—"}" +
                quantityRate(t) + (t.fromAccountId?.let { " ← ${accountName(it)}" } ?: "")

        TransactionKind.REDEEM -> "Redeemed ${instrumentName(t.instrumentId) ?: "—"}" +
                quantityRate(t) + (t.toAccountId?.let { " → ${accountName(it)}" } ?: "")
    }

    private fun quantityRate(t: TransactionEntity): String {
        val q = t.quantity ?: return ""
        val qText = if (q == q.toLong().toDouble()) q.toLong().toString() else "%.3f".format(q)
        return t.rate?.let { " $qText @ %.2f".format(it) } ?: " ×$qText"
    }
}

val TransactionKind.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }
