package com.nagabhushansn95.financetracker.data.investments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.repository.OthersRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class OthersRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: OthersRepository
    private val now = LocalDateTime.of(2026, 11, 10, 9, 0)
    private var bank = 0L
    private var wallet = 0L

    @Before
    fun setUp() = runTest {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repo = OthersRepository(db)
        bank = db.accountDao().insert(
            Account(
                name = "Bank",
                accountType = AccountType.BANK,
                balance = 100000.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        wallet = db.accountDao().insert(
            Account(
                name = "Wallet",
                accountType = AccountType.WALLET,
                balance = 1000.0,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun view(id: Long): OtherView =
        Others.build(db.instrumentDao().getAll(), db.transactionDao().getAllWithInstrument())
            .single { it.instrument.id == id }

    private suspend fun balance(id: Long) = db.accountDao().getById(id)!!.balance

    @Test
    fun aLoanGoesOutComesBackInPartsAndSettles() = runTest {
        val id = repo.add("Loan To Shreedhar Kulkarni", " ", now)
        val instrument = db.instrumentDao().getById(id)!!
        assertEquals(InstrumentType.OTHER, instrument.instrumentType)
        assertNull(instrument.notes)
        assertTrue(view(id).rows.isEmpty())

        val outId = repo.saveEntry(
            view(id),
            OtherEntryKind.OUT,
            LocalDate.of(2020, 5, 11),
            15000.0,
            "",
            bank,
            now = now
        )
        val out = db.transactionDao().getById(outId)!!
        assertEquals(TransactionKind.INVEST, out.kind)
        assertEquals("Loaned To Shreedhar Kulkarni", out.particulars)
        assertEquals(bank, out.fromAccountId)
        assertNull(out.quantity)
        assertEquals(85000.0, balance(bank), 1e-9)
        assertEquals(15000.0, view(id).outstanding, 1e-9)

        val backId = repo.saveEntry(
            view(id),
            OtherEntryKind.IN,
            LocalDate.of(2026, 12, 1),
            5000.0,
            "",
            wallet,
            now = now
        )
        assertEquals(
            "Shreedhar Kulkarni Paid Back",
            db.transactionDao().getById(backId)!!.particulars
        )
        assertEquals(6000.0, balance(wallet), 1e-9)
        assertEquals(10000.0, view(id).outstanding, 1e-9)
        assertFalse(view(id).settled)

        // Editing moves the repayment to the bank and makes it the rest of the loan.
        repo.saveEntry(
            view(id),
            OtherEntryKind.IN,
            LocalDate.of(2026, 12, 1),
            15000.0,
            "All Of It Back",
            bank,
            existing = db.transactionDao().getById(backId),
            now = now
        )
        assertEquals(1000.0, balance(wallet), 1e-9)
        assertEquals(100000.0, balance(bank), 1e-9)
        assertTrue(view(id).settled)
        assertEquals("All Of It Back", Others.suggestedParticulars(OtherEntryKind.IN, view(id)))

        // Deleting the repayment reopens it and takes the money back out of the bank.
        repo.deleteEntry(backId, now)
        assertEquals(85000.0, balance(bank), 1e-9)
        assertFalse(view(id).settled)

        // The whole loan can go, balances put back.
        repo.delete(id, now)
        assertNull(db.instrumentDao().getById(id))
        assertTrue(db.transactionDao().getForInstrument(id).isEmpty())
        assertEquals(100000.0, balance(bank), 1e-9)
    }

    @Test
    fun aZeroOrBlankEntryIsRefused() = runTest {
        val id = repo.add("Amma's Account", "KVGB", now)
        assertEquals("KVGB", db.instrumentDao().getById(id)!!.notes)
        val failed = runCatching {
            repo.saveEntry(
                view(id),
                OtherEntryKind.OUT,
                LocalDate.of(2026, 1, 1),
                0.0,
                "",
                bank,
                now = now
            )
        }
        assertTrue(failed.isFailure)
        assertTrue(runCatching { repo.add("  ", null, now) }.isFailure)
        assertEquals(100000.0, balance(bank), 1e-9)
    }
}
