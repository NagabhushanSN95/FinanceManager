package com.nagabhushansn95.financetracker.data.repository

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.Converters
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.data.investments.Position
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

class InstrumentMaintenance(private val db: AppDatabase) {

    /**
     * Stocks and mutual funds whose units are back to zero and that saw no activity for [idleDays] are marked
     * inactive, so they leave the pickers but keep their history. Units come from the same engine as the Holdings
     * screen ([Holdings]); instruments without any unit information are left alone. Returns what was retired.
     */
    suspend fun retireIdleHoldings(
        today: LocalDate = LocalDate.now(),
        idleDays: Long = 90,
        now: LocalDateTime = LocalDateTime.now()
    ): List<Instrument> {
        val candidates = db.instrumentDao().getAll().filter {
            it.active && (it.instrumentType == InstrumentType.STOCK || it.instrumentType == InstrumentType.MUTUAL_FUND)
        }
        val ids = candidates.map { it.id }.toSet()
        val positions = Holdings.positions(
            db.transactionDao().getAllWithInstrument().filter { it.instrumentId in ids },
            events = db.holdingEventDao().getAll(),
        )
        val cutoff = today.minusDays(idleDays)
        val retired = candidates.filter { instrument ->
            val p = positions[instrument.id] ?: return@filter false
            p.hasUnits && abs(p.units) < Position.UNIT_EPSILON && (p.lastDate ?: today) <= cutoff
        }
        val stamp = Converters().localDateTimeToString(now)!!
        retired.forEach { db.instrumentDao().setActive(it.id, false, stamp) }
        return retired
    }
}
