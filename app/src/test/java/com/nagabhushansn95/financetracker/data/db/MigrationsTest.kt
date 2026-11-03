package com.nagabhushansn95.financetracker.data.db

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Builds a database from the exported schema of an old version, fills it, then opens it with the current AppDatabase. */
@RunWith(RobolectricTestRunner::class)
class MigrationsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Creates the tables of [version] exactly as Room exported them (app/schemas), with user_version set. */
    private fun createOldDatabase(
        name: String,
        version: Int,
        fill: (androidx.sqlite.SQLiteConnection) -> Unit
    ): File {
        val schema =
            JSONObject(File("schemas/${AppDatabase::class.java.name}/$version.json").readText()).getJSONObject(
                "database"
            )
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        val c = BundledSQLiteDriver().open(file.path)
        try {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                c.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = e.getJSONArray("indices")
                for (j in 0 until indices.length()) c.execSQL(
                    indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table)
                )
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) c.execSQL(setup.getString(i))
            fill(c)
            c.execSQL("PRAGMA user_version = $version")
        } finally {
            c.close()
        }
        return file
    }

    @Test
    fun migrates1To2KeepingCategoriesAndTheirInstruments() = runTest {
        createOldDatabase("migrate-1-2.db", 1) { c ->
            c.execSQL("INSERT INTO investment_categories (id, name, parent_id, risk_bucket, target_pct, tolerance_pct, target_goal_pct, target_wealth_pct, rebalance_notes, display_order) VALUES (1, 'Stocks', NULL, 'MEDIUM', 0.2, 0.03, 0.0, 0.32, NULL, 1)")
            c.execSQL("INSERT INTO investment_categories (id, name, parent_id, risk_bucket, target_pct, tolerance_pct, target_goal_pct, target_wealth_pct, rebalance_notes, display_order) VALUES (2, 'REIT', 1, 'MEDIUM', 0.07, 0.03, 0.0, 0.12, 'Do not rebalance against equity', 2)")
            c.execSQL("INSERT INTO investment_categories (id, name, parent_id, risk_bucket, target_pct, tolerance_pct, target_goal_pct, target_wealth_pct, rebalance_notes, display_order) VALUES (3, 'Equity', 1, 'HIGH', NULL, NULL, NULL, NULL, NULL, 3)")
            c.execSQL("INSERT INTO instruments (id, name, instrument_type, currency, category_id, owner, active, display_order, created_time, modified_time) VALUES (10, 'EMBASSY', 'STOCK', 'INR', 2, 'Self', 1, 1, '2026-01-01T00:00:00.000', '2026-01-01T00:00:00.000')")
        }
        val db = AppDatabase.create(context, "migrate-1-2.db")
        try {
            val levels = db.instrumentDao().getAllRiskLevels()
            assertEquals(InvestmentCategories.DEFAULT_RISK_LEVELS, levels.map { it.name })
            val categories = db.instrumentDao().getAllCategories()
            assertEquals(
                listOf("Stocks", "REIT", "Equity", InvestmentCategories.PROVIDENT_FUNDS),
                categories.map { it.name })
            val reit = categories.first { it.name == "REIT" }
            assertEquals(1L, reit.parentId)
            assertEquals(
                levels.first { it.name == InvestmentCategories.MEDIUM_RISK }.id,
                reit.riskLevelId
            )
            assertEquals(0.12, reit.targetWealthPct!!, 1e-9)
            assertEquals(0.03, reit.tolerancePct!!, 1e-9)
            assertEquals("Do not rebalance against equity", reit.rebalanceNotes)
            assertEquals(
                levels.first { it.name == InvestmentCategories.HIGH_RISK }.id,
                categories.first { it.name == "Equity" }.riskLevelId
            )
            assertEquals(
                levels.first { it.name == InvestmentCategories.NO_RISK }.id,
                categories.first { it.name == InvestmentCategories.PROVIDENT_FUNDS }.riskLevelId
            )
            assertEquals(2L, db.instrumentDao().getById(10)!!.categoryId)
            assertNull(
                db.settingsDao()
                    .get(com.nagabhushansn95.financetracker.data.db.dao.Settings.GOAL_AMOUNT)
            )
            // Foreign keys are enforced again after the migration.
            val result = runCatching {
                db.instrumentDao()
                    .deleteRiskLevel(levels.first { it.name == InvestmentCategories.MEDIUM_RISK }.id)
            }
            assertEquals(true, result.isFailure)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrates1To2WithoutSeedingAnEmptyTree() = runTest {
        createOldDatabase("migrate-1-2-empty.db", 1) {}
        val db = AppDatabase.create(context, "migrate-1-2-empty.db")
        try {
            assertEquals(4, db.instrumentDao().riskLevelCount())
            assertEquals(0, db.instrumentDao().categoryCount())
            // ... and the regular seed then fills the tree, including Provident Funds.
            InvestmentCategories.seedIfEmpty(db)
            assertEquals(
                1,
                db.instrumentDao().getAllCategories()
                    .count { it.name == InvestmentCategories.PROVIDENT_FUNDS })
        } finally {
            db.close()
        }
    }
}
