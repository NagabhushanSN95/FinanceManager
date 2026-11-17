package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel

/**
 * The category tree of the Investment Summary sheet (names and risk levels only; targets are personal and are filled
 * in by the user), and the default risk levels. Seeded once, when the tables are empty.
 */
object InvestmentCategories {
    const val FIXED_DEPOSITS = "Fixed Deposits"
    const val MUTUAL_FUNDS = "Mutual Funds"
    const val STOCKS = "Stocks"

    /** Provident funds are accounts, not instruments; this category carries their targets and risk level. */
    const val PROVIDENT_FUNDS = "Provident Funds"
    const val LIFE_INSURANCE = "Life Insurance"
    const val OTHERS = "Others"

    /** Vehicles have no category of their own: they are protection, outside the summary. */
    const val VEHICLES = "Vehicles"

    /** Index-tracking equity ETFs (NIFTYBEES, JUNIORBEES, ...): the sheet called the sub-category just "ETF". */
    const val EQUITY_INDEX_ETF = "Equity Index ETF"

    const val NO_RISK = "No risk"
    const val LOW_RISK = "Low risk"
    const val MEDIUM_RISK = "Medium risk"
    const val HIGH_RISK = "High risk"
    val DEFAULT_RISK_LEVELS = listOf(NO_RISK, LOW_RISK, MEDIUM_RISK, HIGH_RISK)

    private data class Seed(
        val name: String,
        val risk: String,
        val children: List<Pair<String, String>> = emptyList()
    )

    private val TREE = listOf(
        Seed(FIXED_DEPOSITS, NO_RISK),
        Seed(
            MUTUAL_FUNDS, MEDIUM_RISK, listOf(
                "Arbitrage" to NO_RISK,
                "Large Cap" to LOW_RISK,
                "Flexi Cap" to LOW_RISK,
                "Focused" to MEDIUM_RISK,
                "Value Discovery" to MEDIUM_RISK,
                "Mid Cap" to MEDIUM_RISK,
                "Foreign Equity" to MEDIUM_RISK,
                "Small Cap" to HIGH_RISK,
            )
        ),
        Seed(
            STOCKS, MEDIUM_RISK, listOf(
                "Debt ETF" to NO_RISK,
                "Gold ETF" to LOW_RISK,
                EQUITY_INDEX_ETF to MEDIUM_RISK,
                "Foreign ETF" to MEDIUM_RISK,
                "REIT" to MEDIUM_RISK,
                "Silver ETF" to MEDIUM_RISK,
                "Equity" to HIGH_RISK,
            )
        ),
        Seed(PROVIDENT_FUNDS, NO_RISK),
        Seed(LIFE_INSURANCE, NO_RISK),
        Seed(OTHERS, NO_RISK),
    )

    /** The top-level category an instrument falls under when it has none of its own. */
    fun defaultCategoryName(type: InstrumentType): String = when (type) {
        InstrumentType.FIXED_DEPOSIT -> FIXED_DEPOSITS
        InstrumentType.MUTUAL_FUND -> MUTUAL_FUNDS
        InstrumentType.STOCK -> STOCKS
        InstrumentType.INSURANCE -> LIFE_INSURANCE
        InstrumentType.VEHICLE -> VEHICLES
        InstrumentType.OTHER -> OTHERS
    }

    /** Seeds the risk levels and the category tree where each is empty. Returns true when the tree was seeded. */
    suspend fun seedIfEmpty(db: AppDatabase): Boolean {
        val dao = db.instrumentDao()
        if (dao.riskLevelCount() == 0) {
            DEFAULT_RISK_LEVELS.forEachIndexed { i, name ->
                dao.insertRiskLevel(
                    RiskLevel(
                        name = name,
                        displayOrder = i + 1
                    )
                )
            }
        }
        if (dao.categoryCount() > 0) return false
        val riskByName = dao.getAllRiskLevels().associate { it.name to it.id }
        var order = 1
        for (seed in TREE) {
            val parentId = dao.insertCategory(
                InvestmentCategory(
                    name = seed.name,
                    riskLevelId = riskByName[seed.risk],
                    displayOrder = order++
                )
            )
            for ((name, risk) in seed.children) {
                dao.insertCategory(
                    InvestmentCategory(
                        name = name,
                        parentId = parentId,
                        riskLevelId = riskByName[risk],
                        displayOrder = order++
                    )
                )
            }
        }
        return true
    }
}
