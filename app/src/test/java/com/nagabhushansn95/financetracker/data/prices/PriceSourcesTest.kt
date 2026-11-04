package com.nagabhushansn95.financetracker.data.prices

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class PriceSourcesTest {
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private fun instrument(
        name: String,
        type: InstrumentType = InstrumentType.MUTUAL_FUND,
        symbol: String? = null,
        isin: String? = null,
        exchange: String? = null
    ) =
        Instrument(
            name = name,
            instrumentType = type,
            symbol = symbol,
            isin = isin,
            exchange = exchange,
            displayOrder = 1,
            createdTime = now,
            modifiedTime = now
        )

    @Test
    fun yahooParsesTheChartMeta() {
        val json =
            """{"chart":{"result":[{"meta":{"currency":"INR","symbol":"GOLDBEES.NS","exchangeName":"NSI","exchangeTimezoneName":"Asia/Kolkata",
            "regularMarketTime":1793700000,"regularMarketPrice":112.69,"longName":"Nippon India ETF Gold BeES"},"timestamp":[1793700000],
            "indicators":{"quote":[{"close":[112.69000244140625]}]}}],"error":null}}"""
        val q = YahooQuotes.parse(json)
        assertEquals("GOLDBEES.NS", q.symbol)
        assertEquals(112.69, q.price, 1e-9)
        assertEquals("INR", q.currency)
        assertEquals(LocalDate.of(2026, 11, 3), q.date)
        assertEquals("Nippon India ETF Gold BeES", q.name)
        val error =
            runCatching { YahooQuotes.parse("""{"chart":{"result":null,"error":{"code":"Not Found","description":"No data found, symbol may be delisted"}}}""") }
        assertEquals("No data found, symbol may be delisted", error.exceptionOrNull()?.message)
    }

    @Test
    fun yahooTickers() {
        val yahoo = YahooQuotes(object : HttpClient {
            override suspend fun get(url: String) = ""
        })
        assertEquals(
            "GOLDBEES.NS",
            yahoo.ticker(
                instrument(
                    "x",
                    InstrumentType.STOCK,
                    symbol = "GOLDBEES",
                    exchange = "NSE"
                )
            )
        )
        assertEquals(
            "GOLDBEES.NS",
            yahoo.ticker(instrument("x", InstrumentType.STOCK, symbol = "GOLDBEES"))
        )
        assertEquals(
            "500325.BO",
            yahoo.ticker(instrument("x", InstrumentType.STOCK, symbol = "500325", exchange = "BSE"))
        )
        assertEquals(
            "AAPL",
            yahoo.ticker(
                instrument(
                    "x",
                    InstrumentType.STOCK,
                    symbol = "AAPL",
                    exchange = "NASDAQ"
                )
            )
        )
        assertEquals(
            "BRK-B.X",
            yahoo.ticker(
                instrument(
                    "x",
                    InstrumentType.STOCK,
                    symbol = "BRK-B.X",
                    exchange = "NSE"
                )
            )
        )
        assertNull(yahoo.ticker(instrument("x", InstrumentType.STOCK)))
    }

    private val navAll =
        """Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Plan;Option;Net Asset Value;Date
 
Open Ended Schemes(Equity Scheme - Flexi Cap Fund)
 
PPFAS Mutual Fund
 
122639;INF879O01027;-;Parag Parikh Flexi Cap Fund;Direct Plan;Growth;90.7043;03-Nov-2026
153964;-;INF879O01308;Parag Parikh Flexi Cap Fund;Direct Plan;Monthly IDCW Payout;90.7043;03-Nov-2026
122640;INF879O01019;-;Parag Parikh Flexi Cap Fund;Regular Plan;Growth;82.5606;03-Nov-2026
127042;INF247L01445;-;Motilal Oswal Midcap Fund;;;114.5393;03-Nov-2026
127039;INF247L01411;-;Motilal Oswal Midcap Fund;;;98.9861;03-Nov-2026
138456;INF223J01NL0;-;PGIM India Emerging Markets Equity Fund of Fund;Direct Plan;Direct Growth;22.63;02-Nov-2026
138453;INF223J01AY0;-;PGIM India Emerging Markets Equity Fund of Fund;Regular Plan;Regular Growth;19.6;02-Nov-2026
bad;line;with;no;code
"""

    @Test
    fun amfiParsesAndMatches() {
        val schemes = AmfiNav.parse(navAll)
        assertEquals(7, schemes.size)
        val ppfas = schemes.first()
        assertEquals("122639", ppfas.code)
        assertEquals("INF879O01027", ppfas.isinGrowth)
        assertNull(ppfas.isinReinvestment)
        assertEquals(90.7043, ppfas.nav, 1e-9)
        assertEquals(LocalDate.of(2026, 11, 3), ppfas.date)
        assertEquals("INF879O01308", schemes[1].isinReinvestment)

        // ISIN first, then scheme code, then the one Direct-Growth plan with that name.
        assertEquals(
            "122640",
            AmfiNav.match(instrument("Whatever", isin = "INF879O01019"), schemes)!!.code
        )
        assertEquals(
            "153964",
            AmfiNav.match(
                instrument("Whatever", symbol = "153964", exchange = "AMFI"),
                schemes
            )!!.code
        )
        assertEquals(
            "122639",
            AmfiNav.match(instrument("Parag Parikh Flexi Cap Fund"), schemes)!!.code
        )
        assertEquals(
            "138456",
            AmfiNav.match(
                instrument("PGIM India Emerging Markets Equity Fund Of Fund"),
                schemes
            )!!.code
        )
        // Two plans with blank plan/option columns: ambiguous, so no match.
        assertNull(AmfiNav.match(instrument("Motilal Oswal Midcap Fund"), schemes))
        assertNull(AmfiNav.match(instrument("Nippon India Large Cap Fund"), schemes))

        val found = AmfiNav.search("parag growth", schemes)
        assertEquals(listOf("122639", "122640"), found.map { it.code })
    }
}
