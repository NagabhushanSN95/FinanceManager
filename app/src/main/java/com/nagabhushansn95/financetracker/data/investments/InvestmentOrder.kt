package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory

/** The groups the investment screens are organised in: one tile on the hub, one entry in the drawer each. */
enum class InvestmentGroup { FIXED_DEPOSITS, PROVIDENT_FUNDS, INSURANCE, STOCKS, MUTUAL_FUNDS, OTHERS }

/**
 * The order investments are listed in everywhere (hub tiles, drawer, pickers) follows the
 * order of the top-level categories under Manage › Investment categories, so one drag there moves them all.
 * A type whose default category is missing goes last, in the default order.
 */
object InvestmentOrder {
    val DEFAULT_TYPES = listOf(
        InstrumentType.FIXED_DEPOSIT,
        InstrumentType.INSURANCE,
        InstrumentType.VEHICLE,
        InstrumentType.STOCK,
        InstrumentType.MUTUAL_FUND,
        InstrumentType.OTHER,
    )

    val DEFAULT_GROUPS = listOf(
        InvestmentGroup.FIXED_DEPOSITS,
        InvestmentGroup.PROVIDENT_FUNDS,
        InvestmentGroup.INSURANCE,
        InvestmentGroup.STOCKS,
        InvestmentGroup.MUTUAL_FUNDS,
        InvestmentGroup.OTHERS,
    )

    /** Top-level category names in display order. */
    fun names(categories: List<InvestmentCategory>): List<String> =
        categories.filter { it.parentId == null }.sortedBy { it.displayOrder }.map { it.name }

    fun types(categoryNames: List<String>): List<InstrumentType> =
        DEFAULT_TYPES.withIndex().sortedWith(compareBy({ (_, t) ->
            // Vehicles sit with the policies, under the Insurance screen.
            val type = if (t == InstrumentType.VEHICLE) InstrumentType.INSURANCE else t
            rank(categoryNames, InvestmentCategories.defaultCategoryName(type))
        }, { it.index })).map { it.value }

    fun groups(categoryNames: List<String>): List<InvestmentGroup> =
        DEFAULT_GROUPS.withIndex().sortedWith(compareBy({ (_, g) ->
            when (g) {
                InvestmentGroup.FIXED_DEPOSITS -> rank(
                    categoryNames,
                    InvestmentCategories.FIXED_DEPOSITS
                )

                InvestmentGroup.PROVIDENT_FUNDS -> rank(
                    categoryNames,
                    InvestmentCategories.PROVIDENT_FUNDS
                )

                InvestmentGroup.INSURANCE -> rank(
                    categoryNames,
                    InvestmentCategories.LIFE_INSURANCE
                )

                InvestmentGroup.STOCKS -> rank(categoryNames, InvestmentCategories.STOCKS)
                InvestmentGroup.MUTUAL_FUNDS -> rank(
                    categoryNames,
                    InvestmentCategories.MUTUAL_FUNDS
                )

                InvestmentGroup.OTHERS -> rank(categoryNames, InvestmentCategories.OTHERS)
            }
        }, { it.index })).map { it.value }

    private fun rank(names: List<String>, name: String): Int =
        names.indexOfFirst { it.equals(name, ignoreCase = true) }
            .let { if (it < 0) Int.MAX_VALUE else it }
}
