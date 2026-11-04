package com.nagabhushansn95.financetracker.data.importer.zerodha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ZerodhaStatementsTest {
    @Test
    fun parsesCoinOrders() {
        val text = """
            client_id,isin,scheme_name,plan,transaction_mode,settlement_id,trade_date,ordered_at,folio_number,amount,units,nav,status,exchange_order_id,remarks,tag
            AB1234,INF000000001,PARAG PARIKH FLEXI CAP FUND,,BUY,2026191,10/10/2026,01:37 AM,1,25000,282.297,88.55,COMPLETE,4628300001, ,coinandroidsip
            AB1234,INF000000002,Some Arbitrage Fund,,BUY,2026191,10/10/2026,01:37 AM,2,45000,1127.215,39.92,REJECTED,4628300002, ,
            AB1234,INF000000003,Quant Small Cap Fund,,SELL,2026180,01/06/2026,09:00 AM,3,10000,31.5,317.46,COMPLETE,4628300003,Funds credited to 5010 on 02 JUN 2026 08:51:03:155 with reference number as 1666,
        """.trimIndent()
        val parsed = ZerodhaStatements.parse(text)
        assertEquals(StatementType.COIN_ORDERS, parsed.type)
        assertEquals(2, parsed.rows.size)                       // the rejected order is ignored
        val sip = parsed.rows[0] as TradeRow
        assertEquals("Parag Parikh Flexi Cap Fund", sip.name)
        assertEquals(LocalDate.of(2026, 10, 10), sip.date)
        assertEquals(Side.BUY, sip.side)
        assertTrue(sip.mutualFund && sip.sip)
        assertEquals(282.297, sip.quantity, 1e-9)
        assertEquals(88.55, sip.price, 1e-9)
        assertEquals(25000.0, sip.amount, 1e-9)
        assertEquals("zerodha:4628300001", sip.ref)
        val sell = parsed.rows[1] as TradeRow
        assertEquals(Side.SELL, sell.side)
        assertEquals(
            LocalDate.of(2026, 6, 2),
            sell.date
        )      // the day the money arrived, from the remark
        assertNull(sell.symbol)
    }

    @Test
    fun parsesKiteTradebookForStocksAndFunds() {
        val text = """
            symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time
            NIFTYBEES,INF204KB14I2,2026-10-06,NSE,EQ,EQ,buy,false,10.000000,258.500000,403865182,1200000034646814,2026-10-06T11:00:03
            MASPTOP50,INF769K01HP3,2026-10-21,NSE,EQ,EQ,sell,false,127.000000,103.510002,407792762,1200000085347860,2026-10-21T13:45:47
            PARAG PARIKH FLEXI CAP FUND - DIRECT PLAN,INF000000001,2026-10-12,NSE,MF,,buy,false,282.297000,88.554700,4628300001,4628300001,2026-10-12T00:00:00
        """.trimIndent()
        val parsed = ZerodhaStatements.parse(text)
        assertEquals(StatementType.KITE_TRADEBOOK, parsed.type)
        val buy = parsed.rows[0] as TradeRow
        assertEquals("NIFTYBEES", buy.symbol)
        assertEquals(2585.0, buy.amount, 1e-9)
        assertEquals("zerodha:403865182", buy.ref)
        val sell = parsed.rows[1] as TradeRow
        assertEquals(Side.SELL, sell.side)
        assertEquals(103.51, sell.price, 1e-9)                   // rounded to 4 places
        assertEquals(13145.77, sell.amount, 1e-9)
        val fund = parsed.rows[2] as TradeRow
        assertTrue(fund.mutualFund)
        assertNull(fund.symbol)
        assertEquals("Parag Parikh Flexi Cap Fund - Direct Plan", fund.name)
        assertEquals("zerodha:4628300001", fund.ref)            // same reference as the Coin order
    }

    @Test
    fun parsesDividendsAndCharges() {
        val dividends =
            ZerodhaStatements.parse("Symbol,Ex-date,Qty,Dividend per share,Total dividend\nEMBASSY,2026-08-04,454,6.31,2864.74\n")
        assertEquals(StatementType.DIVIDENDS, dividends.type)
        val d = dividends.rows.single() as DividendRow
        assertEquals("EMBASSY", d.symbol)
        assertEquals(LocalDate.of(2026, 8, 4), d.exDate)
        assertEquals(d.exDate, d.date)
        assertEquals(2864.74, d.total, 1e-9)

        val charges = ZerodhaStatements.parse("date,amount\n2026/10/06,10.87\n2026/10/13,10.89\n\n")
        assertEquals(StatementType.CHARGES, charges.type)
        assertEquals(
            listOf(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 13)),
            charges.rows.map { it.date })
        assertEquals(10.89, (charges.rows[1] as ChargeRow).amount, 1e-9)
    }

    @Test
    fun rejectsUnknownFilesAndReportsBadLines() {
        assertTrue(runCatching { ZerodhaStatements.parse("a,b,c\n1,2,3\n") }.exceptionOrNull() is ZerodhaParseException)
        val parsed = ZerodhaStatements.parse("date,amount\n2026/10/06,10.87\nnot-a-date,5\n")
        assertEquals(1, parsed.rows.size)
        assertEquals(1, parsed.skipped.size)
    }

    @Test
    fun csvHandlesQuotesAndSchemeNames() {
        assertEquals(
            listOf(listOf("a", "b,c", "d\"e")),
            ZerodhaStatements.csv("a,\"b,c\",\"d\"\"e\"\r\n")
        )
        assertEquals(
            "ICICI Prudential Value And Growth Fund",
            ZerodhaStatements.schemeName("ICICI PRUDENTIAL VALUE & GROWTH FUND")
        )
        assertEquals(
            "Nippon India Large Cap Fund",
            ZerodhaStatements.schemeName("Nippon India Large Cap Fund")
        )
    }
}
