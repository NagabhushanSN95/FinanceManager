package com.nagabhushansn95.financetracker.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class DatabaseBackupTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // A real file under the app's database folder, so that getDatabasePath() finds it as it does on the phone.
        db = AppDatabase.create(context, AppDatabase.DATABASE_NAME)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
    }

    @Test
    fun backupIsASelfContainedSqliteFileWithTheLatestWrites() = runTest {
        val now = LocalDateTime.of(2026, 11, 3, 10, 0)
        db.accountDao().insert(
            Account(
                name = "HDFC",
                accountType = AccountType.BANK,
                balance = 1234.5,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )

        val bytes =
            ByteArrayOutputStream().also { DatabaseBackup(context, db).writeTo(it) }.toByteArray()

        assertTrue(bytes.size > 100)
        assertEquals("SQLite format 3", String(bytes, 0, 15, Charsets.US_ASCII))
        // Rollback-journal format flags, so the file opens read-only on a desktop without -wal/-shm files.
        assertEquals(1, bytes[18].toInt())
        assertEquals(1, bytes[19].toInt())

        val copy = File.createTempFile("backup", ".db").apply { writeBytes(bytes) }
        try {
            SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READONLY)
                .use { sqlite ->
                    sqlite.rawQuery("SELECT name, balance FROM accounts", null).use { c ->
                        assertTrue(c.moveToFirst())
                        assertEquals("HDFC", c.getString(0))
                        assertEquals(1234.5, c.getDouble(1), 0.0)
                        assertEquals(1, c.count)
                    }
                }
        } finally {
            copy.delete()
        }

        // The live database keeps working after the backup.
        val later = now.plusMinutes(1)
        db.accountDao().insert(
            Account(
                name = "Cash",
                accountType = AccountType.WALLET,
                displayOrder = 2,
                createdTime = later,
                modifiedTime = later
            )
        )
        assertEquals(2, db.accountDao().getAll().size)
    }

    @Test
    fun suggestedFileNameUsesTheTimestamp() {
        val name =
            DatabaseBackup(context, db).suggestedFileName(LocalDateTime.of(2026, 11, 3, 9, 2, 55))
        assertEquals("Finance Tracker Database - 2026-11-03 09-02-55.db", name)
    }
}
