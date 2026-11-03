package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.dao.Settings
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

/** One instrument with its position, as the lists show it. */
data class HoldingRow(
    val instrument: Instrument,
    val position: Position,
    val categoryName: String,
    val unitPrice: Double?,
    val priceDate: LocalDate?,
    val currentValue: Double,
    val xirr: Double?,
) {
    val heldProfit get() = currentValue - position.costHeld
}

/**
 * A line of the Investment Summary: a category (with children), a provident-fund account, or a synthetic group such as
 * a risk level. [targetPct] is derived: goal amount × [goalPct] plus the rest of the current total × [wealthPct],
 * as a share of the current total.
 */
data class SummaryLine(
    val id: Long?,                  // category id; null for synthetic lines and PF accounts
    val name: String,
    val invested: Double,
    val current: Double,
    val realised: Double,
    val income: Double,
    val xirr: Double?,
    val goalPct: Double? = null,
    val wealthPct: Double? = null,
    val targetPct: Double? = null,
    val tolerancePct: Double? = null,
    val riskLevel: String? = null,
    val notes: String? = null,
    val children: List<SummaryLine> = emptyList(),
    val cashFlows: List<CashFlow> = emptyList(),
    /** Set on provident-fund lines: the account behind the line. */
    val accountId: Long? = null,
) {
    val held get() = current - invested
    val total get() = held + realised
    fun shareOf(total: Double) = if (total > 0) current / total else 0.0

    /** Current share minus target; null when there is no target. */
    fun drift(totalCurrent: Double): Double? = targetPct?.let { shareOf(totalCurrent) - it }
    fun offTarget(totalCurrent: Double): Boolean {
        val d = drift(totalCurrent) ?: return false
        val tolerance = tolerancePct ?: return false
        return kotlin.math.abs(d) > tolerance
    }
}

data class InvestmentSummary(
    val asOf: LocalDate,
    /** Top-level categories with children, in display order; provident funds included; empty categories left out. */
    val categories: List<SummaryLine>,
    val riskLevels: List<SummaryLine>,
    val zerodha: SummaryLine,
    val liquid: SummaryLine,
    val total: SummaryLine,
    /** Stocks and funds (own, active or still holding units): the Holdings screen. */
    val holdings: List<HoldingRow>,
    /** Every instrument with a position, own and family, for the per-type screens. */
    val rows: List<HoldingRow>,
    /** Own instruments rolled up by type (deposits, policies, ...). */
    val byType: Map<InstrumentType, SummaryLine>,
    val providentFunds: SummaryLine?,
    val goalAmount: Double?,
    val latestPriceDate: LocalDate?,
) {
    fun investedShare(line: SummaryLine) =
        if (total.invested > 0) line.invested / total.invested else 0.0

    fun currentShare(line: SummaryLine) = line.shareOf(total.current)
}

object InvestmentSummaryBuilder {

    /** The summary, live: recomputed whenever instruments, categories, the ledger, prices, accounts or settings change. */
    fun observe(
        db: AppDatabase,
        asOf: () -> LocalDate = { LocalDate.now() }
    ): Flow<InvestmentSummary> {
        val data = combine(
            db.instrumentDao().observeAll(),
            db.instrumentDao().observeCategories(),
            db.transactionDao().observeAll(),
            db.instrumentDao().observeValuations()
        ) { i, c, t, v -> Quad(i, c, t, v) }
        val more = combine(
            db.accountDao().observeAll(),
            db.instrumentDao().observeRiskLevels(),
            db.settingsDao().observe(Settings.GOAL_AMOUNT)
        ) { a, r, g -> Triple(a, r, g) }
        return combine(
            data,
            more
        ) { (instruments, categories, transactions, valuations), (accounts, riskLevels, goal) ->
            build(
                instruments,
                categories,
                transactions.filter { it.instrumentId != null },
                valuations,
                accounts,
                transactions,
                riskLevels,
                goal?.toDoubleOrNull(),
                asOf()
            )
        }.flowOn(Dispatchers.Default)
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    /**
     * Rolls the ledger up into the Investment Summary. Family-owned instruments are excluded from the totals. Provident
     * funds come from the accounts: invested = contributions in minus transfers out, current = balance.
     */
    fun build(
        instruments: List<Instrument>,
        categories: List<InvestmentCategory>,
        ledgerWithInstrument: List<TransactionEntity>,
        valuations: List<InstrumentValuation>,
        accounts: List<Account>,
        allTransactions: List<TransactionEntity>,
        riskLevels: List<RiskLevel> = emptyList(),
        goalAmount: Double? = null,
        asOf: LocalDate = LocalDate.now(),
    ): InvestmentSummary {
        val unitBasedIds =
            instruments.filter { it.instrumentType == InstrumentType.STOCK || it.instrumentType == InstrumentType.MUTUAL_FUND }
                .map { it.id }.toSet()
        val positions = Holdings.positions(ledgerWithInstrument) { it in unitBasedIds }
        val latest = valuations.groupBy { it.instrumentId }
            .mapValues { (_, v) -> v.maxWith(compareBy({ it.date }, { it.id })) }
        val byId = categories.associateBy { it.id }
        val byName = categories.filter { it.parentId == null }.associateBy { it.name }
        val riskName = riskLevels.associate { it.id to it.name }

        val rows = instruments.mapNotNull { instrument ->
            val position = positions[instrument.id] ?: return@mapNotNull null
            val valuation = latest[instrument.id]
            val category = instrument.categoryId?.let { byId[it] }
            val categoryName = category?.let { c ->
                c.parentId?.let { byId[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
            }
                ?: InvestmentCategories.defaultCategoryName(instrument.instrumentType)
            val current = position.currentValue(valuation?.unitPrice, valuation?.totalValue)
            HoldingRow(
                instrument = instrument,
                position = position,
                categoryName = categoryName,
                unitPrice = valuation?.unitPrice,
                priceDate = valuation?.date,
                currentValue = current,
                xirr = Holdings.xirr(position.cashFlows, current, asOf),
            )
        }
        val own = rows.filter { it.instrument.owner == Instrument.OWNER_SELF }

        // Which top-level category each holding belongs to, and which child (if any).
        fun topOf(h: HoldingRow): InvestmentCategory? {
            val c = h.instrument.categoryId?.let { byId[it] }
                ?: return byName[InvestmentCategories.defaultCategoryName(h.instrument.instrumentType)]
            return c.parentId?.let { byId[it] } ?: c
        }

        fun childOf(h: HoldingRow): InvestmentCategory? =
            h.instrument.categoryId?.let { byId[it] }?.takeIf { it.parentId != null }

        fun riskOf(h: HoldingRow): Long? = childOf(h)?.riskLevelId ?: topOf(h)?.riskLevelId

        fun lineFor(
            id: Long?,
            name: String,
            rows: List<HoldingRow>,
            cat: InvestmentCategory?,
            children: List<SummaryLine> = emptyList()
        ): SummaryLine {
            val flows = rows.flatMap { it.position.cashFlows }
            val current = rows.sumOf { it.currentValue }
            return SummaryLine(
                id = id,
                name = name,
                invested = rows.sumOf { it.position.costHeld },
                current = current,
                realised = rows.sumOf { it.position.realised },
                income = rows.sumOf { it.position.income },
                xirr = Holdings.xirr(flows, current, asOf),
                goalPct = cat?.targetGoalPct,
                wealthPct = cat?.targetWealthPct,
                tolerancePct = cat?.tolerancePct,
                riskLevel = cat?.riskLevelId?.let { riskName[it] },
                notes = cat?.rebalanceNotes,
                children = children,
                cashFlows = flows,
            )
        }

        fun group(
            name: String,
            lines: List<SummaryLine>,
            cat: InvestmentCategory? = null,
            id: Long? = null
        ) = SummaryLine(
            id = id,
            name = name,
            invested = lines.sumOf { it.invested },
            current = lines.sumOf { it.current },
            realised = lines.sumOf { it.realised },
            income = lines.sumOf { it.income },
            xirr = Holdings.xirr(lines.flatMap { it.cashFlows }, lines.sumOf { it.current }, asOf),
            goalPct = cat?.targetGoalPct,
            wealthPct = cat?.targetWealthPct,
            tolerancePct = cat?.tolerancePct,
            riskLevel = cat?.riskLevelId?.let { riskName[it] },
            notes = cat?.rebalanceNotes,
            children = lines,
            cashFlows = lines.flatMap { it.cashFlows },
        )

        // Provident funds, from the accounts.
        val pfAccounts =
            accounts.filter { it.accountType == AccountType.PROVIDENT_FUND && !it.deleted }
        val pfLines = pfAccounts.map { pf ->
            val rows =
                allTransactions.filter { it.toAccountId == pf.id || it.fromAccountId == pf.id }
            val contributions =
                rows.filter { it.toAccountId == pf.id && it.kind == TransactionKind.INCOME && it.contributor != "Interest" }
                    .sumOf { it.amountInr } +
                        rows.filter { it.toAccountId == pf.id && it.kind == TransactionKind.TRANSFER }
                            .sumOf { it.amountInr } -
                        rows.filter { it.fromAccountId == pf.id && it.kind == TransactionKind.TRANSFER }
                            .sumOf { it.amountInr }
            val interest =
                rows.filter { it.toAccountId == pf.id && it.kind == TransactionKind.INCOME && it.contributor == "Interest" }
                    .sumOf { it.amountInr }
            val flows = rows.mapNotNull { t ->
                when {
                    t.toAccountId == pf.id && (t.kind == TransactionKind.INCOME && t.contributor != "Interest" || t.kind == TransactionKind.TRANSFER) -> CashFlow(
                        t.date,
                        -t.amountInr
                    )

                    t.fromAccountId == pf.id && t.kind == TransactionKind.TRANSFER -> CashFlow(
                        t.date,
                        t.amountInr
                    )

                    else -> null
                }
            }
            SummaryLine(
                id = null,
                name = pf.name,
                invested = contributions,
                current = pf.balance,
                realised = 0.0,
                income = interest,
                xirr = Holdings.xirr(flows, pf.balance, asOf),
                cashFlows = flows,
                accountId = pf.id
            )
        }
        val pfCategory = byName[InvestmentCategories.PROVIDENT_FUNDS]
        val pfLine = if (pfLines.isEmpty()) null else group(
            InvestmentCategories.PROVIDENT_FUNDS,
            pfLines,
            pfCategory,
            pfCategory?.id
        )

        val topLevel = categories.filter { it.parentId == null }
        val categoryLines = ArrayList<SummaryLine>()
        for (top in topLevel) {
            if (top.id == pfCategory?.id) {
                pfLine?.let { categoryLines += it }; continue
            }
            val rows = own.filter { topOf(it)?.id == top.id }
            val childLines = categories.filter { it.parentId == top.id }.map { child ->
                lineFor(child.id, child.name, rows.filter { childOf(it)?.id == child.id }, child)
            }
                .filter { it.invested != 0.0 || it.current != 0.0 || it.realised != 0.0 || it.goalPct != null || it.wealthPct != null }
            val uncategorised = rows.filter { childOf(it) == null }
            val children = if (childLines.isNotEmpty() && uncategorised.isNotEmpty())
                childLines + lineFor(null, "Unassigned", uncategorised, null) else childLines
            // Targets entered on the children roll up to the parent; a childless parent carries its own.
            val line = lineFor(top.id, top.name, rows, top, children).let { l ->
                val typed = childLines.filter { it.id != null }
                if (typed.any { it.goalPct != null || it.wealthPct != null })
                    l.copy(
                        goalPct = typed.sumOf { it.goalPct ?: 0.0 },
                        wealthPct = typed.sumOf { it.wealthPct ?: 0.0 }) else l
            }
            if (line.invested != 0.0 || line.current != 0.0 || line.realised != 0.0) categoryLines += line
        }
        if (pfLine != null && pfCategory == null) categoryLines += pfLine
        // Instruments whose category tree is missing entirely (no seed): one line per type.
        val orphanRows = own.filter { topOf(it) == null }
        orphanRows.groupBy { it.instrument.instrumentType }.forEach { (type, rows) ->
            categoryLines += lineFor(
                null,
                InvestmentCategories.defaultCategoryName(type),
                rows,
                null
            )
        }

        val totalRaw = group("Total", categoryLines)
        val totalCurrent = totalRaw.current
        val goal = goalAmount?.coerceIn(0.0, totalCurrent.coerceAtLeast(0.0)) ?: 0.0
        val wealth = totalCurrent - goal
        fun withTargets(line: SummaryLine): SummaryLine {
            val target =
                if (line.goalPct == null && line.wealthPct == null || totalCurrent <= 0) null
                else (goal * (line.goalPct ?: 0.0) + wealth * (line.wealthPct
                    ?: 0.0)) / totalCurrent
            return line.copy(targetPct = target, children = line.children.map(::withTargets))
        }

        val finalCategories = categoryLines.map(::withTargets)
        val total =
            totalRaw.copy(targetPct = if (totalCurrent > 0) 1.0 else null, children = emptyList())

        // Risk levels: by the finest category that has one; PF by its category.
        val riskLines = ArrayList<SummaryLine>()
        for (level in riskLevels) {
            val lines = listOfNotNull(
                lineFor(
                    null,
                    level.name,
                    own.filter { riskOf(it) == level.id },
                    null
                )
            ) +
                    listOfNotNull(pfLine?.takeIf { pfCategory?.riskLevelId == level.id })
            riskLines += group(level.name, lines)
        }
        val unrated = own.filter { riskOf(it) == null }
        val pfUnrated = pfLine?.takeIf { pfCategory?.riskLevelId == null }
        if (unrated.isNotEmpty() || pfUnrated != null) {
            riskLines += group(
                "No risk level set",
                listOfNotNull(lineFor(null, "", unrated, null), pfUnrated)
            )
        }

        val zerodha = lineFor(
            null,
            "In Zerodha",
            own.filter { it.instrument.instrumentType == InstrumentType.STOCK || it.instrument.instrumentType == InstrumentType.MUTUAL_FUND },
            null
        )
        val liquid = lineFor(null, "Liquid (deposits, funds, stocks)", own.filter {
            it.instrument.instrumentType == InstrumentType.STOCK || it.instrument.instrumentType == InstrumentType.MUTUAL_FUND || it.instrument.instrumentType == InstrumentType.FIXED_DEPOSIT
        }, null)
        val byType = InstrumentType.entries.associateWith { type ->
            lineFor(
                null,
                InvestmentCategories.defaultCategoryName(type),
                own.filter { it.instrument.instrumentType == type },
                null
            )
        }

        return InvestmentSummary(
            asOf = asOf,
            categories = finalCategories,
            riskLevels = riskLines.map { it.copy(children = emptyList()) },
            zerodha = zerodha,
            liquid = liquid,
            total = total,
            // The Holdings list is stocks and funds; deposits, policies and others have their own screens.
            holdings = own.filter { it.instrument.id in unitBasedIds && (it.instrument.active || it.position.units != 0.0) }
                .sortedWith(
                    compareBy(
                        { it.instrument.instrumentType },
                        { it.categoryName },
                        { it.instrument.displayOrder })
                ),
            rows = rows,
            byType = byType,
            providentFunds = pfLine?.let(::withTargets),
            goalAmount = goalAmount,
            latestPriceDate = latest.values.maxOfOrNull { it.date },
        )
    }
}
