package com.nagabhushansn95.financetracker.ui.transaction

import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The money fields fill each other in on save by 4.x's rule, and a template overwrites only what it holds. */
class EditorFormTest {
    private fun money(rate: String = "", quantity: String = "", amount: String = "") =
        EditorForm(rate = rate, quantity = quantity, amount = amount).resolvedMoney()

    @Test
    fun rateOrAmountAloneIsOneUnit() {
        assertEquals(EditorForm.Money(250.0, 1.0, 250.0), money(rate = "250"))
        assertEquals(EditorForm.Money(99.5, 1.0, 99.5), money(amount = "99.5"))
    }

    @Test
    fun rateAndAmountGiveAWholeQuantity() {
        assertEquals(EditorForm.Money(30.0, 3.0, 90.0), money(rate = "30", amount = "90"))
        // 100 / 30 = 3.33 → 3; the amount stays as typed even though it is not 3 × 30.
        assertEquals(EditorForm.Money(30.0, 3.0, 100.0), money(rate = "30", amount = "100"))
        // 10 / 30 rounds to nothing; one unit at least.
        assertEquals(EditorForm.Money(30.0, 1.0, 10.0), money(rate = "30", amount = "10"))
    }

    @Test
    fun rateAndQuantityGiveTheAmount() {
        assertEquals(EditorForm.Money(12.5, 4.0, 50.0), money(rate = "12.5", quantity = "4"))
    }

    @Test
    fun amountAndQuantityGiveTheRate() {
        assertEquals(EditorForm.Money(25.0, 4.0, 100.0), money(amount = "100", quantity = "4"))
    }

    @Test
    fun allThreeStayAsTyped() {
        assertEquals(
            EditorForm.Money(30.0, 2.0, 55.0),
            money(rate = "30", quantity = "2", amount = "55")
        )
    }

    @Test
    fun nothingWithoutARateOrAnAmount() {
        assertNull(money())
        assertNull(money(quantity = "3"))
        assertNull(money(rate = "abc", amount = "10"))
        assertEquals(
            "Enter the rate or the amount",
            EditorForm(particulars = "x", quantity = "3").problem(emptyMap())
        )
        assertEquals(
            "Enter a quantity above zero",
            EditorForm(particulars = "x", rate = "10", quantity = "0").problem(emptyMap())
        )
    }

    @Test
    fun theChosenAccountFollowsTheMoneyWhenTheKindChanges() {
        val categories = mapOf(
            2L to Category(
                id = 2,
                name = "Gifts",
                appliesTo = CategoryScope.BOTH,
                displayOrder = 1
            ),
            3L to Category(
                id = 3,
                name = "Salary",
                appliesTo = CategoryScope.INCOME,
                displayOrder = 2
            ),
        )
        val income = EditorForm(kind = TransactionKind.INCOME, toAccountId = 5, categoryId = 2)
        val expense = income.withKind(TransactionKind.EXPENSE, categories)
        assertEquals(5L, expense.fromAccountId)
        assertNull(expense.toAccountId)
        // A category for both kinds stays; one for income only must be chosen again for an expense.
        assertEquals(2L, expense.categoryId)
        assertNull(
            income.copy(categoryId = 3).withKind(TransactionKind.EXPENSE, categories).categoryId
        )
        assertNull(
            income.copy(categoryId = 3).withKind(TransactionKind.TRANSFER, categories).categoryId
        )
        assertNull(income.withKind(TransactionKind.EXPENSE).categoryId) // unknown to the editor: dropped
        val transfer = income.withKind(TransactionKind.TRANSFER)
        assertNull(transfer.fromAccountId)
        assertEquals(5L, transfer.toAccountId)
        assertNull(transfer.categoryId)

        val fromExpense = EditorForm(kind = TransactionKind.EXPENSE, fromAccountId = 8)
        assertEquals(8L, fromExpense.withKind(TransactionKind.INCOME).toAccountId)
        assertEquals(8L, fromExpense.withKind(TransactionKind.TRANSFER).fromAccountId)
        assertNull(fromExpense.withKind(TransactionKind.TRANSFER).toAccountId)

        val both = EditorForm(kind = TransactionKind.TRANSFER, fromAccountId = 1, toAccountId = 9)
        assertEquals(9L, both.withKind(TransactionKind.INCOME).toAccountId)
        assertNull(both.withKind(TransactionKind.INCOME).fromAccountId)
        assertEquals(1L, both.withKind(TransactionKind.EXPENSE).fromAccountId)
        assertNull(both.withKind(TransactionKind.EXPENSE).toAccountId)

        assertEquals(
            "Choose the account",
            EditorForm(particulars = "x", amount = "5", kind = TransactionKind.EXPENSE).problem(
                emptyMap()
            )
        )
    }

    @Test
    fun aTemplateOverwritesOnlyWhatItHolds() {
        val form = EditorForm(
            kind = TransactionKind.EXPENSE,
            particulars = "Petrol",
            fromAccountId = 1,
            categoryId = 7,
            rate = "90",
            quantity = "20",
            amount = "1800",
        )
        val rateOnly =
            Template(particulars = "Petrol", kind = TransactionKind.EXPENSE, rate = 105.0)
        val applied = form.applying(rateOnly)
        assertEquals("105", applied.rate)
        assertEquals("20", applied.quantity)
        assertEquals("1800", applied.amount)
        assertEquals(1L, applied.fromAccountId)
        assertEquals(7L, applied.categoryId)

        val full = Template(
            particulars = "Milk",
            kind = TransactionKind.EXPENSE,
            fromAccountId = 2,
            categoryId = 9,
            rate = 28.0,
            quantity = 2.0
        )
        val milk = form.applying(full)
        assertEquals("Milk", milk.particulars)
        assertEquals(2L, milk.fromAccountId)
        assertEquals(9L, milk.categoryId)
        assertEquals("28", milk.rate)
        assertEquals("2", milk.quantity)
        assertEquals("1800", milk.amount)

        // A transfer template clears the category and keeps the account as the from side.
        val transfer =
            Template(particulars = "To bank", kind = TransactionKind.TRANSFER, toAccountId = 3)
        val moved = form.applying(transfer)
        assertEquals(TransactionKind.TRANSFER, moved.kind)
        assertEquals(1L, moved.fromAccountId)
        assertEquals(3L, moved.toAccountId)
        assertNull(moved.categoryId)
    }
}
