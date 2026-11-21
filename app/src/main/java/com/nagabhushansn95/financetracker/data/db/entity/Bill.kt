package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * How a bill's due dates follow one another. CALENDAR: every so many months on a day of the month (rent, a card,
 * a yearly tax). DURATION: the next one falls due when the last payment's validity runs out (a mobile recharge).
 */
enum class BillSchedule { CALENDAR, DURATION }

/**
 * A standing commitment: what is paid, from where, to whom or under which category, how much is expected, and when
 * it falls due. Each payment is a ledger row linked through [BillPayment]; the bill itself never moves money.
 * [kind] is EXPENSE or TRANSFER (a card bill or a mudupu goes into another account of ours).
 */
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["from_account_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
    ],
    indices = [Index("from_account_id"), Index("to_account_id"), Index("category_id")]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    @ColumnInfo(name = "from_account_id") val fromAccountId: Long? = null,
    @ColumnInfo(name = "to_account_id") val toAccountId: Long? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    /** What is usually paid; the history keeps what was actually paid. Null when it varies (a card, electricity). */
    val amount: Double? = null,
    /** The particulars of a payment, with `{period}` standing for the period covered ("Home Rent - {period}"). */
    val particulars: String? = null,
    val schedule: BillSchedule = BillSchedule.CALENDAR,
    /** CALENDAR: the months between due dates (1 monthly, 3 quarterly, 12 yearly). */
    @ColumnInfo(name = "every_months") val everyMonths: Int = 1,
    /** The first due date; its day of the month is the due day of every later one (clamped to shorter months). */
    @ColumnInfo(name = "first_due") val firstDue: LocalDate,
    /** CALENDAR: the payment covers the month(s) before the due month (rent paid in November for October). */
    @ColumnInfo(name = "covers_previous") val coversPrevious: Boolean = false,
    /** DURATION: how long a payment usually holds, offered when the payment is recorded. */
    @ColumnInfo(name = "validity_days") val validityDays: Int? = null,
    /** Alert only once the due date has passed, instead of in the run-up to it: for bills that can wait. */
    @ColumnInfo(
        name = "alert_when_overdue",
        defaultValue = "0"
    ) val alertWhenOverdue: Boolean = false,
    /** Set when the bill stopped (the maid left, the subscription was cancelled); the history stays. */
    @ColumnInfo(name = "ended_on") val endedOn: LocalDate? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_time") val createdTime: LocalDateTime,
    @ColumnInfo(name = "modified_time") val modifiedTime: LocalDateTime,
) {
    val ended: Boolean get() = endedOn != null
}

/**
 * One period of a bill settled: the period it covers, when it was due, and the ledger row that paid it. Without a
 * row the period was skipped (no YouTube that month) or waived; that keeps the bill from reading as overdue.
 * Deleting the ledger row removes this with it, so the period reads as unpaid again.
 */
@Entity(
    tableName = "bill_payments",
    foreignKeys = [
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["bill_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index(value = ["bill_id", "period_start"]), Index("transaction_id")]
)
data class BillPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bill_id") val billId: Long,
    @ColumnInfo(name = "period_start") val periodStart: LocalDate,
    @ColumnInfo(name = "period_end") val periodEnd: LocalDate,
    @ColumnInfo(name = "due_date") val dueDate: LocalDate,
    @ColumnInfo(name = "transaction_id") val transactionId: Long? = null,
    val note: String? = null,
) {
    val skipped: Boolean get() = transactionId == null
}

/** A credit bureau (CIBIL, Experian, …) whose scores are recorded under Bills. */
@Entity(tableName = "credit_agencies")
data class CreditAgency(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    val notes: String? = null,
)

/** One reading of a credit score: the agency, the date it was checked, and the score. Not a transaction. */
@Entity(
    tableName = "credit_scores",
    foreignKeys = [ForeignKey(
        entity = CreditAgency::class,
        parentColumns = ["id"],
        childColumns = ["agency_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["agency_id", "date"])]
)
data class CreditScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "agency_id") val agencyId: Long,
    val date: LocalDate,
    val score: Int,
    val notes: String? = null,
)
