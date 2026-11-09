package com.nagabhushansn95.financetracker.data.investments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class ProvidentFundsFilingTest {
    private lateinit var db: AppDatabase
    private val now = LocalDateTime.of(2026, 11, 6, 9, 0)

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun filesOldContributionsAndCreatesTheCategoryOnce() = runTest {
        val others = db.categoryDao()
            .insert(Category(name = "Others", appliesTo = CategoryScope.BOTH, displayOrder = 1))
        val interest = db.categoryDao().insert(
            Category(
                name = "Interest & Dividends",
                appliesTo = CategoryScope.INCOME,
                displayOrder = 2
            )
        )
        val salary = db.categoryDao()
            .insert(Category(name = "Salary", appliesTo = CategoryScope.INCOME, displayOrder = 3))
        val epf = db.accountDao().insert(
            Account(
                name = "EPF",
                accountType = AccountType.PROVIDENT_FUND,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val bank = db.accountDao().insert(
            Account(
                name = "Bank",
                accountType = AccountType.BANK,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )

        suspend fun row(
            contributor: String?,
            category: Long?,
            to: Long = epf,
            kind: TransactionKind = TransactionKind.INCOME,
            from: Long? = null
        ) =
            db.transactionDao().insert(
                TransactionEntity(
                    createdTime = now,
                    modifiedTime = now,
                    date = LocalDate.of(2017, 3, 31),
                    kind = kind,
                    fromAccountId = from,
                    toAccountId = to,
                    categoryId = category,
                    particulars = "x",
                    amount = 100.0,
                    contributor = contributor
                )
            )

        val employee = row("Employee", others)        // filed from Others
        val employer = row("Employer", null)          // filed from nothing
        val byHand = row("Employee", salary)          // a hand-picked category stays
        val interestRow = row("Interest", null)       // goes under Interest & Dividends
        val plainIncome = row(null, others, to = bank) // not a provident-fund row
        val withdrawal = row(null, null, to = bank, kind = TransactionKind.TRANSFER, from = epf)

        assertEquals(3, ProvidentFunds.fileCategories(db, now))
        val pf = db.categoryDao().getAll().single { it.name == ProvidentFunds.CATEGORY }
        assertEquals(CategoryScope.INCOME, pf.appliesTo)
        assertEquals(pf.id, db.transactionDao().getById(employee)!!.categoryId)
        assertEquals(pf.id, db.transactionDao().getById(employer)!!.categoryId)
        assertEquals(salary, db.transactionDao().getById(byHand)!!.categoryId)
        assertEquals(interest, db.transactionDao().getById(interestRow)!!.categoryId)
        assertEquals(others, db.transactionDao().getById(plainIncome)!!.categoryId)
        assertNull(db.transactionDao().getById(withdrawal)!!.categoryId)

        // A second run changes nothing and adds no category.
        assertEquals(0, ProvidentFunds.fileCategories(db, now))
        assertEquals(1, db.categoryDao().getAll().count { it.name == ProvidentFunds.CATEGORY })
    }
}
