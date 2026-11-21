package com.nagabhushansn95.financetracker.data.db

import com.nagabhushansn95.financetracker.data.db.entity.BillAlert
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.db.entity.ValidityUnit
import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Builds a database of an older schema version from its exported JSON (exactly what a backup of that version looks
 * like), opens it with the app, and lets Room run the migrations and validate every table against the current
 * schema: a column or index the migration forgot fails the open. The data put in before must still be there after.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun aSchema11FileOpensOnTheCurrentAppWithItsDataIntact() = runTest {
        val file = context.getDatabasePath("migration-test.db")
        file.parentFile?.mkdirs()
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }

        createFromExportedSchema(file, 11)
        BundledSQLiteDriver().open(file.path).using { c ->
            c.execSQL(
                "INSERT INTO accounts (name, account_type, currency, balance, display_order, deleted, created_time, modified_time) " +
                        "VALUES ('Bank', 'BANK', 'INR', 1234.5, 1, 0, '2026-11-20T10:00:00.000', '2026-11-20T10:00:00.000')"
            )
        }

        val db = AppDatabase.create(context, "migration-test.db")
        try {
            val accounts = db.accountDao().getAll()
            assertEquals(1, accounts.size)
            assertEquals(1234.5, accounts.single().balance, 0.0)
            // The new tables are there and usable.
            assertEquals(listOf("CIBIL"), db.billDao().getAgencies().map { it.name })
            assertTrue(db.billDao().getAll().isEmpty())
        } finally {
            db.close()
        }
        BundledSQLiteDriver().open(file.path).using { c ->
            c.prepare("PRAGMA user_version")
                .using { s -> s.step(); assertEquals(AppDatabase.SCHEMA_VERSION, s.getInt(0)) }
        }
    }

    @Test
    fun aSchema12FileOpensWithItsBillsAlertingAsBefore() = runTest {
        val file = context.getDatabasePath("migration-test-12.db")
        file.parentFile?.mkdirs()
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }

        createFromExportedSchema(file, 12)
        BundledSQLiteDriver().open(file.path).using { c ->
            c.execSQL(
                "INSERT INTO bills (name, kind, schedule, every_months, first_due, covers_previous, created_time, modified_time) " +
                        "VALUES ('Home Rent', 'EXPENSE', 'CALENDAR', 1, '2026-12-10', 1, '2026-11-21T10:00:00.000', '2026-11-21T10:00:00.000')"
            )
        }

        val db = AppDatabase.create(context, "migration-test-12.db")
        try {
            val bill = db.billDao().getAll().single()
            assertEquals("Home Rent", bill.name)
            assertEquals(BillAlert.BEFORE_DUE, bill.alert)
        } finally {
            db.close()
        }
        BundledSQLiteDriver().open(file.path).using { c ->
            c.prepare("PRAGMA user_version")
                .using { s -> s.step(); assertEquals(AppDatabase.SCHEMA_VERSION, s.getInt(0)) }
        }
    }

    @Test
    fun aSchema13FileOpensWithItsBillsInTheOrderTheyWereAdded() = runTest {
        val file = context.getDatabasePath("migration-test-13.db")
        file.parentFile?.mkdirs()
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }

        createFromExportedSchema(file, 13)
        BundledSQLiteDriver().open(file.path).using { c ->
            listOf("Home Rent", "Netflix").forEach { name ->
                c.execSQL(
                    "INSERT INTO bills (name, kind, schedule, every_months, first_due, covers_previous, alert_when_overdue, created_time, modified_time) " +
                            "VALUES ('$name', 'EXPENSE', 'CALENDAR', 1, '2026-12-10', 0, 0, '2026-11-21T10:00:00.000', '2026-11-21T10:00:00.000')"
                )
            }
        }

        val db = AppDatabase.create(context, "migration-test-13.db")
        try {
            val bills = db.billDao().getAll().sortedBy { it.id }
            assertEquals(listOf(1, 2), bills.map { it.displayOrder })
        } finally {
            db.close()
        }
    }

    @Test
    fun aSchema14FileKeepsItsAlertChoiceUnderTheNewName() = runTest {
        val file = context.getDatabasePath("migration-test-14.db")
        file.parentFile?.mkdirs()
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }

        createFromExportedSchema(file, 14)
        BundledSQLiteDriver().open(file.path).using { c ->
            c.execSQL(
                "INSERT INTO bills (name, kind, schedule, every_months, first_due, covers_previous, alert_when_overdue, display_order, created_time, modified_time) " +
                        "VALUES ('Netflix', 'EXPENSE', 'CALENDAR', 1, '2026-12-01', 0, 1, 1, '2026-11-21T10:00:00.000', '2026-11-21T10:00:00.000')"
            )
        }

        val db = AppDatabase.create(context, "migration-test-14.db")
        try {
            assertEquals(BillAlert.WHEN_OVERDUE, db.billDao().getAll().single().alert)
        } finally {
            db.close()
        }
    }

    @Test
    fun aSchema15FileKeepsItsValidityCountedInDays() = runTest {
        val file = context.getDatabasePath("migration-test-15.db")
        file.parentFile?.mkdirs()
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }

        createFromExportedSchema(file, 15)
        BundledSQLiteDriver().open(file.path).using { c ->
            c.execSQL(
                "INSERT INTO bills (name, kind, schedule, every_months, first_due, covers_previous, validity_days, alert_mode, display_order, created_time, modified_time) " +
                        "VALUES ('Airtel Wi-Fi', 'EXPENSE', 'DURATION', 1, '2024-03-13', 0, 180, 0, 1, '2026-11-21T10:00:00.000', '2026-11-21T10:00:00.000')"
            )
        }

        val db = AppDatabase.create(context, "migration-test-15.db")
        try {
            val bill = db.billDao().getAll().single()
            assertEquals(180, bill.validity)
            assertEquals(ValidityUnit.DAYS, bill.validityUnit)
            assertEquals("Every 180 days from the last payment", Bills.scheduleText(bill))
        } finally {
            db.close()
        }
    }

    /** Runs the `createSql` of every entity and index in `app/schemas/…/<version>.json` and stamps the version. */
    private fun createFromExportedSchema(file: File, version: Int) {
        val json = listOf(
            "schemas/com.nagabhushansn95.financetracker.data.db.AppDatabase/$version.json",
            "app/schemas/com.nagabhushansn95.financetracker.data.db.AppDatabase/$version.json",
        ).map(::File).firstOrNull { it.exists() }
            ?: error("Exported schema $version.json not found; it is written by the build with exportSchema = true")
        val database = JSONObject(json.readText()).getJSONObject("database")
        assertEquals(version, database.getInt("version"))
        BundledSQLiteDriver().open(file.path).using { c ->
            val entities = database.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                c.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = e.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    c.execSQL(
                        indices.getJSONObject(j).getString("createSql")
                            .replace("\${TABLE_NAME}", table)
                    )
                }
            }
            val views = database.optJSONArray("views")
            if (views != null) for (i in 0 until views.length()) {
                val v = views.getJSONObject(i)
                c.execSQL(
                    v.getString("createSql").replace("\${VIEW_NAME}", v.getString("viewName"))
                )
            }
            c.execSQL("PRAGMA user_version = $version")
        }
    }
}

private inline fun <R> SQLiteConnection.using(block: (SQLiteConnection) -> R): R = try {
    block(this)
} finally {
    close()
}

private inline fun <R> SQLiteStatement.using(block: (SQLiteStatement) -> R): R = try {
    block(this)
} finally {
    close()
}
