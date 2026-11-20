package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** What happened inside a holding without cash moving. */
enum class HoldingEventKind(val title: String) {
    /** Bonus shares: units added at no cost. */
    BONUS("Bonus shares"),

    /** A stock split: units added at no cost, the same cost now covering more units. */
    SPLIT("Stock split"),

    /** Interest the bank credited into a deposit. */
    INTEREST_CREDITED("Interest credited"),

    /** Tax the bank deducted at source from a deposit. */
    TDS_DEDUCTED("Tax deducted (TDS)");

    val addsUnits: Boolean get() = this == BONUS || this == SPLIT
}

/**
 * Something that happened to a holding and moved no money of ours: bonus or split units on a stock, interest
 * credited into or TDS deducted from a deposit. Shown in the holding's ledger beside the cash entries; never touches
 * an account balance. The ledger itself holds only rows that move money.
 */
@Entity(
    tableName = "holding_events",
    foreignKeys = [ForeignKey(
        entity = Instrument::class,
        parentColumns = ["id"],
        childColumns = ["instrument_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["instrument_id", "date"])]
)
data class HoldingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    val date: LocalDate,
    val kind: HoldingEventKind,
    /** Units added (bonus, split). */
    val units: Double? = null,
    /** Rupees (interest credited, TDS deducted). */
    val amount: Double? = null,
    /** "1:2" for a bonus (one for every two) or a split (old:new); display only. */
    val ratio: String? = null,
    val notes: String? = null,
)
