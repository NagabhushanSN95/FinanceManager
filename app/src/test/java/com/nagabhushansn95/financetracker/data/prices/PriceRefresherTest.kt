package com.nagabhushansn95.financetracker.data.prices

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.TestDb
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.PriceSource
import com.nagabhushansn95.financetracker.data.db.entity.Security
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.insertListed
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class PriceRefresherTest {
    private lateinit var db: AppDatabase
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private val requests = ArrayList<String>()

    private val http = object : HttpClient {
        override suspend fun get(url: String): String {
            requests += url
            return when {
                url.contains("GOLDBEES.NS") -> chart("GOLDBEES.NS", 112.69, "INR")
                url.contains("AAPL") -> chart("AAPL", 331.9, "USD")
                url.contains("USDINR") -> chart("INR=X", 94.73, "INR")
                url.contains("NAVAll") -> "Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Plan;Option;Net Asset Value;Date\n" +
                        "122639;INF879O01027;-;Parag Parikh Flexi Cap Fund;Direct Plan;Growth;90.7043;03-Nov-2026\n" +
                        "122640;INF879O01019;-;Parag Parikh Flexi Cap Fund;Regular Plan;Growth;82.5606;03-Nov-2026\n"

                else -> """{"chart":{"result":null,"error":{"code":"Not Found","description":"No data found"}}}"""
            }
        }
    }

    private fun chart(symbol: String, price: Double, currency: String) =
        """{"chart":{"result":[{"meta":{"currency":"$currency","symbol":"$symbol","exchangeTimezoneName":"Asia/Kolkata","regularMarketTime":1793700000,"regularMarketPrice":$price}}],"error":null}}"""

    @Before
    fun setUp() {
        db = TestDb.inMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun instrument(
        name: String,
        type: InstrumentType,
        symbol: String? = null,
        exchange: String? = null,
        currency: String = "INR",
        units: Double = 10.0,
        cost: Double = 1000.0
    ): Long {
        val id = db.insertListed(
            Instrument(
                name = name,
                instrumentType = type,
                createdTime = now,
                modifiedTime = now
            ), Security(symbol = symbol, exchange = exchange, currency = currency)
        )
        if (units > 0) db.transactionDao().insert(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.of(2026, 1, 5),
                kind = TransactionKind.INVEST,
                instrumentId = id,
                particulars = "buy",
                quantity = units,
                rate = cost / units,
                amount = cost,
                fxRate = if (currency == "INR") null else 85.0
            )
        )
        return id
    }

    @Test
    fun refreshStoresPricesLinksFundsAndConvertsUsd() = runTest {
        val gold =
            instrument("GOLDBEES", InstrumentType.STOCK, symbol = "GOLDBEES", exchange = "NSE")
        val apple = instrument(
            "Apple",
            InstrumentType.STOCK,
            symbol = "AAPL",
            exchange = "NASDAQ",
            currency = "USD",
            units = 2.0,
            cost = 500.0
        )
        val unknown = instrument("Nowhere", InstrumentType.STOCK, symbol = "NOPE", exchange = "NSE")
        instrument("Sold out", InstrumentType.STOCK, symbol = "SOLD", exchange = "NSE", units = 0.0)
        val ppfas = instrument("Parag Parikh Flexi Cap Fund", InstrumentType.MUTUAL_FUND)
        val other = instrument("Some Other Fund", InstrumentType.MUTUAL_FUND)

        val result = PriceRefresher(db, YahooQuotes(http), AmfiNav(http)).refresh(now)
        assertEquals(2, result.stocks)
        assertEquals(1, result.funds)
        assertEquals(94.73, result.usdInr!!, 1e-9)
        assertEquals(listOf("NOPE", "Some Other Fund"), result.failed)
        assertEquals(listOf("Parag Parikh Flexi Cap Fund"), result.linked)
        assertTrue(requests.none { it.contains("SOLD") })

        val goldPrice = db.instrumentDao().getSecurity(gold)!!
        assertEquals(112.69, goldPrice.lastPrice!!, 1e-9)
        assertEquals(PriceSource.YAHOO, goldPrice.lastPriceSource)
        assertEquals(LocalDate.of(2026, 11, 3), goldPrice.lastPriceDate)
        assertEquals(now, goldPrice.lastPriceFetchedAt)
        val linked = db.instrumentDao().getSecurity(ppfas)!!
        assertEquals(PriceSource.AMFI, linked.lastPriceSource)
        assertEquals(90.7043, linked.lastPrice!!, 1e-9)
        assertEquals("122639", linked.symbol)
        assertEquals("AMFI", linked.exchange)
        assertEquals("INF879O01027", linked.isin)
        assertEquals(null, db.instrumentDao().getSecurity(unknown)!!.lastPrice)
        assertEquals(null, db.instrumentDao().getSecurity(other)!!.lastPrice)
        assertEquals(
            94.73,
            db.instrumentDao().getAllFxRates().last { it.base == "USD" && it.quote == "INR" }.rate,
            1e-9
        )

        // A second refresh replaces the price in place.
        PriceRefresher(db, YahooQuotes(http), AmfiNav(http)).refresh(now.plusMinutes(5))
        assertEquals(now.plusMinutes(5), db.instrumentDao().getSecurity(gold)!!.lastPriceFetchedAt)
        assertEquals(112.69, db.instrumentDao().getSecurity(gold)!!.lastPrice!!, 1e-9)

        // The summary values the US holding in INR at the stored rate: 2 × 331.9 × 94.73.
        val summary = InvestmentSummaryBuilder.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllCategories(),
            db.transactionDao().getAllWithInstrument(),
            db.instrumentDao().getAllSecurities(),
            db.accountDao().getAll(),
            db.transactionDao().getAllWithInstrument(),
            asOf = LocalDate.of(2026, 11, 4),
            fxToInr = mapOf("USD" to 94.73),
        )
        val appleRow = summary.rows.first { it.instrument.id == apple }
        assertEquals(2 * 331.9 * 94.73, appleRow.currentValue, 0.01)
        // Costs stay in the holding's own currency; only the current value is brought to rupees.
        assertEquals(500.0, appleRow.position.costHeld, 0.01)
        assertEquals(331.9, appleRow.unitPrice!!, 1e-9)
    }
}
