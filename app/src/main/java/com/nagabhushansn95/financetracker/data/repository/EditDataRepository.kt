package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.Converters
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel
import com.nagabhushansn95.financetracker.data.db.entity.Setting

import java.time.LocalDateTime

/**
 * Add / edit / soft-delete / restore / reorder for accounts, categories and instruments (plan 3.8):
 * `display_order` is rewritten for the moved group, deleted rows keep their value, restored rows go to the end.
 */
class EditDataRepository(private val db: AppDatabase) {

    // ------------------------------------------------------------------ accounts

    suspend fun addAccount(account: Account, now: LocalDateTime = LocalDateTime.now()): Long =
        db.accountDao().insert(
            account.copy(
                displayOrder = db.accountDao().maxDisplayOrder() + 1,
                createdTime = now,
                modifiedTime = now
            )
        )

    suspend fun updateAccount(account: Account, now: LocalDateTime = LocalDateTime.now()) =
        db.accountDao().update(account.copy(modifiedTime = now))

    suspend fun deleteAccount(id: Long, now: LocalDateTime = LocalDateTime.now()) =
        db.accountDao().setDeleted(id, true, stamp(now))

    suspend fun restoreAccount(id: Long, now: LocalDateTime = LocalDateTime.now()) = write {
        db.accountDao().setDisplayOrder(id, db.accountDao().maxDisplayOrder() + 1)
        db.accountDao().setDeleted(id, false, stamp(now))
    }

    /** [orderedIds] is the new order of one visible group; they receive consecutive values from the group's minimum. */
    suspend fun reorderAccounts(orderedIds: List<Long>) = write {
        val current = db.accountDao().getAll().filter { it.id in orderedIds }
        val base = current.minOfOrNull { it.displayOrder } ?: 1
        orderedIds.forEachIndexed { i, id -> db.accountDao().setDisplayOrder(id, base + i) }
    }

    suspend fun accountTransactionCount(id: Long) = db.accountDao().transactionCount(id)

    // ------------------------------------------------------------------ categories

    suspend fun addCategory(category: Category): Long =
        db.categoryDao()
            .insert(category.copy(displayOrder = db.categoryDao().maxDisplayOrder() + 1))

    suspend fun updateCategory(category: Category) = db.categoryDao().update(category)

    /** A parent with visible children cannot be deleted; move or delete the children first. */
    suspend fun deleteCategory(id: Long) {
        require(db.categoryDao().childCount(id) == 0) { "Delete or move its sub-categories first" }
        db.categoryDao().setDeleted(id, true)
    }

    suspend fun restoreCategory(id: Long) = write {
        db.categoryDao().setDisplayOrder(id, db.categoryDao().maxDisplayOrder() + 1)
        db.categoryDao().setDeleted(id, false)
    }

    suspend fun reorderCategories(orderedIds: List<Long>) = write {
        val current = db.categoryDao().getAll().filter { it.id in orderedIds }
        val base = current.minOfOrNull { it.displayOrder } ?: 1
        orderedIds.forEachIndexed { i, id -> db.categoryDao().setDisplayOrder(id, base + i) }
    }

    suspend fun categoryTransactionCount(id: Long) = db.categoryDao().transactionCount(id)

    // ------------------------------------------------------------------ instruments

    suspend fun addInstrument(
        instrument: Instrument,
        now: LocalDateTime = LocalDateTime.now()
    ): Long =
        db.instrumentDao().insert(instrument.copy(createdTime = now, modifiedTime = now))

    suspend fun updateInstrument(instrument: Instrument, now: LocalDateTime = LocalDateTime.now()) =
        db.instrumentDao().update(instrument.copy(modifiedTime = now))

    /**
     * Moves every transaction, event and pending dividend of [fromId] onto [intoId] and deletes [fromId] (its
     * listing and deposit/policy details go with it). For a renamed stock: merge the old symbol into the new one, so
     * the position carries over and the sale of the new symbol closes the purchase of the old. Same type required.
     */
    suspend fun mergeInstruments(
        fromId: Long,
        intoId: Long,
        now: LocalDateTime = LocalDateTime.now()
    ) = write {
        require(fromId != intoId) { "Pick a different instrument" }
        val from = requireNotNull(db.instrumentDao().getById(fromId)) { "Instrument not found" }
        val into = requireNotNull(db.instrumentDao().getById(intoId)) { "Instrument not found" }
        require(from.instrumentType == into.instrumentType) { "Both must be the same type" }
        val stamp = stamp(now)
        db.transactionDao().reassignInstrument(fromId, intoId, stamp)
        db.holdingEventDao().reassignInstrument(fromId, intoId)
        db.pendingDividendDao().reassignInstrument(fromId, intoId)
        db.instrumentDao().reassignPreviousFd(fromId, intoId)
        db.instrumentDao().reassignNextFd(fromId, intoId)
        val fromSecurity = db.instrumentDao().getSecurity(fromId)
        val intoSecurity = db.instrumentDao().getSecurity(intoId)
        if (fromSecurity != null && intoSecurity != null && intoSecurity.isin == null && fromSecurity.isin != null)
            db.instrumentDao().upsertSecurity(intoSecurity.copy(isin = fromSecurity.isin))
        db.instrumentDao().delete(fromId)
        val note = "Merged from ${from.name}" + (fromSecurity?.symbol?.let { " ($it)" } ?: "")
        db.instrumentDao().update(
            into.copy(
                categoryId = into.categoryId ?: from.categoryId,
                active = true,
                notes = listOfNotNull(into.notes, note).joinToString("\n"),
                modifiedTime = now,
            )
        )
    }

    // ------------------------------------------------------------------ investment categories

    suspend fun addInvestmentCategory(category: InvestmentCategory): Long =
        db.instrumentDao().insertCategory(
            category.copy(
                displayOrder = (db.instrumentDao().getAllCategories()
                    .maxOfOrNull { it.displayOrder } ?: 0) + 1))

    suspend fun updateInvestmentCategory(category: InvestmentCategory) =
        db.instrumentDao().updateCategory(category)

    suspend fun deleteInvestmentCategory(id: Long) {
        require(
            db.instrumentDao().childCategoryCount(id) == 0
        ) { "Delete or move its sub-categories first" }
        require(
            db.instrumentDao().instrumentCountInCategory(id) == 0
        ) { "Some instruments still use this category" }
        db.instrumentDao().deleteCategory(id)
    }

    suspend fun reorderInvestmentCategories(orderedIds: List<Long>) = write {
        val current = db.instrumentDao().getAllCategories().filter { it.id in orderedIds }
        val base = current.minOfOrNull { it.displayOrder } ?: 1
        orderedIds.forEachIndexed { i, id ->
            db.instrumentDao().setCategoryDisplayOrder(id, base + i)
        }
    }

    // ------------------------------------------------------------------ risk levels

    suspend fun addRiskLevel(name: String): Long =
        db.instrumentDao().insertRiskLevel(
            RiskLevel(
                name = name.trim(),
                displayOrder = (db.instrumentDao().getAllRiskLevels()
                    .maxOfOrNull { it.displayOrder } ?: 0) + 1))

    suspend fun renameRiskLevel(level: RiskLevel, name: String) =
        db.instrumentDao().updateRiskLevel(level.copy(name = name.trim()))

    suspend fun deleteRiskLevel(id: Long) {
        require(
            db.instrumentDao().categoryCountWithRiskLevel(id) == 0
        ) { "Some investment categories still use this risk level" }
        db.instrumentDao().deleteRiskLevel(id)
    }

    suspend fun reorderRiskLevels(orderedIds: List<Long>) = write {
        val current = db.instrumentDao().getAllRiskLevels().filter { it.id in orderedIds }
        val base = current.minOfOrNull { it.displayOrder } ?: 1
        orderedIds.forEachIndexed { i, id ->
            db.instrumentDao().setRiskLevelDisplayOrder(id, base + i)
        }
    }

    // ------------------------------------------------------------------ summary groups

    suspend fun addSummaryGroup(name: String, members: String): Long {
        require(name.isNotBlank()) { "A name is needed" }
        return db.summaryGroupDao().insert(
            com.nagabhushansn95.financetracker.data.db.entity.SummaryGroup(
                name = name.trim(),
                displayOrder = (db.summaryGroupDao().getAll().maxOfOrNull { it.displayOrder }
                    ?: 0) + 1,
                members = members,
            )
        )
    }

    suspend fun updateSummaryGroup(group: com.nagabhushansn95.financetracker.data.db.entity.SummaryGroup) {
        require(group.name.isNotBlank()) { "A name is needed" }
        db.summaryGroupDao().update(group.copy(name = group.name.trim()))
    }

    suspend fun deleteSummaryGroup(id: Long) = db.summaryGroupDao().delete(id)

    suspend fun reorderSummaryGroups(orderedIds: List<Long>) = write {
        val current = db.summaryGroupDao().getAll().filter { it.id in orderedIds }
        val base = current.minOfOrNull { it.displayOrder } ?: 1
        orderedIds.forEachIndexed { i, id -> db.summaryGroupDao().setDisplayOrder(id, base + i) }
    }

    // ------------------------------------------------------------------ allocation targets

    /** Writes the goal amount and the per-category goal / wealth / tolerance percentages in one transaction. */
    suspend fun saveTargets(goalAmount: Double?, categories: List<InvestmentCategory>) = write {
        if (goalAmount == null) db.settingsDao()
            .remove(com.nagabhushansn95.financetracker.data.db.dao.Settings.GOAL_AMOUNT)
        else db.settingsDao().set(
            Setting(
                com.nagabhushansn95.financetracker.data.db.dao.Settings.GOAL_AMOUNT,
                goalAmount.toString()
            )
        )
        categories.forEach { db.instrumentDao().updateCategory(it) }
    }

    private fun stamp(now: LocalDateTime) = Converters().localDateTimeToString(now)!!

    private suspend fun <T> write(block: suspend () -> T): T =
        db.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
