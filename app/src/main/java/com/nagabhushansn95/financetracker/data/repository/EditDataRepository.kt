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
        db.instrumentDao().insert(
            instrument.copy(
                displayOrder = db.instrumentDao().maxDisplayOrder() + 1,
                createdTime = now,
                modifiedTime = now
            )
        )

    suspend fun updateInstrument(instrument: Instrument, now: LocalDateTime = LocalDateTime.now()) =
        db.instrumentDao().update(instrument.copy(modifiedTime = now))

    suspend fun setInstrumentActive(
        id: Long,
        active: Boolean,
        now: LocalDateTime = LocalDateTime.now()
    ) = write {
        if (active) db.instrumentDao().setDisplayOrder(id, db.instrumentDao().maxDisplayOrder() + 1)
        db.instrumentDao().setActive(id, active, stamp(now))
    }

    suspend fun reorderInstruments(orderedIds: List<Long>) = write {
        val current = db.instrumentDao().getAll().filter { it.id in orderedIds }
        val base = current.minOfOrNull { it.displayOrder } ?: 1
        orderedIds.forEachIndexed { i, id -> db.instrumentDao().setDisplayOrder(id, base + i) }
    }

    suspend fun instrumentTransactionCount(id: Long) = db.instrumentDao().transactionCount(id)

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
