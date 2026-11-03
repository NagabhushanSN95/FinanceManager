package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class InstrumentType { STOCK, MUTUAL_FUND, FIXED_DEPOSIT, INSURANCE, OTHER }

enum class RiskBucket { NONE, LOW, MEDIUM, HIGH }

/** Equity, ETF, Gold ETF, REIT, Arbitrage, Flexi Cap, Life Insurance, ... with the targets from the sheet's Investment Summary. */
@Entity(
    tableName = "investment_categories",
    foreignKeys = [ForeignKey(
        entity = InvestmentCategory::class,
        parentColumns = ["id"],
        childColumns = ["parent_id"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("parent_id"), Index("display_order")]
)
data class InvestmentCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "risk_bucket") val riskBucket: RiskBucket = RiskBucket.NONE,
    @ColumnInfo(name = "target_pct") val targetPct: Double? = null,
    @ColumnInfo(name = "tolerance_pct") val tolerancePct: Double? = null,
    @ColumnInfo(name = "target_goal_pct") val targetGoalPct: Double? = null,
    @ColumnInfo(name = "target_wealth_pct") val targetWealthPct: Double? = null,
    @ColumnInfo(name = "rebalance_notes") val rebalanceNotes: String? = null,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
)

/** The things you invest in. Never has a stored balance: units, cost basis and P/L are computed from the ledger. */
@Entity(
    tableName = "instruments",
    foreignKeys = [ForeignKey(
        entity = InvestmentCategory::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("instrument_type"), Index("category_id"), Index("isin"), Index("symbol"), Index(
        "display_order"
    )]
)
data class Instrument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "short_name") val shortName: String? = null,
    @ColumnInfo(name = "instrument_type") val instrumentType: InstrumentType,
    val isin: String? = null,
    val symbol: String? = null,
    /** NSE | BSE | NASDAQ | NYSE | AMFI */
    val exchange: String? = null,
    val currency: String = "INR",
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    /** "Self" for own investments; family FDs carry the family member's name and are excluded from own totals. */
    val owner: String = OWNER_SELF,
    val active: Boolean = true,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    val notes: String? = null,
    @ColumnInfo(name = "created_time") val createdTime: LocalDateTime,
    @ColumnInfo(name = "modified_time") val modifiedTime: LocalDateTime,
) {
    companion object {
        const val OWNER_SELF = "Self"
    }
}

enum class FdStatus { ACTIVE, CLOSED_AT_MATURITY, RENEWED, CLOSED_PREMATURELY }

/** Static terms of a fixed deposit. The deposit amount and date are the INVEST transaction, not stored here. */
@Entity(
    tableName = "fixed_deposits",
    foreignKeys = [
        ForeignKey(
            entity = Instrument::class,
            parentColumns = ["id"],
            childColumns = ["instrument_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Instrument::class,
            parentColumns = ["id"],
            childColumns = ["previous_fd_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Instrument::class,
            parentColumns = ["id"],
            childColumns = ["next_fd_id"],
            onDelete = ForeignKey.SET_NULL
        ),
    ],
    indices = [Index("status"), Index("maturity_date"), Index("previous_fd_id"), Index("next_fd_id")]
)
data class FixedDeposit(
    @PrimaryKey @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    @ColumnInfo(name = "certificate_no") val certificateNo: String? = null,
    @ColumnInfo(name = "holder_name") val holderName: String? = null,
    @ColumnInfo(name = "bank_name") val bankName: String? = null,
    val branch: String? = null,
    @ColumnInfo(name = "bank_account_no") val bankAccountNo: String? = null,
    @ColumnInfo(name = "cust_id") val custId: String? = null,
    @ColumnInfo(name = "period_months") val periodMonths: Int? = null,
    @ColumnInfo(name = "period_days") val periodDays: Int? = null,
    /** Annual interest rate in percent, e.g. 7.25 */
    val rate: Double? = null,
    @ColumnInfo(name = "compounding_months") val compoundingMonths: Int? = null,
    @ColumnInfo(name = "maturity_date") val maturityDate: LocalDate? = null,
    @ColumnInfo(name = "maturity_amount_printed") val maturityAmountPrinted: Double? = null,
    @ColumnInfo(name = "tds_rate") val tdsRate: Double? = null,
    @ColumnInfo(name = "tds_reported") val tdsReported: Double? = null,
    val status: FdStatus = FdStatus.ACTIVE,
    @ColumnInfo(name = "previous_fd_id") val previousFdId: Long? = null,
    @ColumnInfo(name = "next_fd_id") val nextFdId: Long? = null,
    val notes: String? = null,
)

enum class InsuranceType { LIFE, HEALTH, MOTOR, OTHER }

@Entity(
    tableName = "insurance_policies",
    foreignKeys = [ForeignKey(
        entity = Instrument::class,
        parentColumns = ["id"],
        childColumns = ["instrument_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("next_due_date")]
)
data class InsurancePolicy(
    @PrimaryKey @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    @ColumnInfo(name = "insurance_type") val insuranceType: InsuranceType,
    val provider: String? = null,
    @ColumnInfo(name = "policy_name") val policyName: String? = null,
    @ColumnInfo(name = "policy_number") val policyNumber: String? = null,
    @ColumnInfo(name = "sum_assured") val sumAssured: Double? = null,
    @ColumnInfo(name = "premium_amount") val premiumAmount: Double? = null,
    @ColumnInfo(name = "premium_frequency_months") val premiumFrequencyMonths: Int? = null,
    @ColumnInfo(name = "start_date") val startDate: LocalDate? = null,
    @ColumnInfo(name = "maturity_date") val maturityDate: LocalDate? = null,
    @ColumnInfo(name = "next_due_date") val nextDueDate: LocalDate? = null,
    /** Life insurance counts as an investment in the dashboard; health does not. */
    @ColumnInfo(name = "counts_as_investment") val countsAsInvestment: Boolean = false,
)

enum class ValuationSource { YAHOO, AMFI, MANUAL, FD_FORMULA }

/** Price / value observations. The latest row per instrument is the "current" value. */
@Entity(
    tableName = "instrument_valuations",
    foreignKeys = [ForeignKey(
        entity = Instrument::class,
        parentColumns = ["id"],
        childColumns = ["instrument_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["instrument_id", "date"])]
)
data class InstrumentValuation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    val date: LocalDate,
    @ColumnInfo(name = "unit_price") val unitPrice: Double? = null,
    @ColumnInfo(name = "total_value") val totalValue: Double? = null,
    val source: ValuationSource,
    @ColumnInfo(name = "fetched_at") val fetchedAt: LocalDateTime,
)

@Entity(tableName = "fx_rates", indices = [Index(value = ["base", "quote", "date"], unique = true)])
data class FxRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val base: String,
    val quote: String,
    /** 1 base = rate quote, e.g. base USD, quote INR, rate 84.2 */
    val rate: Double,
    val source: String,
)
