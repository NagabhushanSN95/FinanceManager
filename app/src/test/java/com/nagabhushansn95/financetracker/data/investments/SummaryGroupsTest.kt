package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.SummaryGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SummaryGroupsTest {
    private val now = LocalDateTime.of(2026, 11, 11, 9, 0)
    private val stocks = InvestmentCategory(id = 11, name = "Stocks", displayOrder = 1)
    private val etf =
        InvestmentCategory(id = 14, name = "Equity Index ETF", parentId = 11, displayOrder = 2)
    private val reit = InvestmentCategory(id = 16, name = "REIT", parentId = 11, displayOrder = 3)
    private val funds = InvestmentCategory(id = 2, name = "Mutual Funds", displayOrder = 4)
    private val byId = listOf(stocks, etf, reit, funds).associateBy { it.id }
    private fun instrument(
        id: Long,
        name: String,
        type: InstrumentType,
        category: Long?,
        symbol: String? = null
    ) =
        Instrument(
            id = id,
            name = name,
            instrumentType = type,
            categoryId = category,
            createdTime = now,
            modifiedTime = now
        ).also {
            if (symbol != null) securities[id] =
                com.nagabhushansn95.financetracker.data.db.entity.Security(id, symbol = symbol)
        }

    private val securities =
        HashMap<Long, com.nagabhushansn95.financetracker.data.db.entity.Security>()

    private val nifty =
        instrument(4, "Nippon India ETF Nifty 50 BeES", InstrumentType.STOCK, 14, "NIFTYBEES")
    private val sbiNifty = instrument(93, "SBI Nifty 50 ETF", InstrumentType.STOCK, 14, "SETFNIF50")
    private val gold =
        instrument(23, "Nippon India ETF Gold BeES", InstrumentType.STOCK, null, "GOLDBEES")
    private val embassy =
        instrument(74, "Embassy Office Parks REIT", InstrumentType.STOCK, 16, "EMBASSY")
    private val ppfas =
        instrument(29, "Parag Parikh Flexi Cap Fund", InstrumentType.MUTUAL_FUND, null)

    @Test
    fun tokensRoundTripAndMembershipFollowsKindCategoryOrInstrument() {
        val text = "type:MUTUAL_FUND,category:16,instrument:4,pf,junk,type:NOPE"
        val members = SummaryGroups.parse(text)
        assertEquals(
            listOf(
                GroupMember.Type(InstrumentType.MUTUAL_FUND),
                GroupMember.Category(16),
                GroupMember.Instrument(4),
                GroupMember.ProvidentFunds
            ),
            members
        )
        assertEquals("type:MUTUAL_FUND,category:16,instrument:4,pf", SummaryGroups.format(members))
        assertTrue(SummaryGroups.includes(members, ppfas, byId))      // by kind
        assertTrue(SummaryGroups.includes(members, embassy, byId))    // by sub-category
        assertTrue(SummaryGroups.includes(members, nifty, byId))      // named itself
        assertFalse(SummaryGroups.includes(members, sbiNifty, byId))
        assertFalse(SummaryGroups.includes(members, gold, byId))
        // A whole group takes its sub-categories.
        assertTrue(SummaryGroups.includes(listOf(GroupMember.Category(11)), embassy, byId))
        assertFalse(SummaryGroups.includes(listOf(GroupMember.Category(11)), ppfas, byId))
    }

    @Test
    fun theDefaultsAndTheDescription() {
        assertEquals(listOf("Liquid", "In Zerodha"), SummaryGroups.DEFAULTS.map { it.name })
        assertTrue(
            SummaryGroups.includesPf(
                SummaryGroup(
                    name = "x",
                    displayOrder = 1,
                    members = "pf,type:STOCK"
                )
            )
        )
        val nifty50 = SummaryGroup(
            name = "Nifty 50",
            displayOrder = 3,
            members = "instrument:4,instrument:93"
        )
        assertEquals(
            "NIFTYBEES, SETFNIF50",
            SummaryGroups.describe(
                nifty50,
                byId,
                listOf(nifty, sbiNifty).associateBy { it.id },
                securities
            )
        )
        assertEquals(
            "Stocks & ETFs, Stocks › REIT",
            SummaryGroups.describe(
                SummaryGroup(
                    name = "y",
                    displayOrder = 1,
                    members = "type:STOCK,category:16"
                ), byId
            )
        )
    }
}
