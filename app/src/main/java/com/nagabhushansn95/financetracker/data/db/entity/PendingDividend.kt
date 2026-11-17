package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A dividend a statement announced that the bank has not credited yet. It waits here, outside the ledger, until the
 * user records the credit date and the amount received (net of tax), which turns it into the stock's INCOME row
 * carrying [brokerRef]; the row here then goes. Nothing in the totals counts it meanwhile.
 */
@Entity(
    tableName = "pending_dividends",
    foreignKeys = [
        ForeignKey(
            entity = Instrument::class,
            parentColumns = ["id"],
            childColumns = ["instrument_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index("instrument_id"), Index(value = ["broker_ref"], unique = true)]
)
data class PendingDividend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    @ColumnInfo(name = "ex_date") val exDate: LocalDate,
    val quantity: Double,
    @ColumnInfo(name = "per_share") val perShare: Double,
    /** quantity × per share, before tax. */
    val gross: Double,
    /** The statement's identity for the dividend (`div:<symbol>:<ex-date>`), kept on the ledger row once recorded. */
    @ColumnInfo(name = "broker_ref") val brokerRef: String,
    @ColumnInfo(name = "imported_at") val importedAt: LocalDateTime,
)
