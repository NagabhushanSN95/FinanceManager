package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.Converters
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEvent
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Listed
import com.nagabhushansn95.financetracker.data.db.entity.PendingDividend
import com.nagabhushansn95.financetracker.data.db.entity.PriceSource
import com.nagabhushansn95.financetracker.data.db.entity.Security
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.investments.HoldingEntries
import com.nagabhushansn95.financetracker.data.investments.HoldingEntryKind
import com.nagabhushansn95.financetracker.data.investments.HoldingView
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import com.nagabhushansn95.financetracker.data.prices.AmfiNav
import com.nagabhushansn95.financetracker.data.prices.AmfiScheme
import java.time.LocalDate
import java.time.LocalDateTime

/** Stocks, ETFs and mutual funds: the instruments and the entries recorded on their screens. */
class HoldingsRepository(private val db: AppDatabase) {
    private val transactions = TransactionRepository(db)
    private val edit = EditDataRepository(db)

    /** A new stock or fund. A stock without an exchange is taken as NSE, a fund's code as its AMFI scheme. */
    suspend fun add(
        name: String,
        type: InstrumentType,
        symbol: String?,
        exchange: String?,
        isin: String?,
        currency: String,
        categoryId: Long?,
        owner: String,
        notes: String?,
        now: LocalDateTime = LocalDateTime.now(),
        shortName: String? = null,
        series: String? = null,
    ): Long {
        require(type == InstrumentType.STOCK || type == InstrumentType.MUTUAL_FUND) { "Not a stock or fund" }
        require(name.isNotBlank()) { "A name is needed" }
        val code = symbol?.trim()?.ifBlank { null }
        val defaultCategory =
            db.instrumentDao().getCategoryByName(InvestmentCategories.defaultCategoryName(type))?.id
        return db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val id = edit.addInstrument(
                    Instrument(
                        name = name.trim(),
                        instrumentType = type,
                        categoryId = categoryId ?: defaultCategory,
                        owner = owner.trim().ifBlank { Instrument.OWNER_SELF },
                        notes = notes?.trim()?.ifBlank { null },
                        createdTime = now,
                        modifiedTime = now,
                    ), now
                )
                db.instrumentDao().upsertSecurity(
                    Security(
                        instrumentId = id,
                        shortName = shortName?.trim()?.ifBlank { null },
                        symbol = code,
                        exchange = exchange?.trim()?.uppercase()?.ifBlank { null }
                            ?: if (type == InstrumentType.STOCK) "NSE" else if (code != null) AmfiNav.EXCHANGE else null,
                        isin = isin?.trim()?.uppercase()?.ifBlank { null },
                        series = series?.trim()?.uppercase()?.ifBlank { null },
                        currency = currency.trim().uppercase().ifBlank { "INR" },
                    )
                )
                id
            }
        }
    }

    /** Saves the instrument and its listing together (the kind never changes). */
    suspend fun update(listed: Listed, now: LocalDateTime = LocalDateTime.now()) {
        require(listed.name.isNotBlank()) { "A name is needed" }
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                edit.updateInstrument(listed.instrument, now)
                db.instrumentDao().upsertSecurity(listed.security.copy(instrumentId = listed.id))
            }
        }
    }

    /** Records bonus shares or a split (units that arrived without cash), or rewrites [existing]. */
    suspend fun saveEvent(
        view: HoldingView,
        kind: HoldingEntryKind,
        date: LocalDate,
        units: Double?,
        ratio: String?,
        existing: HoldingEvent? = null,
    ): Long {
        HoldingEntries.problem(view, kind, units, 0.0, null, null)
            ?.let { throw IllegalArgumentException(it) }
        val event = HoldingEntries.toEvent(view, kind, date, units!!, ratio, existing)
        return if (existing == null) db.holdingEventDao().insert(event) else {
            db.holdingEventDao().update(event); event.id
        }
    }

    suspend fun deleteEvent(id: Long) = db.holdingEventDao().delete(id)

    /** Records a cash entry, or rewrites [existing] as the sheet now describes it; account balances follow. */
    suspend fun saveEntry(
        view: HoldingView,
        kind: HoldingEntryKind,
        date: LocalDate,
        units: Double?,
        rate: Double?,
        amount: Double,
        accountId: Long?,
        particulars: String,
        exDate: LocalDate? = null,
        existing: TransactionEntity? = null,
        now: LocalDateTime = LocalDateTime.now(),
        brokerRef: String? = null,
    ): Long {
        HoldingEntries.problem(view, kind, units, amount, accountId, existing)
            ?.let { throw IllegalArgumentException(it) }
        val categoryId = when (kind) {
            HoldingEntryKind.DIVIDEND -> categoryId(
                HoldingEntries.DIVIDEND_CATEGORY,
                CategoryScope.INCOME
            )

            HoldingEntryKind.CHARGES -> categoryId(
                HoldingEntries.CHARGES_CATEGORY,
                CategoryScope.EXPENSE
            )

            else -> null
        }
        val row = HoldingEntries.toTransaction(
            view,
            kind,
            date,
            units,
            rate,
            amount,
            accountId,
            particulars,
            exDate,
            categoryId,
            existing,
            now,
            brokerRef,
        )
        return if (existing == null) transactions.add(row, now = now)
        else {
            transactions.update(row, now = now); row.id
        }
    }

    suspend fun deleteEntry(id: Long, now: LocalDateTime = LocalDateTime.now()) =
        transactions.delete(id, now)

    /**
     * The bank credited a dividend the statement announced: records it as the stock's dividend entry with the
     * statement's reference (so a re-import knows it) and takes it off the pending list.
     */
    suspend fun recordPendingDividend(
        pending: PendingDividend,
        view: HoldingView,
        date: LocalDate,
        /** The gross figures as the sheet left them (the statement's unless corrected); null keeps the statement's. */
        units: Double?,
        rate: Double?,
        amount: Double,
        accountId: Long?,
        particulars: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): Long {
        require(view.instrument.id == pending.instrumentId) { "Not this holding's dividend" }
        val id = saveEntry(
            view,
            HoldingEntryKind.DIVIDEND,
            date,
            units ?: pending.quantity,
            rate ?: pending.perShare,
            amount,
            accountId,
            particulars,
            pending.exDate,
            null,
            now,
            pending.brokerRef,
        )
        db.pendingDividendDao().delete(pending.id)
        return id
    }

    /** Drops a pending dividend without recording it (recorded by hand already, or never due). */
    suspend fun dismissPendingDividend(id: Long) = db.pendingDividendDao().delete(id)

    /** Removes the holding with every entry on it (balances put back) and its valuations, in one write. */
    suspend fun delete(instrumentId: Long, now: LocalDateTime = LocalDateTime.now()) =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                requireNotNull(db.instrumentDao().getById(instrumentId)) { "Not found" }
                transactions.deleteAllInTransaction(
                    db.transactionDao().getForInstrument(instrumentId), now
                )
                db.instrumentDao().delete(instrumentId)
            }
        }

    suspend fun mergeInto(fromId: Long, intoId: Long, now: LocalDateTime = LocalDateTime.now()) =
        edit.mergeInstruments(fromId, intoId, now)

    /** A price typed by hand, as the security's last price. */
    suspend fun setManualPrice(
        instrumentId: Long,
        unitPrice: Double,
        date: LocalDate,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        require(unitPrice >= 0) { "The price cannot be negative" }
        val c = Converters()
        db.instrumentDao().setPrice(
            instrumentId,
            unitPrice,
            c.localDateToString(date)!!,
            PriceSource.MANUAL,
            c.localDateTimeToString(now)!!
        )
    }

    /** Stores the scheme code and ISIN on the fund and the NAV from the list. */
    suspend fun linkScheme(
        instrumentId: Long,
        scheme: AmfiScheme,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val security = db.instrumentDao().getSecurity(instrumentId) ?: return
        db.instrumentDao().upsertSecurity(
            security.copy(
                symbol = scheme.code,
                exchange = AmfiNav.EXCHANGE,
                isin = scheme.isinGrowth ?: scheme.isinReinvestment ?: security.isin,
            )
        )
        val c = Converters()
        db.instrumentDao().setPrice(
            instrumentId,
            scheme.nav,
            c.localDateToString(scheme.date)!!,
            PriceSource.AMFI,
            c.localDateTimeToString(now)!!
        )
    }

    /** The category by name for the scope, created when missing (the Zerodha import does the same). */
    private suspend fun categoryId(name: String, scope: CategoryScope): Long {
        val all = db.categoryDao().getAll()
        val fits = { c: Category ->
            c.name.equals(
                name,
                ignoreCase = true
            ) && (c.appliesTo == scope || c.appliesTo == CategoryScope.BOTH)
        }
        all.firstOrNull { fits(it) && !it.deleted }?.let { return it.id }
        all.firstOrNull(fits)?.let { return it.id }
        return db.categoryDao().insert(
            Category(
                name = name,
                appliesTo = scope,
                displayOrder = db.categoryDao().maxDisplayOrder() + 1
            )
        )
    }
}
