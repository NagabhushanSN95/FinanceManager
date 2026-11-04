package com.nagabhushansn95.financetracker.data.export

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.dao.Settings
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.FixedDeposits
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import com.nagabhushansn95.financetracker.data.investments.Savings
import com.nagabhushansn95.financetracker.data.investments.SavingsPeriod
import com.nagabhushansn95.financetracker.data.investments.SummaryLine
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The "My Savings" workbook as values: Savings Yearly, Savings Monthly, Investment Summary, Fixed Deposits,
 * Provident Funds, Insurances, Insurance Policies, Mutual Funds Summary, Stocks Summary, Others, Prices.
 * Everything is computed from the ledger the same way the screens do it.
 */
class SummaryWorkbook(private val db: AppDatabase) {

    fun suggestedFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "Finance Tracker Summary - ${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm"))}.xlsx"

    suspend fun write(out: OutputStream, asOf: LocalDate = LocalDate.now()) {
        val instruments = db.instrumentDao().getAll()
        val categories = db.instrumentDao().getAllCategories()
        val transactions = db.transactionDao().getAll()
        val withInstrument = transactions.filter { it.instrumentId != null }
        val valuations = db.instrumentDao().getAllValuationsLatestFirst()
        val accounts = db.accountDao().getAll()
        val riskLevels = db.instrumentDao().getAllRiskLevels()
        val goal = db.settingsDao().get(Settings.GOAL_AMOUNT)?.toDoubleOrNull()
        val fixedDeposits = db.instrumentDao().getAllFixedDeposits()
        val fx = InvestmentSummaryBuilder.latestFx(db.instrumentDao().getAllFxRates())
        val summary = InvestmentSummaryBuilder.build(
            instruments,
            categories,
            withInstrument,
            valuations,
            accounts,
            transactions,
            riskLevels,
            goal,
            asOf,
            fixedDeposits,
            fx
        )
        val instrumentById = instruments.associateBy { it.id }
        val accountById = accounts.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }

        val xlsx = XlsxWriter()
        savingsSheet(
            xlsx,
            "Savings Yearly",
            "Year",
            Savings.build(transactions, instruments, categories, accounts, monthly = false),
            categories.filter { it.parentId == null }.map { it.name })
        savingsSheet(
            xlsx,
            "Savings Monthly",
            "Month",
            Savings.build(transactions, instruments, categories, accounts, monthly = true),
            categories.filter { it.parentId == null }.map { it.name })

        xlsx.sheet(
            "Investment Summary",
            listOf(
                "Category",
                "Invested",
                "Current",
                "Held P/L",
                "Realised",
                "Income",
                "XIRR",
                "% Invested",
                "% Current",
                "Goal %",
                "Wealth %",
                "Target %",
                "Tolerance %",
                "Drift",
                "Risk level",
                "Notes"
            ),
            listOf(36, 14, 14, 14, 14, 14, 9, 10, 10, 9, 9, 9, 11, 9, 14, 40),
        ).also { sheet ->
            fun line(l: SummaryLine, indent: String) {
                sheet.row(
                    indent + l.name,
                    l.invested,
                    l.current,
                    l.held,
                    l.realised,
                    l.income,
                    l.xirr,
                    summary.investedShare(l),
                    summary.currentShare(l),
                    l.goalPct,
                    l.wealthPct,
                    l.targetPct,
                    l.tolerancePct,
                    l.drift(summary.total.current),
                    l.riskLevel,
                    l.notes,
                )
                l.children.forEach { line(it, "$indent    ") }
            }
            summary.categories.forEach { line(it, "") }
            summary.providentFunds?.takeIf { pf -> summary.categories.none { it.name == InvestmentCategories.PROVIDENT_FUNDS } }
                ?.let { line(it, "") }
            sheet.row()
            summary.riskLevels.forEach { line(it, "") }
            sheet.row()
            line(summary.zerodha, ""); line(summary.liquid, ""); line(summary.total, "")
            sheet.row()
            sheet.row("Goal amount", goal); sheet.row("As of", asOf); sheet.row(
            "Prices as of",
            summary.latestPriceDate
        )
        }

        xlsx.sheet(
            "Fixed Deposits",
            listOf(
                "ID",
                "Name",
                "Certificate No",
                "Holder",
                "Bank",
                "Branch",
                "Account No",
                "Customer ID",
                "Deposit Amount",
                "Deposit Date",
                "Period (Months)",
                "Period (Days)",
                "Rate",
                "Compounding (Months)",
                "Maturity Date",
                "Current Amount (Estimated)",
                "Maturity Amount (Calculated)",
                "TDS Rate",
                "TDS (Calculated)",
                "Maturity Amount (After TDS)",
                "Maturity Amount (Printed)",
                "TDS Reported",
                "Amount Received",
                "Interest Earned",
                "Status",
                "Owner",
                "Previous FD",
                "Next FD",
                "Notes"
            ),
        ).also { sheet ->
            FixedDeposits.build(instruments, fixedDeposits, withInstrument, valuations, asOf)
                .sortedBy { it.instrument.id }.forEach { d ->
                val t = d.terms
                sheet.row(
                    d.instrument.id,
                    d.instrument.name,
                    t?.certificateNo,
                    t?.holderName,
                    t?.bankName,
                    t?.branch,
                    t?.bankAccountNo,
                    t?.custId,
                    d.principal.takeIf { it > 0 } ?: d.position.totalInvested,
                    d.depositDate,
                    t?.periodMonths,
                    t?.periodDays,
                    t?.rate,
                    t?.compoundingMonths,
                    d.maturityDate,
                    if (d.isOpen) d.estimated else null,
                    d.maturityAmount,
                    t?.tdsRate?.let { if (it > 1) it / 100 else it },
                    d.tdsAtMaturity.takeIf { it > 0 },
                    d.maturityAfterTds,
                    t?.maturityAmountPrinted,
                    t?.tdsReported,
                    d.position.totalRedeemed.takeIf { it > 0 },
                    if (!d.isOpen) d.position.realised else null,
                    d.status.name,
                    d.instrument.owner,
                    t?.previousFdId?.let { instrumentById[it]?.name },
                    t?.nextFdId?.let { instrumentById[it]?.name },
                    d.instrument.notes ?: t?.notes,
                )
            }
        }

        val pfIds =
            accounts.filter { it.accountType == AccountType.PROVIDENT_FUND }.map { it.id }.toSet()
        xlsx.sheet(
            "Provident Funds",
            listOf(
                "Date",
                "Provident Fund",
                "Contributor",
                "Credit Debit Type",
                "Amount",
                "Particulars"
            ),
            listOf(12, 18, 12, 16, 14, 40)
        ).also { sheet ->
            transactions.filter { it.toAccountId in pfIds || it.fromAccountId in pfIds }
                .sortedWith(compareBy({ it.date }, { it.id })).forEach { t ->
                val credit = t.toAccountId in pfIds
                sheet.row(
                    t.date,
                    accountById[if (credit) t.toAccountId else t.fromAccountId]?.name,
                    t.contributor ?: if (t.kind == TransactionKind.TRANSFER) "Self" else null,
                    if (credit) "Credit" else "Debit",
                    t.amountInr,
                    t.particulars
                )
            }
        }

        val policies = db.instrumentDao().getAllInsurancePolicies().associateBy { it.instrumentId }
        xlsx.sheet(
            "Insurances",
            listOf(
                "Date",
                "Insurance Type",
                "Provider",
                "Insurance Name",
                "Policy Number",
                "Transaction Type",
                "Amount",
                "Particulars"
            ),
            listOf(12, 14, 18, 26, 20, 16, 14, 40)
        ).also { sheet ->
            withInstrument.filter { instrumentById[it.instrumentId]?.instrumentType == InstrumentType.INSURANCE }
                .sortedWith(compareBy({ it.date }, { it.id })).forEach { t ->
                val p = policies[t.instrumentId]
                sheet.row(
                    t.date,
                    p?.insuranceType?.name?.lowercase()?.replaceFirstChar { it.uppercase() },
                    p?.provider,
                    p?.policyName ?: instrumentById[t.instrumentId]?.name,
                    p?.policyNumber,
                    if (t.kind == TransactionKind.INVEST || t.kind == TransactionKind.EXPENSE) "Premium" else "Payout",
                    t.amountInr,
                    t.particulars,
                )
            }
        }
        xlsx.sheet(
            "Insurance Policies",
            listOf(
                "Name",
                "Type",
                "Provider",
                "Policy Name",
                "Policy Number",
                "Sum Assured",
                "Premium",
                "Frequency (Months)",
                "Start",
                "Maturity",
                "Next Due",
                "Counts As Investment",
                "Active",
                "Owner"
            )
        ).also { sheet ->
            instruments.filter { it.instrumentType == InstrumentType.INSURANCE }.sortedBy { it.id }
                .forEach { i ->
                    val p = policies[i.id]
                    sheet.row(
                        i.name,
                        p?.insuranceType?.name,
                        p?.provider,
                        p?.policyName,
                        p?.policyNumber,
                        p?.sumAssured,
                        p?.premiumAmount,
                        p?.premiumFrequencyMonths,
                        p?.startDate,
                        p?.maturityDate,
                        p?.nextDueDate,
                        p?.countsAsInvestment ?: (p?.insuranceType == null),
                        i.active,
                        i.owner
                    )
                }
        }

        for ((title, type) in listOf(
            "Mutual Funds Summary" to InstrumentType.MUTUAL_FUND,
            "Stocks Summary" to InstrumentType.STOCK
        )) {
            xlsx.sheet(
                title,
                listOf(
                    "Name",
                    if (type == InstrumentType.STOCK) "Symbol" else "Scheme Code",
                    "ISIN",
                    "Category",
                    "Currency",
                    "Units",
                    "Average Cost",
                    "Price",
                    "Price Date",
                    "Invested",
                    "Current",
                    "Held P/L",
                    "Realised",
                    if (type == InstrumentType.STOCK) "Dividends" else "Income",
                    "XIRR",
                    "First",
                    "Last",
                    "Total Put In",
                    "Total Taken Out",
                    "Active",
                    "Owner"
                ),
                listOf(
                    40,
                    14,
                    16,
                    22,
                    9,
                    12,
                    12,
                    12,
                    12,
                    14,
                    14,
                    14,
                    14,
                    12,
                    9,
                    12,
                    12,
                    14,
                    14,
                    8,
                    10
                ),
            ).also { sheet ->
                summary.rows.filter { it.instrument.instrumentType == type }.sortedWith(
                    compareBy(
                        { !it.instrument.active },
                        { it.categoryName },
                        { it.instrument.name })
                ).forEach { h ->
                    val p = h.position
                    sheet.row(
                        h.instrument.name,
                        h.instrument.symbol,
                        h.instrument.isin,
                        h.categoryName,
                        h.instrument.currency,
                        p.units.takeIf { p.hasUnits },
                        p.averageCost,
                        h.unitPrice,
                        h.priceDate,
                        p.costHeld,
                        h.currentValue,
                        h.heldProfit,
                        p.realised,
                        p.income,
                        h.xirr,
                        p.firstDate,
                        p.lastDate,
                        p.totalInvested,
                        p.totalRedeemed,
                        h.instrument.active,
                        h.instrument.owner,
                    )
                }
            }
        }

        xlsx.sheet(
            "Others",
            listOf("Date", "Instrument", "Kind", "Amount", "Account", "Particulars"),
            listOf(12, 30, 10, 14, 20, 40)
        ).also { sheet ->
            withInstrument.filter { instrumentById[it.instrumentId]?.instrumentType == InstrumentType.OTHER }
                .sortedWith(compareBy({ it.date }, { it.id })).forEach { t ->
                sheet.row(
                    t.date,
                    instrumentById[t.instrumentId]?.name,
                    t.kind.name,
                    t.amountInr,
                    accountById[t.fromAccountId ?: t.toAccountId]?.name,
                    t.particulars
                )
            }
        }

        xlsx.sheet(
            "Prices",
            listOf("Instrument", "Symbol", "Date", "Unit Price", "Total Value", "Source", "Fetched")
        ).also { sheet ->
            valuations.sortedWith(compareBy({ instrumentById[it.instrumentId]?.name }, { it.date }))
                .forEach { v ->
                    sheet.row(
                        instrumentById[v.instrumentId]?.name,
                        instrumentById[v.instrumentId]?.symbol,
                        v.date,
                        v.unitPrice,
                        v.totalValue,
                        v.source.name,
                        v.fetchedAt.toLocalDate()
                    )
                }
        }
        xlsx.write(out)
    }

    private fun savingsSheet(
        xlsx: XlsxWriter,
        title: String,
        periodLabel: String,
        periods: List<SavingsPeriod>,
        categoryOrder: List<String>
    ) {
        val names = (categoryOrder + periods.flatMap { p -> p.lines.map { it.name } }).distinct()
            .filter { n -> periods.any { p -> p.lines.any { it.name == n } } }
        val sheet = xlsx.sheet(
            title,
            listOf(
                periodLabel,
                "Total Savings"
            ) + names.map { "$it Savings" } + names.flatMap { listOf("$it Credits", "$it Debits") })
        periods.forEach { p ->
            val byName = p.lines.associateBy { it.name }
            sheet.row(listOf(p.key, p.total) + names.map {
                byName[it]?.net ?: 0.0
            } + names.flatMap { listOf(byName[it]?.credits ?: 0.0, byName[it]?.debits ?: 0.0) })
        }
    }
}
