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
                val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
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

    @Test
    fun migrates2To3MovingTheTemplateAmountIntoTheRate() = runTest {
        createOldDatabase("migrate-2-3.db", 2) { c ->
            c.execSQL("INSERT INTO accounts (id, name, account_type, currency, balance, display_order, deleted, created_time, modified_time) VALUES (1, 'Wallet', 'WALLET', 'INR', 0, 1, 0, '2026-01-01T00:00:00.000', '2026-01-01T00:00:00.000')")
            c.execSQL("INSERT INTO templates (id, particulars, kind, from_account_id, amount, hidden) VALUES (1, 'Milk', 'EXPENSE', 1, 30.0, 0)")
            c.execSQL("INSERT INTO templates (id, particulars, kind, from_account_id, amount, hidden) VALUES (2, 'Bus', 'EXPENSE', 1, NULL, 1)")
        }
        val db = AppDatabase.create(context, "migrate-2-3.db")
        try {
            val templates = db.transactionDao().getAllTemplates().sortedBy { it.id }
            assertEquals(listOf("Milk", "Bus"), templates.map { it.particulars })
            assertEquals(30.0, templates[0].rate!!, 1e-9)
            assertNull(templates[0].quantity)
            assertNull(templates[1].rate)
            assertEquals(true, templates[1].hidden)
            assertEquals(1L, templates[0].fromAccountId)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrates3To4AddingTheDepositTemplates() = runTest {
        createOldDatabase("migrate-3-4.db", 3) { c ->
            c.execSQL("INSERT INTO templates (id, particulars, kind, rate, hidden) VALUES (1, 'Milk', 'EXPENSE', 30.0, 0)")
        }
        val db = AppDatabase.create(context, "migrate-3-4.db")
        try {
            assertEquals(
                listOf("Milk"),
                db.transactionDao().getAllTemplates().map { it.particulars })
            val id = db.instrumentDao().insertFdTemplate(
                com.nagabhushansn95.financetracker.data.db.entity.FdTemplate(
                    name = "SBI 1y",
                    bankName = "SBI",
                    periodMonths = 12,
                    rate = 7.0
                )
            )
            val saved = db.instrumentDao().getAllFdTemplates().single()
            assertEquals(id, saved.id)
            assertEquals("SBI", saved.bankName)
            assertEquals(7.0, saved.rate!!, 1e-9)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrates4To5KeepingTemplatesAndAddingEvents() = runTest {
        createOldDatabase("migrate-4-5.db", 4) { c ->
            c.execSQL("INSERT INTO fd_templates (id, name, bank_name, period_months, rate, tds_rate, owner, display_order) VALUES (1, 'SBI 1y', 'SBI', 12, 7.0, 0.1, 'Self', 1)")
            c.execSQL("INSERT INTO instruments (id, name, instrument_type, currency, owner, active, display_order, created_time, modified_time) VALUES (5, 'FD #1', 'FIXED_DEPOSIT', 'INR', 'Self', 1, 1, '2026-01-01T00:00:00', '2026-01-01T00:00:00')")
            c.execSQL("INSERT INTO fixed_deposits (instrument_id, bank_name, status) VALUES (5, 'SBI', 'ACTIVE')")
        }
        val db = AppDatabase.create(context, "migrate-4-5.db")
        try {
            val template = db.instrumentDao().getAllFdTemplates().single()
            assertEquals("SBI 1y", template.name)
            assertEquals(12, template.periodMonths)
            assertEquals("Self", template.owner)
            assertEquals(null, template.amount)
            val terms = db.instrumentDao().getFixedDeposit(5)!!
            assertEquals(null, terms.tdsYearly)
            db.instrumentDao().upsertFixedDeposit(terms.copy(tdsYearly = true))
            assertEquals(true, db.instrumentDao().getFixedDeposit(5)!!.tdsYearly)
            val id = db.instrumentDao().insertFdEvent(
                com.nagabhushansn95.financetracker.data.db.entity.FdEvent(
                    instrumentId = 5,
                    date = java.time.LocalDate.of(2026, 3, 31),
                    kind = com.nagabhushansn95.financetracker.data.db.entity.FdEventKind.TDS,
                    amount = 640.0,
                )
            )
            assertEquals(id, db.instrumentDao().getAllFdEvents().single().id)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrates5To6AddingTheDepositType() = runTest {
        createOldDatabase("migrate-5-6.db", 5) { c ->
            c.execSQL("INSERT INTO instruments (id, name, instrument_type, currency, owner, active, display_order, created_time, modified_time) VALUES (5, 'FD #1', 'FIXED_DEPOSIT', 'INR', 'Self', 1, 1, '2026-01-01T00:00:00', '2026-01-01T00:00:00')")
            c.execSQL("INSERT INTO fixed_deposits (instrument_id, bank_name, status) VALUES (5, 'HDFC', 'ACTIVE')")
        }
        val db = AppDatabase.create(context, "migrate-5-6.db")
        try {
            val terms = db.instrumentDao().getFixedDeposit(5)!!
            assertEquals(
                com.nagabhushansn95.financetracker.data.db.entity.FdType.REGULAR,
                terms.depositType
            )
            assertEquals(null, terms.sweepInDate)
            db.instrumentDao().upsertFixedDeposit(
                terms.copy(
                    depositType = com.nagabhushansn95.financetracker.data.db.entity.FdType.SWEEP_IN,
                    sweepInDate = java.time.LocalDate.of(2024, 12, 1)
                )
            )
            val again = db.instrumentDao().getFixedDeposit(5)!!
            assertEquals(
                com.nagabhushansn95.financetracker.data.db.entity.FdType.SWEEP_IN,
                again.depositType
            )
            assertEquals(java.time.LocalDate.of(2024, 12, 1), again.sweepInDate)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrates6To7RebuildingInsuranceAndAddingValidityPeriods() = runTest {
        createOldDatabase("migrate-6-7.db", 6) { c ->
            c.execSQL("INSERT INTO instruments (id, name, instrument_type, currency, owner, active, display_order, created_time, modified_time) VALUES (3, 'SBI Life - Smart Platina Plus', 'INSURANCE', 'INR', 'Self', 1, 1, '2026-01-01T00:00:00', '2026-01-01T00:00:00')")
            c.execSQL("INSERT INTO instruments (id, name, instrument_type, currency, owner, active, display_order, created_time, modified_time) VALUES (4, 'Something', 'INSURANCE', 'INR', 'Self', 1, 2, '2026-01-01T00:00:00', '2026-01-01T00:00:00')")
            c.execSQL("INSERT INTO insurance_policies (instrument_id, insurance_type, provider, policy_name, premium_amount, premium_frequency_months, start_date, next_due_date, counts_as_investment) VALUES (3, 'LIFE', 'SBI Life', 'Smart Platina Plus', 8500.0, 1, '2024-06-28', '2026-11-20', 1)")
            c.execSQL("INSERT INTO insurance_policies (instrument_id, insurance_type, premium_frequency_months, next_due_date, counts_as_investment) VALUES (4, 'OTHER', 12, '2027-03-10', 0)")
        }
        val db = AppDatabase.create(context, "migrate-6-7.db")
        try {
            val life = db.instrumentDao().getInsurancePolicy(3)!!
            assertEquals(
                com.nagabhushansn95.financetracker.data.db.entity.InsuranceType.LIFE,
                life.insuranceType
            )
            assertEquals(20, life.dueDay)
            assertEquals(11, life.dueMonth)
            assertEquals(8500.0, life.premiumAmount!!, 1e-9)
            assertEquals(java.time.LocalDate.of(2024, 6, 28), life.startDate)
            assertNull(life.vehicleNumber)
            val other = db.instrumentDao().getInsurancePolicy(4)!!
            assertEquals(
                com.nagabhushansn95.financetracker.data.db.entity.InsuranceType.TERM,
                other.insuranceType
            )
            assertEquals(10, other.dueDay)
            assertEquals(3, other.dueMonth)
            val id = db.instrumentDao().insertValidityPeriod(
                com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod(
                    instrumentId = 3,
                    kind = com.nagabhushansn95.financetracker.data.db.entity.ValidityKind.COVER,
                    endDate = java.time.LocalDate.of(2027, 1, 1)
                )
            )
            assertEquals(id, db.instrumentDao().getValidityPeriods(3).single().id)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrates7To8AddingTheSumInsuredToValidityPeriods() = runTest {
        createOldDatabase("migrate-7-8.db", 7) { c ->
            c.execSQL("INSERT INTO instruments (id, name, instrument_type, currency, owner, active, display_order, created_time, modified_time) VALUES (5, 'HDFC Ergo - Optima Secure', 'INSURANCE', 'INR', 'Self', 1, 1, '2026-01-01T00:00:00', '2026-01-01T00:00:00')")
            c.execSQL("INSERT INTO insurance_policies (instrument_id, insurance_type, provider, policy_name, sum_assured) VALUES (5, 'HEALTH', 'HDFC Ergo', 'Optima Secure', 1000000.0)")
            c.execSQL("INSERT INTO validity_periods (id, instrument_id, kind, start_date, end_date, provider, reference) VALUES (9, 5, 'COVER', '2026-03-16', '2027-03-15', 'HDFC Ergo', 'P-2026')")
        }
        val db = AppDatabase.create(context, "migrate-7-8.db")
        try {
            val period = db.instrumentDao().getValidityPeriods(5).single()
            assertEquals(9L, period.id)
            assertEquals("P-2026", period.reference)
            assertNull(period.sumInsured)
            db.instrumentDao().updateValidityPeriod(period.copy(sumInsured = 2000000.0))
            assertEquals(
                2000000.0,
                db.instrumentDao().getValidityPeriods(5).single().sumInsured!!,
                1e-9
            )
        } finally {
            db.close()
        }
    }
}
