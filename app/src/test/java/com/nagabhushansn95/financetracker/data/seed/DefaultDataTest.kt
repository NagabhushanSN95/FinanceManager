package com.nagabhushansn95.financetracker.data.seed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultDataTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun skipSeedsOneWalletAndStarterCategories() = runTest {
        DefaultData.seed(db)

        val accounts = db.accountDao().getAll()
        assertEquals(1, accounts.size)
        assertEquals(AccountType.WALLET, accounts.single().accountType)
        assertEquals(0.0, accounts.single().balance, 0.0)

        val categories = db.categoryDao().getAll()
        assertEquals(
            DefaultData.expenseCategories,
            categories.filter { it.appliesTo == CategoryScope.EXPENSE }.map { it.name })
        assertEquals(
            DefaultData.incomeCategories,
            categories.filter { it.appliesTo == CategoryScope.INCOME }.map { it.name })
        assertEquals(
            DefaultData.sharedCategories,
            categories.filter { it.appliesTo == CategoryScope.BOTH }.map { it.name })
        assertEquals(
            categories.map { it.displayOrder },
            categories.map { it.displayOrder }.sorted()
        )
    }

    @Test
    fun wizardChoicesCreateWalletsBanksAndChosenCategories() = runTest {
        DefaultData.seed(
            db,
            SetupChoices(
                wallets = listOf(NewAccount("Cash", 1500.0), NewAccount(" Travel wallet ", 20.5)),
                banks = listOf(NewAccount("HDFC", 12000.0, " 1234 "), NewAccount("SBI", 0.0, "")),
                creditCards = listOf(
                    NewAccount("Amex", -3200.0, "9876", creditLimit = 150000.0),
                    NewAccount("ICICI", 0.0, creditLimit = 0.0)
                ),
                expenseCategories = listOf("Food", "Rent", " ", "food"),
                incomeCategories = listOf("Salary", "Freelance"),
                sharedCategories = listOf("Loans & Repayments"),
            )
        )

        val accounts = db.accountDao().getAll()
        assertEquals(
            listOf("Cash", "Travel wallet", "HDFC", "SBI", "Amex", "ICICI"),
            accounts.map { it.name })
        assertEquals(
            listOf(
                AccountType.WALLET,
                AccountType.WALLET,
                AccountType.BANK,
                AccountType.BANK,
                AccountType.CREDIT_CARD,
                AccountType.CREDIT_CARD
            ),
            accounts.map { it.accountType },
        )
        assertEquals(listOf(1500.0, 20.5, 12000.0, 0.0, -3200.0, 0.0), accounts.map { it.balance })
        assertEquals("1234", accounts[2].accountNumber)
        assertNull(accounts[3].accountNumber)
        assertEquals(150000.0, accounts[4].creditLimit)
        assertNull(accounts[5].creditLimit)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), accounts.map { it.displayOrder })

        val categories = db.categoryDao().getAll()
        assertEquals(
            listOf("Food", "Rent"),
            categories.filter { it.appliesTo == CategoryScope.EXPENSE }.map { it.name })
        assertEquals(
            listOf("Salary", "Freelance"),
            categories.filter { it.appliesTo == CategoryScope.INCOME }.map { it.name })
        assertEquals(
            listOf("Loans & Repayments"),
            categories.filter { it.appliesTo == CategoryScope.BOTH }.map { it.name })
        assertEquals(
            categories.map { it.displayOrder },
            categories.map { it.displayOrder }.sorted()
        )
    }

    @Test
    fun refusesToSeedTwice() = runTest {
        DefaultData.seed(db)
        val second = runCatching { DefaultData.seed(db) }
        assertTrue(second.exceptionOrNull() is IllegalArgumentException)
        assertEquals(1, db.accountDao().getAll().size)
    }
}
