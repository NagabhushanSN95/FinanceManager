package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ProvidentFundsTest {
    private val now = LocalDateTime.of(2026, 11, 6, 9, 0)
    private fun account(id: Long, name: String, type: AccountType = AccountType.PROVIDENT_FUND) =
        Account(
            id = id,
            name = name,
            accountType = type,
            displayOrder = 1,
            createdTime = now,
            modifiedTime = now
        )

    private val epf = account(58, "EPF")
    private val ppf = account(59, "PPF")
    private val bank = account(35, "State Bank Of India", AccountType.BANK)

    @Test
    fun eachAccountOffersItsOwnEntries() {
        assertEquals(
            listOf(
                PfEntryKind.EMPLOYEE_CONTRIBUTION,
                PfEntryKind.EMPLOYER_CONTRIBUTION,
                PfEntryKind.VOLUNTARY_CONTRIBUTION,
                PfEntryKind.INTEREST_CREDIT,
                PfEntryKind.TRANSFER_IN,
                PfEntryKind.WITHDRAWAL
            ), ProvidentFunds.kindsFor(epf)
        )
        assertEquals(
            listOf(
                PfEntryKind.SELF_CONTRIBUTION,
                PfEntryKind.INTEREST_CREDIT,
                PfEntryKind.WITHDRAWAL
            ),
            ProvidentFunds.kindsFor(ppf)
        )
        assertEquals(
            listOf(
                PfEntryKind.SELF_CONTRIBUTION,
                PfEntryKind.INTEREST_CREDIT,
                PfEntryKind.WITHDRAWAL
            ),
            ProvidentFunds.kindsFor(account(60, "Public Provident Fund"))
        )
    }

    @Test
    fun contributionsAreIncomeRowsAndTransfersMoveAgainstTheBank() {
        val date = LocalDate.of(2026, 10, 31)
        val employer = ProvidentFunds.toTransaction(
            PfEntryKind.EMPLOYER_CONTRIBUTION,
            epf,
            date,
            1800.0,
            "",
            null,
            categoryId = 40,
            now = now
        )
        assertEquals(TransactionKind.INCOME, employer.kind)
        assertEquals(40L, employer.categoryId)
        assertEquals(58L, employer.toAccountId)
        assertNull(employer.fromAccountId)
        assertEquals("Employer", employer.contributor)
        assertEquals(
            "Invested In Employees Provident Fund - Employer Contribution",
            employer.particulars
        )

        val vpf = ProvidentFunds.toTransaction(
            PfEntryKind.VOLUNTARY_CONTRIBUTION,
            epf,
            date,
            500.0,
            "",
            null,
            now = now
        )
        assertEquals("Employee", vpf.contributor)
        assertEquals("Invested In Employees Provident Fund - VPF Contribution", vpf.particulars)

        val interest = ProvidentFunds.toTransaction(
            PfEntryKind.INTEREST_CREDIT,
            ppf,
            LocalDate.of(2026, 3, 31),
            16215.0,
            "",
            null,
            now = now
        )
        assertEquals("Interest", interest.contributor)
        assertEquals("Public Provident Fund Interest Credited", interest.particulars)
        // A new entry continues the wording of the last one of its kind on the account; otherwise the house style.
        val old = interest.copy(id = 9, particulars = "PPF Interest For FY 2025-26")
        assertEquals(
            "PPF Interest For FY 2025-26",
            ProvidentFunds.suggestedParticulars(PfEntryKind.INTEREST_CREDIT, ppf, listOf(old))
        )
        assertEquals(
            "Invested In Public Provident Fund - Self",
            ProvidentFunds.suggestedParticulars(PfEntryKind.SELF_CONTRIBUTION, ppf, listOf(old))
        )

        val deposit = ProvidentFunds.toTransaction(
            PfEntryKind.SELF_CONTRIBUTION,
            ppf,
            date,
            12500.0,
            "PPF Oct",
            35,
            categoryId = 40,
            now = now
        )
        assertEquals(TransactionKind.TRANSFER, deposit.kind)
        assertNull(deposit.categoryId)   // a transfer carries no category
        assertEquals(35L, deposit.fromAccountId)
        assertEquals(59L, deposit.toAccountId)
        assertEquals("Self", deposit.contributor)
        assertEquals("PPF Oct", deposit.particulars)

        val out = ProvidentFunds.toTransaction(
            PfEntryKind.WITHDRAWAL,
            epf,
            date,
            50000.0,
            "",
            35,
            now = now
        )
        assertEquals(TransactionKind.TRANSFER, out.kind)
        assertEquals(58L, out.fromAccountId)
        assertEquals(35L, out.toAccountId)
        assertNull(out.contributor)

        assertThrows(IllegalArgumentException::class.java) {
            ProvidentFunds.toTransaction(
                PfEntryKind.SELF_CONTRIBUTION,
                ppf,
                date,
                100.0,
                "",
                null,
                now = now
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProvidentFunds.toTransaction(
                PfEntryKind.INTEREST_CREDIT,
                bank,
                date,
                100.0,
                "",
                null,
                now = now
            )
        }
    }

    @Test
    fun entriesFileUnderTheProvidentFundOrInterestCategory() {
        assertEquals(
            "Provident Fund",
            ProvidentFunds.categoryNameFor(PfEntryKind.EMPLOYER_CONTRIBUTION)
        )
        assertEquals("Provident Fund", ProvidentFunds.categoryNameFor(PfEntryKind.TRANSFER_IN))
        assertEquals(
            "Interest & Dividends",
            ProvidentFunds.categoryNameFor(PfEntryKind.INTEREST_CREDIT)
        )
        assertNull(ProvidentFunds.categoryNameFor(PfEntryKind.WITHDRAWAL))
    }

    @Test
    fun existingRowsAreRecognisedAndKeptWhenEdited() {
        val date = LocalDate.of(2026, 10, 31)
        val kinds = ProvidentFunds.kindsFor(epf) + ProvidentFunds.kindsFor(ppf)
        for (kind in kinds) {
            val account = if (kind.epf) epf else ppf
            val row = ProvidentFunds.toTransaction(kind, account, date, 100.0, "", 35, now = now)
                .copy(id = 7)
            assertEquals(kind, ProvidentFunds.entryKindOf(row, account.id))
            // Editing keeps the id and rewrites the rest.
            val edited = ProvidentFunds.toTransaction(
                kind,
                account,
                date.plusDays(1),
                200.0,
                "typed",
                35,
                existing = row,
                now = now
            )
            assertEquals(7L, edited.id)
            assertEquals(200.0, edited.amount, 1e-9)
            assertEquals("typed", edited.particulars)
        }
        // A row that is not on the account, or a plain income with no contributor, is not an entry.
        val stray = ProvidentFunds.toTransaction(
            PfEntryKind.INTEREST_CREDIT,
            epf,
            date,
            1.0,
            "",
            null,
            now = now
        )
        assertNull(ProvidentFunds.entryKindOf(stray, ppf.id))
        assertNull(ProvidentFunds.entryKindOf(stray.copy(contributor = null), epf.id))
    }
}
