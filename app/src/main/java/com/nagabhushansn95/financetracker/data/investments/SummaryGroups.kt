package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.SummaryGroup

/**
 * One thing a summary group may hold: a kind of instrument (every holding of the kind, future ones included), the
 * provident funds, an investment category (likewise), or one instrument.
 */
sealed class GroupMember {
    data class Type(val type: InstrumentType) : GroupMember()
    object ProvidentFunds : GroupMember()
    data class Category(val id: Long) : GroupMember()
    data class Instrument(val id: Long) : GroupMember()

    val token: String
        get() = when (this) {
            is Type -> "type:${type.name}"
            ProvidentFunds -> "pf"
            is Category -> "category:$id"
            is Instrument -> "instrument:$id"
        }
}

object SummaryGroups {
    /** The kinds a group may name, with their labels; Others stays out (it is outside the summary). */
    val TYPES: List<Pair<GroupMember, String>> = listOf(
        GroupMember.Type(InstrumentType.FIXED_DEPOSIT) to "Fixed deposits",
        GroupMember.ProvidentFunds to "Provident funds",
        GroupMember.Type(InstrumentType.INSURANCE) to "Life insurance",
        GroupMember.Type(InstrumentType.STOCK) to "Stocks & ETFs",
        GroupMember.Type(InstrumentType.MUTUAL_FUND) to "Mutual funds",
    )

    /** The two groups the sheet always had, as the app starts with them. */
    val DEFAULTS: List<SummaryGroup> = listOf(
        SummaryGroup(
            name = "Liquid",
            displayOrder = 1,
            members = format(
                listOf(
                    GroupMember.Type(InstrumentType.FIXED_DEPOSIT),
                    GroupMember.Type(InstrumentType.MUTUAL_FUND),
                    GroupMember.Type(InstrumentType.STOCK)
                )
            )
        ),
        SummaryGroup(
            name = "In Zerodha",
            displayOrder = 2,
            members = format(
                listOf(
                    GroupMember.Type(InstrumentType.STOCK),
                    GroupMember.Type(InstrumentType.MUTUAL_FUND)
                )
            )
        ),
    )

    fun format(members: Collection<GroupMember>): String = members.joinToString(",") { it.token }

    fun parse(members: String): List<GroupMember> = members.split(',').mapNotNull { raw ->
        val t = raw.trim()
        when {
            t == "pf" -> GroupMember.ProvidentFunds
            t.startsWith("type:") -> runCatching { InstrumentType.valueOf(t.removePrefix("type:")) }.getOrNull()
                ?.let { GroupMember.Type(it) }

            t.startsWith("category:") -> t.removePrefix("category:").toLongOrNull()
                ?.let { GroupMember.Category(it) }

            t.startsWith("instrument:") -> t.removePrefix("instrument:").toLongOrNull()
                ?.let { GroupMember.Instrument(it) }

            else -> null
        }
    }

    fun includesPf(group: SummaryGroup): Boolean =
        parse(group.members).any { it is GroupMember.ProvidentFunds }

    /**
     * Whether a holding is in the group: it is named itself, its kind is a member, or its category or the category's
     * group is. [categoriesById] resolves a category to its parent.
     */
    fun includes(
        group: SummaryGroup,
        row: HoldingRow,
        categoriesById: Map<Long, InvestmentCategory>
    ): Boolean =
        includes(parse(group.members), row.instrument, categoriesById)

    fun includes(
        members: List<GroupMember>,
        instrument: com.nagabhushansn95.financetracker.data.db.entity.Instrument,
        categoriesById: Map<Long, InvestmentCategory>
    ): Boolean {
        if (members.any { it is GroupMember.Instrument && it.id == instrument.id }) return true
        if (members.any { it is GroupMember.Type && it.type == instrument.instrumentType }) return true
        val category = instrument.categoryId?.let { categoriesById[it] } ?: return false
        val ids = setOfNotNull(category.id, category.parentId)
        return members.any { it is GroupMember.Category && it.id in ids }
    }

    /** What a group holds, in words, for its row under Manage. */
    fun describe(
        group: SummaryGroup,
        categoriesById: Map<Long, InvestmentCategory>,
        instrumentsById: Map<Long, com.nagabhushansn95.financetracker.data.db.entity.Instrument> = emptyMap(),
        securitiesById: Map<Long, com.nagabhushansn95.financetracker.data.db.entity.Security> = emptyMap(),
    ): String {
        val members = parse(group.members)
        val kinds = TYPES.filter { (m, _) -> m in members }.map { it.second }
        val categories = members.filterIsInstance<GroupMember.Category>().mapNotNull { m ->
            categoriesById[m.id]?.let { c ->
                c.parentId?.let { categoriesById[it]?.name }?.let { "$it › ${c.name}" } ?: c.name
            }
        }
        val instruments = members.filterIsInstance<GroupMember.Instrument>().mapNotNull { m ->
            instrumentsById[m.id]?.let { securitiesById[it.id]?.tickerOrNull ?: it.name }
        }
        return (kinds + categories + instruments).joinToString(", ").ifEmpty { "Nothing yet" }
    }

    /**
     * Seeds "Liquid" and "In Zerodha" into a database that has no groups and no instruments yet (a fresh one); a user
     * who deletes them later is not given them again, since by then something is invested.
     */
    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.summaryGroupDao().count() > 0 || db.instrumentDao().getAll().isNotEmpty()) return
        DEFAULTS.forEach { db.summaryGroupDao().insert(it) }
    }
}
