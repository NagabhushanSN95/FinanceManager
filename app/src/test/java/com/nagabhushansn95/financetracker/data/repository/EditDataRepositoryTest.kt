package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class EditDataRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: EditDataRepository
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repo = EditDataRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun account(name: String, type: AccountType = AccountType.WALLET) =
        Account(
            name = name,
            accountType = type,
            displayOrder = 0,
            createdTime = now,
            modifiedTime = now
        )

    @Test
    fun addsAtTheEndAndReordersOneGroup() = runTest {
        val a = repo.addAccount(account("A"))
        val b = repo.addAccount(account("B"))
        val bank = repo.addAccount(account("Bank", AccountType.BANK))
        val c = repo.addAccount(account("C"))
        assertEquals(listOf(1, 2, 3, 4), db.accountDao().getAll().map { it.displayOrder })

        // Reorder the wallets only: C, A, B. The bank keeps its slot.
        repo.reorderAccounts(listOf(c, a, b))
        val wallets = db.accountDao().getVisibleByType(AccountType.WALLET).map { it.name }
        assertEquals(listOf("C", "A", "B"), wallets)
        assertEquals(3, db.accountDao().getById(bank)!!.displayOrder)
    }

    @Test
    fun deletedAccountsKeepTheirOrderAndRestoreAtTheEnd() = runTest {
        val a = repo.addAccount(account("A"))
        repo.addAccount(account("B"))
        repo.deleteAccount(a, now)
        assertEquals(listOf("B"), db.accountDao().getVisible().map { it.name })
        assertEquals(1, db.accountDao().getById(a)!!.displayOrder)

        repo.restoreAccount(a, now.plusMinutes(1))
        val visible = db.accountDao().getVisible()
        assertEquals(listOf("B", "A"), visible.map { it.name })
        assertFalse(visible.last().deleted)
        assertEquals(now.plusMinutes(1), visible.last().modifiedTime)
    }

    @Test
    fun parentCategoryWithChildrenCannotBeDeleted() = runTest {
        val food = repo.addCategory(Category(name = "Food", displayOrder = 0))
        val snacks = repo.addCategory(Category(name = "Snacks", parentId = food, displayOrder = 0))
        val error = runCatching { repo.deleteCategory(food) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)

        repo.deleteCategory(snacks)
        repo.deleteCategory(food)
        assertTrue(db.categoryDao().getVisible().isEmpty())

        repo.restoreCategory(food)
        assertEquals(listOf("Food"), db.categoryDao().getVisible().map { it.name })
        assertEquals(3, db.categoryDao().getById(food)!!.displayOrder)
    }

    @Test
    fun reordersCategoriesWithinSiblings() = runTest {
        val x = repo.addCategory(
            Category(
                name = "X",
                appliesTo = CategoryScope.INCOME,
                displayOrder = 0
            )
        )
        val y = repo.addCategory(
            Category(
                name = "Y",
                appliesTo = CategoryScope.INCOME,
                displayOrder = 0
            )
        )
        val z = repo.addCategory(
            Category(
                name = "Z",
                appliesTo = CategoryScope.INCOME,
                displayOrder = 0
            )
        )
        repo.reorderCategories(listOf(z, y, x))
        assertEquals(listOf("Z", "Y", "X"), db.categoryDao().getVisible().map { it.name })
    }
}
