package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/** Where a security's last price came from. */
enum class PriceSource { YAHOO, AMFI, MANUAL }

/**
 * The listing of a stock, ETF, REIT or mutual fund: what identifies it on an exchange or at AMFI, its currency, and
 * the latest price known for it. One row per instrument of kind STOCK or MUTUAL_FUND.
 */
@Entity(
    tableName = "securities",
    foreignKeys = [ForeignKey(
        entity = Instrument::class,
        parentColumns = ["id"],
        childColumns = ["instrument_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("isin"), Index("symbol")]
)
data class Security(
    @PrimaryKey @ColumnInfo(name = "instrument_id") val instrumentId: Long = 0,
    /** Shown on the list rows and ledger lines instead of the symbol or the name. */
    @ColumnInfo(name = "short_name") val shortName: String? = null,
    val isin: String? = null,
    /** A stock's ticker, or a fund's AMFI scheme code. */
    val symbol: String? = null,
    /** NSE | BSE | NASDAQ | NYSE | AMFI */
    val exchange: String? = null,
    /** The exchange series a stock trades in when it matters for quotes: "RR" for REITs and InvITs on the NSE. */
    val series: String? = null,
    /** A fund's folio number, from the Coin order history. */
    val folio: String? = null,
    val currency: String = "INR",
    /** The latest price, in [currency]; null until a quote arrives or a price is typed. */
    @ColumnInfo(name = "last_price") val lastPrice: Double? = null,
    /** The quote's own date (the market's trading day). */
    @ColumnInfo(name = "last_price_date") val lastPriceDate: LocalDate? = null,
    @ColumnInfo(name = "last_price_source") val lastPriceSource: PriceSource? = null,
    @ColumnInfo(name = "last_price_fetched_at") val lastPriceFetchedAt: LocalDateTime? = null,
) {
    /** The ticker when it says something (an AMFI scheme code does not), else null. */
    val tickerOrNull: String? get() = symbol?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
}

/** A stock or fund with its listing: the instrument row joined to its [Security]. */
data class Listed(
    @Embedded val instrument: Instrument,
    @Embedded val security: Security,
) {
    val id: Long get() = instrument.id
    val name: String get() = instrument.name

    /** What the lists show: the short name when set, else the name. */
    val displayName: String
        get() = security.shortName?.takeIf { it.isNotBlank() } ?: instrument.name
}
