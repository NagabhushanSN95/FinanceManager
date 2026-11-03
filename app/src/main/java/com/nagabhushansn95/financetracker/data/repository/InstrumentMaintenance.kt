package com.nagabhushansn95.financetracker.data.repository

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.Converters
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** Units held per instrument, from the ledger: INVEST and bonus INCOME add, REDEEM subtracts. */
data class Holding(val units: Double, val lastActivity: LocalDate, val hasUnits: Boolean)

class InstrumentMaintenance(private val db: AppDatabase) {

    /**
     * Stocks and mutual funds whose units are back to zero and that saw no activity for [idleDays] are marked
     * inactive, so they leave the pickers but keep their history. Instruments without any unit information are
     * left alone, since their position cannot be known. Returns what was retired.
     */
    suspend fun retireIdleHoldings(
        today: LocalDate = LocalDate.now(),
        idleDays: Long = 90,
        now: LocalDateTime = LocalDateTime.now()
    ): List<Instrument> {
        val holdings = holdings(db.transactionDao().getAllWithInstrument())
        val cutoff = today.minusDays(idleDays)
        val retired = db.instrumentDao().getAll().filter { instrument ->
            instrument.active &&
                    (instrument.instrumentType == InstrumentType.STOCK || instrument.instrumentType == InstrumentType.MUTUAL_FUND) &&
                    holdings[instrument.id]?.let { it.hasUnits && abs(it.units) < UNIT_EPSILON && it.lastActivity <= cutoff } == true
        }
        val stamp = Converters().localDateTimeToString(now)!!
        retired.forEach { db.instrumentDao().setActive(it.id, false, stamp) }
        return retired
    }

    companion object {
        const val UNIT_EPSILON = 0.0005

        fun holdings(transactions: List<TransactionEntity>): Map<Long, Holding> =
            transactions.groupBy { it.instrumentId!! }.mapValues { (_, rows) ->
                var units = 0.0
                var hasUnits = false
                for (t in rows) {
                    val q = t.quantity ?: continue
                    when (t.kind) {
                        TransactionKind.INVEST, TransactionKind.INCOME -> {
                            units += q; hasUnits = true
                        }

                        TransactionKind.REDEEM -> {
                            units -= q; hasUnits = true
                        }

                        else -> Unit
                    }
                }
                Holding(units, rows.maxOf { it.date }, hasUnits)
            }
    }
}
