package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * INCOME: to_account += amount (from outside; `instrument_id` may tag the source, e.g. a dividend)
 * EXPENSE: from_account -= amount, categorised (`instrument_id` may tag charges to a holding)
 * TRANSFER: from_account -= amount, to_account += amount
 * INVEST: from_account -= amount, units into `instrument_id` (from may be NULL: family FD, no cash of ours)
 * REDEEM: units out of `instrument_id`, to_account += amount (to may be NULL: FD renewed into another FD)
 */
enum class TransactionKind { INCOME, EXPENSE, TRANSFER, INVEST, REDEEM }

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["from_account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Instrument::class,
            parentColumns = ["id"],
            childColumns = ["instrument_id"],
            onDelete = ForeignKey.RESTRICT
        ),
    ],
    indices = [
        Index("date"),
        Index(value = ["from_account_id", "date"]),
        Index(value = ["to_account_id", "date"]),
        Index(value = ["category_id", "date"]),
        Index(value = ["instrument_id", "date"]),
        Index(value = ["broker_ref"], unique = true),
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_time") val createdTime: LocalDateTime,
    @ColumnInfo(name = "modified_time") val modifiedTime: LocalDateTime,
    val date: LocalDate,
    val kind: TransactionKind,
    @ColumnInfo(name = "from_account_id") val fromAccountId: Long? = null,
    @ColumnInfo(name = "to_account_id") val toAccountId: Long? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "instrument_id") val instrumentId: Long? = null,
    val particulars: String,
    val quantity: Double? = null,
    val rate: Double? = null,
    /** In the currency of the account the money leaves (or, for an income, arrives in). */
    val amount: Double,
    /**
     * For a transfer between accounts of different currencies: what arrives in the to-account, in its currency, while
     * [amount] is what left the from-account (16953 INR out, 300 AUD in). Null when both sides are the same.
     */
    @ColumnInfo(name = "to_amount") val toAmount: Double? = null,
    /**
     * Rupees per unit of the row's foreign currency, when the row itself knows it: computed on a load from [amount]
     * and [toAmount], typed on a credit into a foreign account with no load behind it, null on a spend (statistics then
     * take the account's average cost). Never set on a rupee row.
     */
    @ColumnInfo(name = "fx_rate") val fxRate: Double? = null,
    @ColumnInfo(name = "ex_date") val exDate: LocalDate? = null,
    /** PF: Employer | Employee | Self | Interest */
    val contributor: String? = null,
    /** Zerodha trade_id / order_id for idempotent re-import. */
    @ColumnInfo(name = "broker_ref") val brokerRef: String? = null,
    val hidden: Boolean = false,
    @ColumnInfo(name = "include_in_statistics") val includeInStatistics: Boolean = true,
) {
    /** What lands in the to-account: [toAmount] on a cross-currency transfer, else [amount]. */
    val arriving: Double get() = toAmount ?: amount
}

@Entity(
    tableName = "templates",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["from_account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index("from_account_id"), Index("to_account_id"), Index("category_id")]
)
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val particulars: String,
    val kind: TransactionKind,
    @ColumnInfo(name = "from_account_id") val fromAccountId: Long? = null,
    @ColumnInfo(name = "to_account_id") val toAccountId: Long? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    /** What the editor fills in when the template is picked: the rate and, when it is not one, the quantity. */
    val rate: Double? = null,
    val quantity: Double? = null,
    val hidden: Boolean = false,
)
