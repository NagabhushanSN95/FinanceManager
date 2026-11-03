package com.nagabhushansn95.financetracker.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAccountAndExpenseRoundTrip() = runTest {
        val now = LocalDateTime.of(2026, 11, 3, 9, 2, 55, 123_000_000)
        val walletId = db.accountDao().insert(
            Account(
                name = "Wallet",
                accountType = AccountType.WALLET,
                balance = 5400.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val foodId = db.categoryDao().insert(Category(name = "Food", displayOrder = 1))
        val txId = db.transactionDao().insert(
            TransactionEntity(
                createdTime = now, modifiedTime = now, date = LocalDate.of(2026, 11, 1),
                kind = TransactionKind.EXPENSE, fromAccountId = walletId, categoryId = foodId,
                particulars = "Dinner On Zomato", amount = 385.19,
            )
        )

        val tx = db.transactionDao().getById(txId)
        assertNotNull(tx)
        assertEquals(TransactionKind.EXPENSE, tx!!.kind)
        assertEquals(LocalDate.of(2026, 11, 1), tx.date)
        assertEquals(now, tx.createdTime)
        assertEquals(385.19, tx.amountInr, 1e-9)
        assertEquals(1, db.transactionDao().count())
        assertEquals("Wallet", db.accountDao().getVisibleByType(AccountType.WALLET).single().name)
    }
}
