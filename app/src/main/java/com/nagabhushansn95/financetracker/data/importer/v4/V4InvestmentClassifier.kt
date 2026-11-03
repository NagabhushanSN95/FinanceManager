package com.nagabhushansn95.financetracker.data.importer.v4

import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Identity of an instrument discovered from v4 particulars. */
data class InstrumentKey(
    val type: InstrumentType,
    val name: String,
    val symbol: String? = null,
    val exchange: String? = null
) {
    companion object {
        fun stock(symbol: String) = InstrumentKey(InstrumentType.STOCK, symbol, symbol, "NSE")
        fun mutualFund(name: String) = InstrumentKey(InstrumentType.MUTUAL_FUND, name)
        val FD_UNMAPPED = InstrumentKey(InstrumentType.FIXED_DEPOSIT, "Fixed Deposits (unmapped)")
        val FD_SWEEP_IN = InstrumentKey(InstrumentType.FIXED_DEPOSIT, "HDFC Sweep-In Fixed Deposit")
        val AMMA_KVGB = InstrumentKey(InstrumentType.OTHER, "Amma's KVGB Account")
        val LOAN_MAHADEVA = InstrumentKey(InstrumentType.OTHER, "Loan To Mahadeva Anna")
        fun insurance(name: String) = InstrumentKey(InstrumentType.INSURANCE, name)
    }
}

/** What a v4 row that touches the "Investments" pseudo-bank means in the v5 ledger. */
sealed class InvestmentEvent {
    /** Cash account -> instrument (v4: Transfer X Bank16). */
    data class Invest(val instrument: InstrumentKey) : InvestmentEvent()

    /** Instrument -> cash account (v4: Transfer Bank16 X). */
    data class Redeem(val instrument: InstrumentKey) : InvestmentEvent()

    /** Value credited inside an instrument with no cash leg, e.g. FD interest reinvested (v4: Credit Bank16). */
    data class ReinvestedIncome(val instrument: InstrumentKey) : InvestmentEvent()

    /** Into a provident-fund account (v4: Credit Bank16 for EPF, Transfer X Bank16 for PPF). */
    data class ProvidentFundIn(val fund: ProvidentFund, val contributor: String) : InvestmentEvent()

    /** Out of a provident-fund account (v4: Transfer Bank16 X). */
    data class ProvidentFundOut(val fund: ProvidentFund) : InvestmentEvent()

    /** Realised profit/loss bookkeeping rows of the pseudo-bank; computed in v5, so dropped. */
    data class RealisedPnl(val instrument: InstrumentKey, val isProfit: Boolean) : InvestmentEvent()
}

enum class ProvidentFund { EPF, PPF }

/** Extra tags for ordinary (non-Bank16) rows. */
data class DividendInfo(val symbol: String, val exDate: LocalDate?)

object V4InvestmentClassifier {
    private val dividend =
        Regex("^Dividend From Stocks - (\\S+)(?: \\(Ex-Dividend Date: (\\d{2}/\\d{2}/\\d{4})\\))?.*$")
    private val dpCharges = Regex("^Depository Participant Charges For Selling Stocks - (\\S+)$")
    private val exDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** For rows moving money INTO the pseudo-bank (Transfer X Bank16). */
    fun classifyInto(particulars: String): InvestmentEvent? {
        val p = particulars.trim()
        suffix(
            p,
            "Bought Stocks - "
        )?.let { return InvestmentEvent.Invest(InstrumentKey.stock(it)) }
        suffix(
            p,
            "Invested In Mutual Funds - "
        )?.let { return InvestmentEvent.Invest(InstrumentKey.mutualFund(it)) }
        suffix(p, "Mutual Funds SIP Installment - ")?.let {
            return InvestmentEvent.Invest(
                InstrumentKey.mutualFund(it)
            )
        }
        if (p.startsWith("Created A Sweep In Fixed Deposit")) return InvestmentEvent.Invest(
            InstrumentKey.FD_SWEEP_IN
        )
        if (p.startsWith("Created A Fixed Deposit")) return InvestmentEvent.Invest(InstrumentKey.FD_UNMAPPED)
        suffix(
            p,
            "Life Insurance Premium - "
        )?.let { return InvestmentEvent.Invest(InstrumentKey.insurance(it)) }
        if (p.startsWith("Invested In Public Provident Fund")) return InvestmentEvent.ProvidentFundIn(
            ProvidentFund.PPF,
            "Self"
        )
        if (p.startsWith("Transferred To Amma's KVGB Account")) return InvestmentEvent.Invest(
            InstrumentKey.AMMA_KVGB
        )
        if (p.startsWith("Loaned To Mahadeva Anna")) return InvestmentEvent.Invest(InstrumentKey.LOAN_MAHADEVA)
        return null
    }

    /** For rows moving money OUT of the pseudo-bank (Transfer Bank16 X). */
    fun classifyOutOf(particulars: String): InvestmentEvent? {
        val p = particulars.trim()
        suffix(
            p,
            "Sold Stocks In Buyback - "
        )?.let { return InvestmentEvent.Redeem(InstrumentKey.stock(it)) }
        suffix(p, "Sold Stocks - ")?.let { return InvestmentEvent.Redeem(InstrumentKey.stock(it)) }
        suffix(
            p,
            "Redeemed Mutual Funds - "
        )?.let { return InvestmentEvent.Redeem(InstrumentKey.mutualFund(it)) }
        if (p.startsWith("Sweep In Fixed Deposit Liquidated")) return InvestmentEvent.Redeem(
            InstrumentKey.FD_SWEEP_IN
        )
        if (p.startsWith("Fixed Deposit Matured") || p.startsWith("Fixed Deposit Closed")) return InvestmentEvent.Redeem(
            InstrumentKey.FD_UNMAPPED
        )
        if (p.startsWith("Withdrawn From Public Provident Fund")) return InvestmentEvent.ProvidentFundOut(
            ProvidentFund.PPF
        )
        if (p.startsWith("Mahadev Anna Paid Back") || p.startsWith("Mahadeva Anna Paid Back")) return InvestmentEvent.Redeem(
            InstrumentKey.LOAN_MAHADEVA
        )
        return null
    }

    /** For Credit Bank16 rows (value appearing inside investments without a cash leg). */
    fun classifyCredit(particulars: String): InvestmentEvent? {
        val p = particulars.trim()
        suffix(p, "Profit In Selling Stocks - ")?.let {
            return InvestmentEvent.RealisedPnl(
                InstrumentKey.stock(it),
                true
            )
        }
        suffix(
            p,
            "Profit In Redeemeeing Mutual Funds - "
        )?.let { return InvestmentEvent.RealisedPnl(InstrumentKey.mutualFund(it), true) }
        suffix(p, "Profit In Redeeming Mutual Funds - ")?.let {
            return InvestmentEvent.RealisedPnl(
                InstrumentKey.mutualFund(it),
                true
            )
        }
        if (p.startsWith("Sweep In Fixed Deposit Interest")) return InvestmentEvent.ReinvestedIncome(
            InstrumentKey.FD_SWEEP_IN
        )
        if (p.startsWith("Fixed Deposit Interest")) return InvestmentEvent.ReinvestedIncome(
            InstrumentKey.FD_UNMAPPED
        )
        if (p.startsWith("Invested In Employees Provident Fund")) {
            val contributor = when {
                p.contains("Employer", ignoreCase = true) -> "Employer"
                p.contains("Employee", ignoreCase = true) -> "Employee"
                else -> "Self"
            }
            return InvestmentEvent.ProvidentFundIn(ProvidentFund.EPF, contributor)
        }
        if (p.startsWith("Employees Provident Fund Interest")) return InvestmentEvent.ProvidentFundIn(
            ProvidentFund.EPF,
            "Interest"
        )
        if (p.startsWith("Public Provident Fund Interest")) return InvestmentEvent.ProvidentFundIn(
            ProvidentFund.PPF,
            "Interest"
        )
        return null
    }

    /** For Debit Bank16 rows. */
    fun classifyDebit(particulars: String): InvestmentEvent? {
        val p = particulars.trim()
        suffix(p, "Loss In Selling Stocks - ")?.let {
            return InvestmentEvent.RealisedPnl(
                InstrumentKey.stock(it),
                false
            )
        }
        suffix(p, "Loss In Redeeming Mutual Funds - ")?.let {
            return InvestmentEvent.RealisedPnl(
                InstrumentKey.mutualFund(it),
                false
            )
        }
        suffix(p, "Loss In Redeemeeing Mutual Funds - ")?.let {
            return InvestmentEvent.RealisedPnl(
                InstrumentKey.mutualFund(it),
                false
            )
        }
        return null
    }

    /** Dividend credited to a real bank account. */
    fun dividend(particulars: String): DividendInfo? {
        val m = dividend.matchEntire(particulars.trim()) ?: return null
        val exDate =
            m.groupValues[2].takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, exDateFormat) }
        return DividendInfo(m.groupValues[1], exDate)
    }

    /** Stock symbol a charge row refers to, if any. */
    fun chargedStock(particulars: String): String? =
        dpCharges.matchEntire(particulars.trim())?.groupValues?.get(1)

    fun isInterest(particulars: String): Boolean =
        particulars.contains("interest", ignoreCase = true)

    private fun suffix(p: String, prefix: String): String? =
        if (p.startsWith(prefix) && p.length > prefix.length) p.substring(prefix.length)
            .trim() else null
}
