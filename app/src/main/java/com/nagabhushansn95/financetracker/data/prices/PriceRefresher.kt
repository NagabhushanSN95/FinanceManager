package com.nagabhushansn95.financetracker.data.prices

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.ValuationSource
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.data.investments.Position
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDateTime
import kotlin.math.abs

/** What a refresh did, for the hub and the log line under "Prices as of". */
data class RefreshResult(
    val stocks: Int = 0,
    val funds: Int = 0,
    val usdInr: Double? = null,
    val linked: List<String> = emptyList(),
    val failed: List<String> = emptyList(),
    /** Symbols whose latest quote is more than a month old (Yahoo keeps renamed or delisted tickers). */
    val stale: List<String> = emptyList(),
    val skipped: Boolean = false,
) {
    fun summary(): String {
        if (skipped) return "Prices are less than an hour old"
        val parts = ArrayList<String>()
        parts += "$stocks stock${if (stocks == 1) "" else "s"}"
        parts += "$funds fund${if (funds == 1) "" else "s"}"
        usdInr?.let { parts += "USD/INR %.2f".format(it) }
        val head = "Updated " + parts.joinToString(", ")
        return listOfNotNull(
            head,
            failed.takeIf { it.isNotEmpty() }?.let { "not found: ${it.joinToString(", ")}" },
            stale.takeIf { it.isNotEmpty() }?.let { "stale on Yahoo: ${it.joinToString(", ")}" },
        ).joinToString(" · ")
    }
}

/**
 * Pulls today's prices into `instrument_valuations` (source YAHOO / AMFI) and the USD/INR rate into `fx_rates`.
 * Only active, held stocks and funds are fetched. A fund matched by name has its AMFI scheme code (symbol,
 * exchange AMFI) and ISIN written back so the next refresh matches directly.
 */
class PriceRefresher(
    private val db: AppDatabase,
    private val yahoo: YahooQuotes,
    private val amfi: AmfiNav,
    private val parallel: Int = 4,
) {
    suspend fun refresh(now: LocalDateTime = LocalDateTime.now()): RefreshResult = coroutineScope {
        val instruments = db.instrumentDao().getAll().filter { it.active }
        val ledger = db.transactionDao().getAllWithInstrument()
        val positions = Holdings.positions(ledger)
        fun held(i: Instrument) = (positions[i.id]?.units ?: 0.0) > Position.UNIT_EPSILON
        val stocks = instruments.filter {
            it.instrumentType == InstrumentType.STOCK && held(it) && yahoo.ticker(it) != null
        }
        val funds =
            instruments.filter { it.instrumentType == InstrumentType.MUTUAL_FUND && held(it) }
        val failed = ArrayList<String>()
        val linked = ArrayList<String>()
        val stale = ArrayList<String>()
        val staleBefore = now.toLocalDate().minusDays(31)
        val gate = Semaphore(parallel)

        val stockJobs = stocks.map { instrument ->
            async {
                gate.withPermit {
                    runCatching { yahoo.quote(yahoo.ticker(instrument)!!) }
                        .onSuccess { q ->
                            store(instrument.id, q.price, q.date, ValuationSource.YAHOO, now)
                            if (q.date.isBefore(staleBefore)) synchronized(stale) { stale += "${instrument.symbol ?: instrument.name} (${q.date})" }
                        }
                        .onFailure {
                            android.util.Log.w(
                                "PriceRefresher",
                                "Quote failed for ${instrument.symbol}: ${it.message}"
                            )
                            synchronized(failed) { failed += instrument.symbol ?: instrument.name }
                        }
                        .isSuccess
                }
            }
        }
        val fxJob = async {
            gate.withPermit {
                runCatching { yahoo.usdInr() }
                    .onSuccess { q ->
                        db.instrumentDao().upsertFxRate(
                            FxRate(
                                date = q.date,
                                base = "USD",
                                quote = "INR",
                                rate = q.price,
                                source = "YAHOO"
                            )
                        )
                    }
                    .onFailure { synchronized(failed) { failed += "USD/INR" } }
                    .getOrNull()?.price
            }
        }
        var fundCount = 0
        if (funds.isNotEmpty()) {
            val schemes = runCatching { amfi.fetchAll() }.getOrElse {
                android.util.Log.w("PriceRefresher", "AMFI NAV download failed", it)
                synchronized(failed) { failed += "AMFI" }; emptyList()
            }
            if (schemes.isNotEmpty()) for (instrument in funds) {
                val scheme = AmfiNav.match(instrument, schemes)
                if (scheme == null) {
                    failed += instrument.name
                    continue
                }
                store(instrument.id, scheme.nav, scheme.date, ValuationSource.AMFI, now)
                fundCount++
                val isin = instrument.isin ?: scheme.isinGrowth ?: scheme.isinReinvestment
                if (instrument.symbol != scheme.code || instrument.exchange != AmfiNav.EXCHANGE || instrument.isin != isin) {
                    db.instrumentDao().update(
                        instrument.copy(
                            symbol = scheme.code,
                            exchange = AmfiNav.EXCHANGE,
                            isin = isin,
                            modifiedTime = now
                        )
                    )
                    linked += instrument.name
                }
            }
        }
        RefreshResult(
            stocks = stockJobs.count { it.await() },
            funds = fundCount,
            usdInr = fxJob.await(),
            linked = linked,
            failed = failed.sorted(),
            stale = stale.sorted(),
        )
    }

    /** One valuation row per instrument, source and date; a changed price on the same day replaces nothing older. */
    private suspend fun store(
        instrumentId: Long,
        price: Double,
        date: java.time.LocalDate,
        source: ValuationSource,
        now: LocalDateTime
    ) {
        val latest = db.instrumentDao().latestValuation(instrumentId)
        if (latest != null && latest.source == source && latest.date == date && latest.unitPrice != null && abs(
                latest.unitPrice - price
            ) < 1e-9
        ) return
        db.instrumentDao().insertValuation(
            InstrumentValuation(
                instrumentId = instrumentId,
                date = date,
                unitPrice = price,
                source = source,
                fetchedAt = now
            )
        )
    }
}
