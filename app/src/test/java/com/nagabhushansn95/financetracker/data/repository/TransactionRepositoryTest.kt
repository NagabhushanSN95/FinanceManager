package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class TransactionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: TransactionRepository
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private var wallet = 0L
    private var bank = 0L
    private var card = 0L
    private var food = 0L
    private var stock = 0L

    @Before
    fun setUp() = runTest {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repo = TransactionRepository(db)
        wallet = db.accountDao().insert(
            Account(
                name = "Cash",
                accountType = AccountType.WALLET,
                balance = 1000.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        bank = db.accountDao().insert(
            Account(
                name = "HDFC",
                accountType = AccountType.BANK,
                balance = 5000.0,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        card = db.accountDao().insert(
            Account(
                name = "Amex",
                accountType = AccountType.CREDIT_CARD,
                balance = -200.0,
                displayOrder = 3,
                createdTime = now,
                modifiedTime = now
            )
        )
        food = db.categoryDao()
            .insert(Category(name = "Food", appliesTo = CategoryScope.EXPENSE, displayOrder = 1))
        stock = db.instrumentDao().insert(
            Instrument(
                name = "GOLDBEES",
                instrumentType = InstrumentType.STOCK,
                symbol = "GOLDBEES",
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private fun tx(
        kind: TransactionKind,
        amount: Double,
        from: Long? = null,
        to: Long? = null,
        category: Long? = null,
        instrument: Long? = null,
        particulars: String = "x"
    ) =
        TransactionEntity(
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.of(2026, 11, 4),
            kind = kind,
            fromAccountId = from,
            toAccountId = to,
            categoryId = category,
            instrumentId = instrument,
            particulars = particulars,
            amount = amount
        )

    private suspend fun balance(id: Long) = db.accountDao().getById(id)!!.balance

    @Test
    fun eachKindMovesTheRightBalances() = runTest {
        repo.add(tx(TransactionKind.EXPENSE, 150.0, from = wallet, category = food))
        assertEquals(850.0, balance(wallet), 0.0)

        repo.add(tx(TransactionKind.INCOME, 300.0, to = bank))
        assertEquals(5300.0, balance(bank), 0.0)

        repo.add(tx(TransactionKind.TRANSFER, 200.0, from = bank, to = card))
        assertEquals(5100.0, balance(bank), 0.0)
        assertEquals(0.0, balance(card), 0.0)

        repo.add(tx(TransactionKind.INVEST, 1000.0, from = bank, instrument = stock))
        assertEquals(4100.0, balance(bank), 0.0)

        repo.add(tx(TransactionKind.REDEEM, 400.0, to = bank, instrument = stock))
        assertEquals(4500.0, balance(bank), 0.0)

        // Renewal-style rows with a NULL side touch nothing.
        repo.add(tx(TransactionKind.INVEST, 999.0, instrument = stock))
        repo.add(tx(TransactionKind.REDEEM, 999.0, instrument = stock))
        assertEquals(4500.0, balance(bank), 0.0)
        assertEquals(7, db.transactionDao().count())
    }

    @Test
    fun updateReversesTheOldEffectAndAppliesTheNew() = runTest {
        val id =
            repo.add(tx(TransactionKind.EXPENSE, 150.0, from = wallet, category = food), now = now)
        val later = now.plusHours(1)
        repo.update(
            db.transactionDao().getById(id)!!.copy(amount = 50.0, fromAccountId = bank),
            now = later
        )

        assertEquals(1000.0, balance(wallet), 0.0)
        assertEquals(4950.0, balance(bank), 0.0)
        val stored = db.transactionDao().getById(id)!!
        assertEquals(now, stored.createdTime)
        assertEquals(later, stored.modifiedTime)
    }

    @Test
    fun deleteRestoresBalances() = runTest {
        val id = repo.add(tx(TransactionKind.TRANSFER, 200.0, from = bank, to = wallet))
        repo.delete(id)
        assertEquals(5000.0, balance(bank), 0.0)
        assertEquals(1000.0, balance(wallet), 0.0)
        assertNull(db.transactionDao().getById(id))
        repo.delete(id) // deleting again is a no-op
    }

    @Test
    fun savesATemplateWhenAsked() = runTest {
        repo.add(
            tx(
                TransactionKind.EXPENSE,
                60.0,
                from = wallet,
                category = food,
                particulars = "Lunch"
            ), saveAsTemplate = true
        )
        val template = db.transactionDao().getAllTemplates().single()
        assertEquals("Lunch", template.particulars)
        assertEquals(TransactionKind.EXPENSE, template.kind)
        assertEquals(wallet, template.fromAccountId)
        assertEquals(food, template.categoryId)
        // The template keeps the rate (the amount of a 1 × 60 row) and no quantity, since one is the default.
        assertEquals(60.0, template.rate)
        assertEquals(null, template.quantity)
    }

    @Test
    fun rejectsInconsistentRowsWithoutTouchingBalances() = runTest {
        val bad = listOf(
            tx(TransactionKind.EXPENSE, 10.0, from = wallet),                     // no category
            tx(TransactionKind.EXPENSE, 10.0, category = food),                   // no account
            tx(TransactionKind.TRANSFER, 10.0, from = wallet, to = wallet),       // same account
            tx(TransactionKind.INVEST, 10.0, from = bank),                        // no instrument
            tx(
                TransactionKind.INCOME,
                10.0,
                from = bank,
                to = wallet
            ),           // income with a source
            tx(TransactionKind.EXPENSE, -5.0, from = wallet, category = food),    // negative
        )
        for (t in bad) {
            val error = runCatching { repo.add(t) }.exceptionOrNull()
            assertTrue("expected rejection for $t", error is IllegalArgumentException)
        }
        assertEquals(0, db.transactionDao().count())
        assertEquals(1000.0, balance(wallet), 0.0)
        assertEquals(5000.0, balance(bank), 0.0)
    }
}
