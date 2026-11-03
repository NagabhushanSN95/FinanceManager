package com.nagabhushansn95.financetracker.data.importer.v4

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Runs the importer against a real v4 database file. The file is personal data and is never committed:
 * it is read from the FT_V4_DB environment variable (or the default download location) and the test is skipped if absent.
 */
@RunWith(RobolectricTestRunner::class)
class V4ImporterRealDbTest {
    private lateinit var db: AppDatabase
    private val v4File: File = File(
        System.getenv("FT_V4_DB")
            ?: (System.getProperty("user.home") + "/Downloads/FinanceManager/expenditureManager")
    )

    @Before
    fun setUp() {
        assumeTrue("v4 database not found at $v4File", v4File.isFile)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun importsWholeDatabase() = runTest {
        val report = SqliteV4Source(v4File.path).use { V4Importer(db).import(it) }
        println(report.summary())
        report.dropped.groupBy { it.reason }
            .forEach { (reason, rows) -> println("  dropped ${rows.size}: $reason") }
        println("  realised P/L by instrument: " + report.realisedPnlByInstrument.entries.sortedByDescending { it.value }
            .joinToString { "${it.key}=${"%.2f".format(it.value)}" })

        val v4Count = SqliteV4Source(v4File.path).use { it.transactions().size }
        assertEquals(v4Count, report.transactions + report.dropped.size)
        assertTrue(report.dropped.all { it.reason.startsWith("Realised") })

        val accounts = db.accountDao().getAll()
        assertTrue(accounts.none { it.name == "Investments" })
        println("  accounts: " + accounts.joinToString {
            "${it.name}[${it.accountType}${if (it.deleted) ",deleted" else ""}]=${
                "%.2f".format(
                    it.balance
                )
            }"
        })
        println(
            "  instruments: " + db.instrumentDao().getAll()
                .joinToString { "${it.name}[${it.instrumentType}]" })

        // Balances of real accounts are carried over unchanged from v4
        val v4Banks = SqliteV4Source(v4File.path).use { it.banks() }
        for (b in v4Banks.filter { it.name != "Investments" }) {
            val name = if (b.name == "Zerodha Demat") "Zerodha Demat INR" else b.name
            assertEquals(b.name, b.balance, accounts.single { it.name == name }.balance, 1e-6)
        }
        assertTrue(report.transactionsByKind.getValue(TransactionKind.INVEST) > 1000)
    }
}
