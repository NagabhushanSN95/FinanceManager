package com.nagabhushansn95.financetracker.data.importer.zerodha

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

enum class StatementType(val title: String) {
    COIN_ORDERS("Coin order history"),
    KITE_TRADEBOOK("Kite tradebook"),
    DIVIDENDS("Dividends"),

    /** Console › Funds › Statement: every rupee in and out of the trading account. */
    LEDGER("Console ledger"),

    /** The equity tradebook with the ledger of the same period, imported together. */
    TRADEBOOK_AND_LEDGER("Kite tradebook and ledger"),
}

enum class Side { BUY, SELL }

sealed interface ZerodhaRow {
    val date: LocalDate
}

/** A buy or sell of a stock, ETF or mutual fund. [ref] is Zerodha's id for the trade or order. */
data class TradeRow(
    override val date: LocalDate,
    val side: Side,
    /** NSE symbol for stocks; null for funds. */
    val symbol: String?,
    /** Display name: the symbol for stocks, the scheme name for funds. */
    val name: String,
    val isin: String?,
    val quantity: Double,
    val price: Double,
    val amount: Double,
    val ref: String,
    val mutualFund: Boolean,
    val sip: Boolean = false,
    /** NSE or BSE from the tradebook; null for funds. */
    val exchange: String? = null,
    /** The tradebook's series (EQ, BE, RR …); null for funds. */
    val series: String? = null,
    /** Coin's order date (the day the bank was debited) when the file carries one; else null. */
    val orderDate: LocalDate? = null,
    /** Coin's folio number for the scheme. */
    val folio: String? = null,
) : ZerodhaRow {
    /** The day the money moved: a fund buy on its order date, everything else on the trade date. */
    val cashDate: LocalDate get() = if (mutualFund && side == Side.BUY) orderDate ?: date else date
}

data class DividendRow(
    override val date: LocalDate,
    val exDate: LocalDate?,
    val symbol: String,
    val isin: String?,
    val quantity: Double,
    val perShare: Double,
    val total: Double,
) : ZerodhaRow

/**
 * One line of the Console ledger. [amount] is the money out of the trading account (debit less credit) with the six
 * decimals Zerodha keeps; [reference] is the number the text carries (UPI, payout or settlement), the stable handle
 * for the row across exports.
 */
data class LedgerRow(
    override val date: LocalDate,
    val particulars: String,
    val voucherType: String,
    val amount: Double,
    val reference: String?,
) : ZerodhaRow {
    /** "Net settlement for Equity …": the day's trades and their charges in one figure. */
    val settlement: Boolean get() = particulars.startsWith("Net settlement for", ignoreCase = true)
    val buyback: Boolean get() = settlement && particulars.contains("buyback", ignoreCase = true)

    /** A brokerage refund posted beside the settlement; it belongs to the day's charges. */
    val brokerageReversal: Boolean
        get() = particulars.contains(
            "Reversal of Brokerage",
            ignoreCase = true
        )
    val bankIn: Boolean get() = voucherType.equals("Bank Receipts", ignoreCase = true)
    val bankOut: Boolean get() = voucherType.equals("Bank Payments", ignoreCase = true)
    val quarterlySettlement: Boolean
        get() = bankOut && (particulars.contains("quarterly settlement", ignoreCase = true) ||
                particulars.contains("auto-settled", ignoreCase = true))
    val autopay: Boolean get() = bankIn && particulars.contains("autopay", ignoreCase = true)

    /** The stock and the sale date of a "DP Charges for Sale of X on dd/mm/yyyy" line. */
    val dpCharge: Pair<String, LocalDate>?
        get() = DP.find(particulars)?.let { m ->
            m.groupValues[1] to LocalDate.of(
                m.groupValues[4].toInt(),
                m.groupValues[3].toInt(),
                m.groupValues[2].toInt()
            )
        }

    companion object {
        private val DP = Regex(
            "DP Charges for Sale of (\\S+) on (\\d\\d)/(\\d\\d)/(\\d{4})",
            RegexOption.IGNORE_CASE
        )
    }
}

data class ParsedStatement(
    val type: StatementType,
    val rows: List<ZerodhaRow>,
    val skipped: List<String>
)

class ZerodhaParseException(message: String) : Exception(message)

/**
 * Parses the CSV statements Zerodha hands out, recognised by their header row: Coin order history, Kite tradebook
 * (equity or mutual-fund segment), a dividends list and the Console ledger (Funds › Statement).
 */
object ZerodhaStatements {
    private val COIN_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val REMARK_DATE: DateTimeFormatter =
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yyyy")
            .toFormatter(Locale.ENGLISH)
    private val DATE_FORMATS = listOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "yyyy/MM/dd",
        "dd-MM-yyyy"
    ).map { DateTimeFormatter.ofPattern(it) }
    private val FUNDS_CREDITED = Regex("\\bon\\s+(\\d{1,2} [A-Za-z]{3} \\d{4})")

    fun parse(text: String): ParsedStatement {
        val table = csv(text).filter { row -> row.any { it.isNotBlank() } }
        if (table.isEmpty()) throw ZerodhaParseException("The file is empty")
        val headers = table.first().map { it.trim().lowercase() }
        val type = detect(headers) ?: throw ZerodhaParseException(
            "Not a Zerodha statement I recognise (columns: ${
                table.first().joinToString(", ")
            })"
        )
        val index = headers.withIndex().associate { (i, h) -> h to i }
        val skipped = ArrayList<String>()
        fun col(row: List<String>, vararg names: String): String {
            for (n in names) index[n.lowercase()]?.let { i ->
                return row.getOrNull(i)?.trim() ?: ""
            }
            return ""
        }

        val rows = table.drop(1).mapIndexedNotNull { n, row ->
            try {
                when (type) {
                    StatementType.COIN_ORDERS -> coin(row, ::col)
                    StatementType.KITE_TRADEBOOK -> tradebook(row, ::col)
                    StatementType.DIVIDENDS -> dividend(row, ::col)
                    StatementType.LEDGER -> ledger(row, ::col)
                    StatementType.TRADEBOOK_AND_LEDGER -> throw ZerodhaParseException("not a file")
                }
            } catch (e: Exception) {
                skipped += "Line ${n + 2}: ${e.message ?: e.toString()}"
                null
            }
        }
        return ParsedStatement(type, rows, skipped)
    }

    fun detect(headers: List<String>): StatementType? {
        val h = headers.map { it.trim().lowercase() }.toSet()
        return when {
            "scheme_name" in h && "transaction_mode" in h -> StatementType.COIN_ORDERS
            "trade_type" in h && "trade_id" in h -> StatementType.KITE_TRADEBOOK
            "symbol" in h && h.any { it.startsWith("dividend per share") } -> StatementType.DIVIDENDS
            "posting_date" in h && "voucher_type" in h && "net_balance" in h -> StatementType.LEDGER
            else -> null
        }
    }

    private fun coin(
        row: List<String>,
        col: (List<String>, Array<out String>) -> String
    ): TradeRow? {
        fun c(vararg n: String) = col(row, n)
        if (!c("status").equals("COMPLETE", ignoreCase = true)) return null
        val side =
            if (c("transaction_mode").equals("SELL", ignoreCase = true)) Side.SELL else Side.BUY
        val remarks = c("remarks")
        var date = LocalDate.parse(c("trade_date"), COIN_DATE)
        if (side == Side.SELL) {
            // "Funds credited to 5010… on 02 JUN 2026 08:51:03:155 with reference …": the day the money arrived.
            FUNDS_CREDITED.find(remarks)
                ?.let { date = LocalDate.parse(it.groupValues[1], REMARK_DATE) }
        }
        val units = c("units").toDouble()
        val nav = c("nav").toDouble()
        val amount = c("amount").toDoubleOrNull() ?: (units * nav)
        val ref =
            c("exchange_order_id").ifBlank { "${c("settlement_id")}:${c("isin")}:${c("trade_date")}" }
        val tag = c("tag")
        // "ordered_at" is a time of day in some exports and a full timestamp in others; only a date counts.
        val orderDate = c("ordered_at").split(' ', 'T').firstOrNull { part ->
            runCatching { anyDate(part) }.isSuccess
        }?.let(::anyDate)
        return TradeRow(
            date = date,
            side = side,
            symbol = null,
            name = schemeName(c("scheme_name")),
            isin = c("isin").ifBlank { null },
            quantity = units,
            price = nav,
            amount = round2(amount),
            ref = "zerodha:$ref",
            mutualFund = true,
            sip = remarks.contains("sip", ignoreCase = true) || tag.contains(
                "sip",
                ignoreCase = true
            ),
            orderDate = orderDate,
            folio = c("folio_number").ifBlank { null },
        )
    }

    private fun tradebook(
        row: List<String>,
        col: (List<String>, Array<out String>) -> String
    ): TradeRow {
        fun c(vararg n: String) = col(row, n)
        val mutualFund = c("segment").equals("MF", ignoreCase = true)
        val side = if (c("trade_type").equals("sell", ignoreCase = true)) Side.SELL else Side.BUY
        val quantity = c("quantity").toDouble()
        val price = Math.round(c("price").toDouble() * 10000.0) / 10000.0
        val symbol = c("symbol")
        return TradeRow(
            date = LocalDate.parse(c("trade_date"), DateTimeFormatter.ISO_LOCAL_DATE),
            side = side,
            symbol = if (mutualFund) null else symbol,
            name = if (mutualFund) schemeName(symbol) else symbol,
            isin = c("isin").ifBlank { null },
            quantity = quantity,
            price = price,
            amount = round2(quantity * price),
            // A fund order's trade id is Coin's exchange order id, so the two statements de-duplicate against each other.
            ref = "zerodha:${c("trade_id")}",
            mutualFund = mutualFund,
            exchange = if (mutualFund) null else c("exchange").uppercase().ifBlank { null },
            series = if (mutualFund) null else c("series").uppercase().ifBlank { null },
        )
    }

    private fun dividend(
        row: List<String>,
        col: (List<String>, Array<out String>) -> String
    ): DividendRow {
        fun c(vararg n: String) = col(row, n)
        val exDate = c("ex-date", "ex date", "ex_date", "exdate").ifBlank { null }?.let(::anyDate)
        val date =
            c("date", "credit date", "payment date", "pay date").ifBlank { null }?.let(::anyDate)
                ?: exDate
                ?: throw ZerodhaParseException("no date")
        val quantity = c("qty", "quantity").toDouble()
        val perShare = c("dividend per share").toDouble()
        val total = c("total dividend", "net dividend amount", "total").toDoubleOrNull()
            ?: (quantity * perShare)
        return DividendRow(
            date = date,
            exDate = exDate,
            symbol = c("symbol"),
            isin = c("isin").ifBlank { null },
            quantity = quantity,
            perShare = perShare,
            total = round2(total)
        )
    }

    private val REFERENCE =
        Regex("(?:reference number|settlement number):?\\s+(\\S+)", RegexOption.IGNORE_CASE)

    /** Opening and closing balance lines have no date and are not rows. */
    private fun ledger(
        row: List<String>,
        col: (List<String>, Array<out String>) -> String
    ): LedgerRow? {
        fun c(vararg n: String) = col(row, n)
        val date = c("posting_date").ifBlank { return null }
        val particulars = c("particulars").trim()
        val debit = c("debit").toDoubleOrNull() ?: 0.0
        val credit = c("credit").toDoubleOrNull() ?: 0.0
        return LedgerRow(
            date = anyDate(date),
            particulars = particulars,
            voucherType = c("voucher_type").trim(),
            amount = debit - credit,
            reference = REFERENCE.find(particulars)?.groupValues?.get(1)?.trimEnd('.', ','),
        )
    }

    fun anyDate(text: String): LocalDate {
        val t = text.trim().substringBefore(' ').substringBefore('T')
        for (f in DATE_FORMATS) runCatching { return LocalDate.parse(t, f) }
        throw ZerodhaParseException("unreadable date '$text'")
    }

    /**
     * The Coin order history with the allotment prices of the MF tradebook: each order takes the price of the
     * tradebook row with the same order id, or failing that the row for the same ISIN and units within five days of
     * it. Orders without a tradebook row keep Coin's NAV and are listed in [ParsedStatement.skipped] as a note.
     */
    fun mergeCoinWithTradebook(coin: ParsedStatement, tradebook: ParsedStatement): ParsedStatement {
        require(coin.type == StatementType.COIN_ORDERS) { "The first file is not a Coin order history" }
        require(tradebook.type == StatementType.KITE_TRADEBOOK) { "The second file is not a Kite tradebook" }
        val trades =
            tradebook.rows.filterIsInstance<TradeRow>().filter { it.mutualFund }.toMutableList()
        require(trades.isNotEmpty()) { "The tradebook has no mutual-fund rows; export the MF segment" }
        val notes = ArrayList<String>()
        val merged = coin.rows.map { row ->
            if (row !is TradeRow) return@map row
            val match = trades.firstOrNull { it.ref == row.ref }
                ?: trades.firstOrNull {
                    it.isin != null && it.isin == row.isin && kotlin.math.abs(it.quantity - row.quantity) < 1e-3 &&
                            kotlin.math.abs(
                                java.time.temporal.ChronoUnit.DAYS.between(
                                    it.date,
                                    row.date
                                )
                            ) <= 5
                }
            if (match == null) {
                notes += "${row.name} on ${row.date}: no tradebook row, Coin's NAV kept"
                row
            } else {
                trades.remove(match)
                row.copy(price = match.price)
            }
        }
        return ParsedStatement(
            StatementType.COIN_ORDERS,
            merged,
            coin.skipped + tradebook.skipped + notes
        )
    }

    /**
     * The equity rows of a Kite tradebook with the Console ledger of the same period. Fund rows are left to the Coin
     * import; the ledger supplies the bank movements, the DP and account charges, and the settlements from which the
     * day's transaction charges follow. Trades outside the ledger's days are left out, so a trade never comes in
     * without the settlement that carries its charges; a settlement without its trades is left out by the planner.
     */
    fun mergeTradebookWithLedger(
        tradebook: ParsedStatement,
        ledger: ParsedStatement
    ): ParsedStatement {
        require(tradebook.type == StatementType.KITE_TRADEBOOK) { "The first file is not a Kite tradebook" }
        require(ledger.type == StatementType.LEDGER) { "The second file is not a Console ledger" }
        require(ledger.rows.isNotEmpty()) { "The ledger has no rows" }
        val first = ledger.rows.minOf { it.date }
        val last = ledger.rows.maxOf { it.date }
        val equity = tradebook.rows.filterNot { it is TradeRow && it.mutualFund }
        val funds = tradebook.rows.size - equity.size
        val inPeriod = equity.filter { it.date in first..last }
        val outside = equity.size - inPeriod.size
        val notes = ArrayList<String>()
        if (funds > 0) notes += "$funds fund row${if (funds == 1) "" else "s"} left out: fund orders come in with the Coin import under Mutual funds"
        if (outside > 0) notes += "$outside trade${if (outside == 1) "" else "s"} outside the ledger's days ($first to $last) left out"
        return ParsedStatement(
            StatementType.TRADEBOOK_AND_LEDGER,
            inPeriod + ledger.rows,
            tradebook.skipped + ledger.skipped + notes
        )
    }

    /** Fund houses whose names are acronyms and must stay upper case when a shouting tradebook name is title-cased. */
    private val ACRONYMS = setOf(
        "ICICI",
        "HDFC",
        "SBI",
        "PGIM",
        "UTI",
        "DSP",
        "HSBC",
        "IDFC",
        "LIC",
        "PPFAS",
        "ETF",
        "FOF",
        "IDBI",
        "BOI",
        "PNB",
        "ITI",
        "JM",
        "NJ",
        "WOC"
    )

    /**
     * "PARAG PARIKH FLEXI CAP FUND - DIRECT PLAN" → "Parag Parikh Flexi Cap Fund - Direct Plan"; a mixed-case Coin
     * name only gets its first letters capitalised, and "&" becomes "And", as 4.x did.
     */
    fun schemeName(raw: String): String {
        val words = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val shouting = words.all { w -> w.none { it.isLowerCase() } }
        return words.map { if (it == "&") "And" else it.replace("&", "And") }
            .joinToString(" ") { w ->
                when {
                    w == "And" -> w
                    shouting && w.uppercase() in ACRONYMS -> w.uppercase()
                    shouting -> w.lowercase().replaceFirstChar { it.uppercase() }
                    else -> w.replaceFirstChar { it.uppercase() }
                }
            }
    }

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    /** RFC-4180-ish: quoted fields with doubled quotes, CR/LF line ends. */
    fun csv(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var field = StringBuilder()
        var row = ArrayList<String>()
        var quoted = false
        var i = 0
        val s = text.removePrefix("﻿")
        while (i < s.length) {
            val ch = s[i]
            when {
                quoted -> when {
                    ch == '"' && i + 1 < s.length && s[i + 1] == '"' -> {
                        field.append('"'); i++
                    }

                    ch == '"' -> quoted = false
                    else -> field.append(ch)
                }

                ch == '"' -> quoted = true
                ch == ',' -> {
                    row.add(field.toString()); field = StringBuilder()
                }

                ch == '\r' -> {}
                ch == '\n' -> {
                    row.add(field.toString()); rows.add(row); row = ArrayList(); field =
                        StringBuilder()
                }

                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString()); rows.add(row)
        }
        return rows
    }
}
