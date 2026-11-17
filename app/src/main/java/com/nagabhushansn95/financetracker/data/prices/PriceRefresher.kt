package com.nagabhushansn95.financetracker.data.prices

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.Converters
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Listed
import com.nagabhushansn95.financetracker.data.db.entity.PriceSource
import com.nagabhushansn95.financetracker.data.investments.Holdings
import com.nagabhushansn95.financetracker.data.investments.Position
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDateTime

/** What a refresh did, for the hub and the log line under "Prices as of". */
data class RefreshResult(
    val stocks: Int = 0,
    val funds: Int = 0,
    val usdInr: Double? = null,
    val linked: List<String> = emptyList(),
    val failed: List<String> = emptyList(),
    /** Symbols whose latest quote is more than a month old on every ticker tried (renamed or delisted). */
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
 * Pulls today's prices onto the securities (last price, date and source YAHOO / AMFI) and the USD/INR rate into
 * `fx_rates`. Only active, held stocks and funds are fetched. A fund matched by name has its AMFI scheme code
 * (symbol, exchange AMFI) and ISIN written back so the next refresh matches directly.
 */
class PriceRefresher(
    private val db: AppDatabase,
    private val yahoo: YahooQuotes,
    private val amfi: AmfiNav,
    private val parallel: Int = 4,
) {
    suspend fun refresh(now: LocalDateTime = LocalDateTime.now()): RefreshResult = coroutineScope {
        val instruments = db.instrumentDao().getListed().filter { it.instrument.active }
        val ledger = db.transactionDao().getAllWithInstrument()
        val positions = Holdings.positions(ledger, events = db.holdingEventDao().getAll())
        fun held(i: Listed) = (positions[i.id]?.units ?: 0.0) > Position.UNIT_EPSILON
        val stocks = instruments.filter {
            it.instrument.instrumentType == InstrumentType.STOCK && held(it) && yahoo.tickers(it.security)
                .isNotEmpty()
        }
        val funds =
            instruments.filter {
                it.instrument.instrumentType == InstrumentType.MUTUAL_FUND && held(
                    it
                )
            }
        val failed = ArrayList<String>()
        val linked = ArrayList<String>()
        val stale = ArrayList<String>()
        val staleBefore = now.toLocalDate().minusDays(31)
        val gate = Semaphore(parallel)

        val stockJobs = stocks.map { instrument ->
            async {
                gate.withPermit {
                    runCatching { yahoo.quoteAny(yahoo.tickers(instrument.security), staleBefore) }
                        .onSuccess { q ->
                            store(instrument.id, q.price, q.date, PriceSource.YAHOO, now)
                            if (q.date.isBefore(staleBefore)) synchronized(stale) { stale += "${instrument.security.symbol ?: instrument.name} (${q.date})" }
                        }
                        .onFailure {
                            android.util.Log.w(
                                "PriceRefresher",
                                "Quote failed for ${instrument.security.symbol}: ${it.message}"
                            )
                            synchronized(failed) {
                                failed += instrument.security.symbol ?: instrument.name
                            }
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
                // The link first (it rewrites the row), then the price.
                val s = instrument.security
                val isin = s.isin ?: scheme.isinGrowth ?: scheme.isinReinvestment
                if (s.symbol != scheme.code || s.exchange != AmfiNav.EXCHANGE || s.isin != isin) {
                    db.instrumentDao().upsertSecurity(
                        s.copy(
                            symbol = scheme.code,
                            exchange = AmfiNav.EXCHANGE,
                            isin = isin
                        )
                    )
                    linked += instrument.name
                }
                store(instrument.id, scheme.nav, scheme.date, PriceSource.AMFI, now)
                fundCount++
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

    /** The security's last price, date and source. */
    private suspend fun store(
        instrumentId: Long,
        price: Double,
        date: java.time.LocalDate,
        source: PriceSource,
        now: LocalDateTime
    ) {
        val c = Converters()
        db.instrumentDao().setPrice(
            instrumentId,
            price,
            c.localDateToString(date)!!,
            source,
            c.localDateTimeToString(now)!!
        )
    }
}
