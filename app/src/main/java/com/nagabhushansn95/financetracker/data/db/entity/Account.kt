package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/** The things cash is held in. Instruments (stocks, FDs, ...) are deliberately NOT accounts; see [Instrument]. */
enum class AccountType { WALLET, BANK, CREDIT_CARD, DEMAT, PROVIDENT_FUND }

@Entity(
    tableName = "accounts",
    indices = [Index("account_type"), Index("display_order")]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "account_type") val accountType: AccountType,
    val currency: String = "INR",
    /** Stored balance in [currency]. Credit cards keep a signed balance: negative means outstanding. */
    val balance: Double = 0.0,
    @ColumnInfo(name = "account_number") val accountNumber: String? = null,
    @ColumnInfo(name = "credit_limit") val creditLimit: Double? = null,
    @ColumnInfo(name = "statement_day") val statementDay: Int? = null,
    @ColumnInfo(name = "due_day") val dueDay: Int? = null,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    val deleted: Boolean = false,
    val notes: String? = null,
    @ColumnInfo(name = "created_time") val createdTime: LocalDateTime,
    @ColumnInfo(name = "modified_time") val modifiedTime: LocalDateTime,
) {
    val isCashAccount: Boolean
        get() = accountType != AccountType.PROVIDENT_FUND
}
