package com.nagabhushansn95.financetracker.data.db

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
            assertTrue(!bill.alertWhenOverdue)
        } finally {
            db.close()
        }
        BundledSQLiteDriver().open(file.path).using { c ->
            c.prepare("PRAGMA user_version")
                .using { s -> s.step(); assertEquals(AppDatabase.SCHEMA_VERSION, s.getInt(0)) }
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
