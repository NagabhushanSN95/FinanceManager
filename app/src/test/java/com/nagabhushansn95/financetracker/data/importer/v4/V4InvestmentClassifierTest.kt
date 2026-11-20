package com.nagabhushansn95.financetracker.data.importer.v4

import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class V4InvestmentClassifierTest {
    @Test
    fun into() {
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.stock("GOLDBEES")),
            V4InvestmentClassifier.classifyInto("Bought Stocks - GOLDBEES")
        )
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.mutualFund("Parag Parikh Flexi Cap Fund")),
            V4InvestmentClassifier.classifyInto("Invested In Mutual Funds - Parag Parikh Flexi Cap Fund")
        )
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.mutualFund("Motilal Oswal Midcap Fund")),
            V4InvestmentClassifier.classifyInto("Mutual Funds SIP Installment - Motilal Oswal Midcap Fund")
        )
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.FD_UNMAPPED),
            V4InvestmentClassifier.classifyInto("Created A Fixed Deposit For 12 Months 1 Day")
        )
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.FD_UNMAPPED),
            V4InvestmentClassifier.classifyInto("Created A Fixed Deposit")
        )
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.FD_SWEEP_IN),
            V4InvestmentClassifier.classifyInto("Created A Sweep In Fixed Deposit For 55 Months")
        )
        val ins =
            V4InvestmentClassifier.classifyInto("Life Insurance Premium - SBI Life - Smart Platina Plus") as InvestmentEvent.Invest
        assertEquals(InstrumentType.INSURANCE, ins.instrument.type)
        assertEquals("SBI Life - Smart Platina Plus", ins.instrument.name)
        assertEquals(
            InvestmentEvent.ProvidentFundIn(ProvidentFund.PPF, "Self"),
            V4InvestmentClassifier.classifyInto("Invested In Public Provident Fund - Self")
        )
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.AMMA_KVGB),
            V4InvestmentClassifier.classifyInto("Transferred To Amma's KVGB Account From Salary Dec 2016")
        )
        assertEquals(
            InvestmentEvent.Invest(InstrumentKey.LOAN_MAHADEVA),
            V4InvestmentClassifier.classifyInto("Loaned To Mahadeva Anna For Baby Expenses")
        )
        assertNull(V4InvestmentClassifier.classifyInto("Something else"))
    }

    @Test
    fun outOf() {
        assertEquals(
            InvestmentEvent.Redeem(InstrumentKey.stock("MON100")),
            V4InvestmentClassifier.classifyOutOf("Sold Stocks - MON100")
        )
        assertEquals(
            InvestmentEvent.Redeem(InstrumentKey.stock("TCS")),
            V4InvestmentClassifier.classifyOutOf("Sold Stocks In Buyback - TCS")
        )
        assertEquals(
            InvestmentEvent.Redeem(InstrumentKey.mutualFund("SBI Magnum Midcap Fund")),
            V4InvestmentClassifier.classifyOutOf("Redeemed Mutual Funds - SBI Magnum Midcap Fund")
        )
        assertEquals(
            InvestmentEvent.Redeem(InstrumentKey.FD_UNMAPPED),
            V4InvestmentClassifier.classifyOutOf("Fixed Deposit Matured And Closed")
        )
        assertEquals(
            InvestmentEvent.Redeem(InstrumentKey.FD_UNMAPPED),
            V4InvestmentClassifier.classifyOutOf("Fixed Deposit Closed Prematurely")
        )
        assertEquals(
            InvestmentEvent.Redeem(InstrumentKey.FD_SWEEP_IN),
            V4InvestmentClassifier.classifyOutOf("Sweep In Fixed Deposit Liquidated Partially")
        )
        assertEquals(
            InvestmentEvent.ProvidentFundOut(ProvidentFund.PPF),
            V4InvestmentClassifier.classifyOutOf("Withdrawn From Public Provident Fund - Self")
        )
        assertEquals(
            InvestmentEvent.Redeem(InstrumentKey.LOAN_MAHADEVA),
            V4InvestmentClassifier.classifyOutOf("Mahadev Anna Paid Back")
        )
    }

    @Test
    fun pseudoBankCreditsAndDebits() {
        assertEquals(
            InvestmentEvent.RealisedPnl(InstrumentKey.stock("MON100"), true),
            V4InvestmentClassifier.classifyCredit("Profit In Selling Stocks - MON100")
        )
        assertEquals(
            InvestmentEvent.RealisedPnl(
                InstrumentKey.mutualFund("SBI Magnum Midcap Fund"),
                true
            ),
            V4InvestmentClassifier.classifyCredit("Profit In Redeemeeing Mutual Funds - SBI Magnum Midcap Fund")
        )
        assertEquals(
            InvestmentEvent.ReinvestedIncome(InstrumentKey.FD_UNMAPPED),
            V4InvestmentClassifier.classifyCredit("Fixed Deposit Interest Credited")
        )
        assertEquals(
            InvestmentEvent.ReinvestedIncome(InstrumentKey.FD_SWEEP_IN),
            V4InvestmentClassifier.classifyCredit("Sweep In Fixed Deposit Interest Credited")
        )
        assertEquals(
            InvestmentEvent.ProvidentFundIn(ProvidentFund.EPF, "Employer"),
            V4InvestmentClassifier.classifyCredit("Invested In Employees Provident Fund - Employer Contribution")
        )
        assertEquals(
            InvestmentEvent.ProvidentFundIn(ProvidentFund.EPF, "Employee"),
            V4InvestmentClassifier.classifyCredit("Invested In Employees Provident Fund - Employee Contribution")
        )
        assertEquals(
            InvestmentEvent.ProvidentFundIn(ProvidentFund.EPF, "Interest"),
            V4InvestmentClassifier.classifyCredit("Employees Provident Fund Interest Credited")
        )
        assertEquals(
            InvestmentEvent.ProvidentFundIn(ProvidentFund.PPF, "Interest"),
            V4InvestmentClassifier.classifyCredit("Public Provident Fund Interest Credited")
        )
        assertEquals(
            InvestmentEvent.RealisedPnl(InstrumentKey.stock("OLAELEC"), false),
            V4InvestmentClassifier.classifyDebit("Loss In Selling Stocks - OLAELEC")
        )
        assertEquals(
            InvestmentEvent.RealisedPnl(
                InstrumentKey.mutualFund("Mirae Asset Focused Fund"),
                false
            ),
            V4InvestmentClassifier.classifyDebit("Loss In Redeeming Mutual Funds - Mirae Asset Focused Fund")
        )
    }

    @Test
    fun dividendsAndCharges() {
        assertEquals(
            DividendInfo("BAGMANE", LocalDate.of(2026, 8, 14)),
            V4InvestmentClassifier.dividend("Dividend From Stocks - BAGMANE (Ex-Dividend Date: 14/08/2026)")
        )
        assertEquals(
            DividendInfo("NTPC", null),
            V4InvestmentClassifier.dividend("Dividend From Stocks - NTPC")
        )
        assertNull(V4InvestmentClassifier.dividend("Salary"))
        assertEquals(
            "MON100",
            V4InvestmentClassifier.chargedStock("Depository Participant Charges For Selling Stocks - MON100")
        )
        assertNull(V4InvestmentClassifier.chargedStock("Zerodha Transaction Charges"))
    }
}
