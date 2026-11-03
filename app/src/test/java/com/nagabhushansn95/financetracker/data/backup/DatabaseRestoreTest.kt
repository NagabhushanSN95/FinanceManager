package com.nagabhushansn95.financetracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class DatabaseRestoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        AppDatabase.replaceDatabaseFile(
            context,
            File.createTempFile("empty", ".db").also { it.delete(); it.createNewFile() })
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
    }

    @Test
    fun restoresABackupIntoTheAppDatabase() = runTest {
        // A backup taken from one database…
        val now = LocalDateTime.of(2026, 11, 3, 10, 0)
        val source = AppDatabase.create(context, "source.db")
        source.accountDao().insert(
            Account(
                name = "HDFC",
                accountType = AccountType.BANK,
                balance = 500.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val backup = File(context.cacheDir, "Finance Tracker Database - test.db")
        backup.outputStream().use { DatabaseBackup(context, source).writeTo(it) }
        source.close()
        context.deleteDatabase("source.db")

        // …restored over the (empty) app database.
        val result = DatabaseRestore(context).restoreFrom(Uri.fromFile(backup))

        assertEquals(1, result.accounts)
        assertEquals(0, result.transactions)
        val restored = AppDatabase.getInstance(context).accountDao().getAll().single()
        assertEquals("HDFC", restored.name)
        assertEquals(500.0, restored.balance, 0.0)
        backup.delete()
    }

    @Test
    fun rejectsAFinanceManager4File() = runTest {
        val fake = File(context.cacheDir, "v4.db")
        // Any SQLite file without Room's marker table counts as "not ours".
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(fake, null).use {
            it.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY, type TEXT)")
        }
        val error =
            runCatching { DatabaseRestore(context).restoreFrom(Uri.fromFile(fake)) }.exceptionOrNull()
        assertTrue(error is IOException)
        assertTrue(error!!.message!!.contains("Finance Manager 4.x"))
        fake.delete()
    }

    @Test
    fun rejectsANonDatabaseFile() = runTest {
        val text = File(context.cacheDir, "notes.txt").apply { writeText("hello") }
        val error =
            runCatching { DatabaseRestore(context).restoreFrom(Uri.fromFile(text)) }.exceptionOrNull()
        assertTrue(error is IOException)
        text.delete()
    }
}
