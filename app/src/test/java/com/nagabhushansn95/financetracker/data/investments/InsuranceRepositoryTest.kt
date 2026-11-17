package com.nagabhushansn95.financetracker.data.investments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.TestDb
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.Vehicle
import com.nagabhushansn95.financetracker.data.repository.InsuranceRepository
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
class InsuranceRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: InsuranceRepository
    private val now = LocalDateTime.of(2026, 11, 7, 9, 0)
    private val today = LocalDate.of(2026, 11, 7)
    private var bank = 0L

    @Before
    fun setUp() = runTest {
        db = TestDb.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repo = InsuranceRepository(db)
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
    }

    @After
    fun tearDown() = db.close()

    private suspend fun view(id: Long): PolicyView = Insurance.build(
        db.instrumentDao().getAll(),
        db.instrumentDao().getAllInsurancePolicies(),
        db.transactionDao().getAllWithInstrument(),
        Validity.getAll(db),
        today,
        db.vehicleDao().getAllVehicles(),
    ).single { it.instrument.id == id }

    private fun vehicle(name: String) =
        instrument(name).copy(instrumentType = InstrumentType.VEHICLE)

    private fun instrument(name: String) = Instrument(
        name = name,
        instrumentType = InstrumentType.INSURANCE,
        createdTime = now,
        modifiedTime = now
    )

    @Test
    fun aVehiclePremiumWritesTheRowAndItsCoverAndEditingAndDeletingFollow() = runTest {
        val categories = Insurance.fileCategories(db, now).let { db.categoryDao().getAll() }
        val insurance = categories.single { it.name == Insurance.CATEGORY }
        val vehicleCat = categories.single { it.name == Insurance.VEHICLE_CATEGORY }
        assertEquals(CategoryScope.EXPENSE, insurance.appliesTo)

        val id = repo.savePolicy(
            vehicle("Activa"),
            null,
            manual = mapOf(ValidityKind.SERVICE to LocalDate.of(2026, 12, 1)),
            now = now,
            vehicle = Vehicle(registrationNo = "KA01AB1234"),
        )
        assertEquals("KA01AB1234", view(id).reference)
        assertEquals(InsuranceType.MOTOR, view(id).type)
        var v = view(id)
        assertEquals(LocalDate.of(2026, 12, 1), v.validUntil(ValidityKind.SERVICE))
        assertNull(v.current(ValidityKind.SERVICE)!!.transactionId)

        val txId = repo.saveEntry(
            v,
            InsuranceEntry(
                InsuranceEntryKind.PREMIUM, LocalDate.of(2026, 11, 5), 1300.0, "", bank,
                provider = "New India", reference = "P1",
                validUntil = mapOf(
                    ValidityKind.THIRD_PARTY to LocalDate.of(2027, 11, 4),
                    ValidityKind.OWN_DAMAGE to LocalDate.of(2027, 11, 4)
                )
            ),
            categoryId = insurance.id,
            now = now
        )
        v = view(id)
        val row = db.transactionDao().getById(txId)!!
        assertEquals(TransactionKind.EXPENSE, row.kind)
        assertEquals(insurance.id, row.categoryId)
        assertEquals(98700.0, db.accountDao().getById(bank)!!.balance, 1e-6)
        assertEquals(InsuranceEntryKind.PREMIUM, v.entryKindOf(row))
        assertEquals(2, v.periodsOf(row).size)
        assertEquals("New India", v.current(ValidityKind.THIRD_PARTY)!!.provider)

        // A service with its next due date, filed under Vehicle.
        val serviceId = repo.saveEntry(
            v,
            InsuranceEntry(
                InsuranceEntryKind.SERVICE, LocalDate.of(2026, 11, 6), 800.0, "", bank,
                validUntil = mapOf(ValidityKind.SERVICE to LocalDate.of(2027, 5, 6))
            ),
            categoryId = vehicleCat.id,
            now = now
        )
        v = view(id)
        assertEquals(vehicleCat.id, db.transactionDao().getById(serviceId)!!.categoryId)
        assertEquals(
            InsuranceEntryKind.SERVICE,
            v.entryKindOf(db.transactionDao().getById(serviceId)!!)
        )
        assertEquals(
            LocalDate.of(2027, 5, 6),
            v.validUntil(ValidityKind.SERVICE)
        ) // the entry overtakes the hand-set date
        assertEquals(1300.0, v.paid, 1e-9)
        assertEquals(800.0, v.upkeep, 1e-9)

        // Editing the premium drops the own-damage cover and changes the amount; the old periods are replaced.
        repo.saveEntry(
            v,
            InsuranceEntry(
                InsuranceEntryKind.PREMIUM, LocalDate.of(2026, 11, 5), 1200.0, "typed", bank,
                provider = "New India", reference = "P1",
                validUntil = mapOf(
                    ValidityKind.THIRD_PARTY to LocalDate.of(2027, 11, 4),
                    ValidityKind.OWN_DAMAGE to null
                )
            ),
            categoryId = insurance.id,
            existing = row,
            now = now
        )
        v = view(id)
        assertEquals(1200.0, db.transactionDao().getById(txId)!!.amount, 1e-9)
        assertEquals(98000.0, db.accountDao().getById(bank)!!.balance, 1e-6)
        assertEquals(
            listOf(ValidityKind.THIRD_PARTY),
            v.periodsOf(db.transactionDao().getById(txId)!!).map { it.kind })
        assertNull(v.validUntil(ValidityKind.OWN_DAMAGE))

        // Deleting the premium takes its cover with it and gives the money back.
        repo.deleteEntry(txId, now)
        v = view(id)
        assertNull(db.transactionDao().getById(txId))
        assertNull(v.validUntil(ValidityKind.THIRD_PARTY))
        assertEquals(99200.0, db.accountDao().getById(bank)!!.balance, 1e-6)

        // The policy sheet clears the hand-set service date without touching the entry's.
        repo.savePolicy(
            v.instrument,
            null,
            vehicle = v.vehicle,
            manual = mapOf(ValidityKind.SERVICE to null),
            now = now
        )
        v = view(id)
        assertEquals(LocalDate.of(2027, 5, 6), v.validUntil(ValidityKind.SERVICE))
        assertTrue(Validity.getFor(db, id).all { it.transactionId != null })
    }

    @Test
    fun lifePremiumsAreInvestmentsAndOldExpensePremiumsAreFiled() = runTest {
        val others = db.categoryDao()
            .insert(Category(name = "Others", appliesTo = CategoryScope.BOTH, displayOrder = 1))
        val id = repo.savePolicy(
            instrument("SBI Life - Smart Platina Plus"),
            InsurancePolicy(
                0,
                InsuranceType.LIFE,
                provider = "SBI Life",
                policyName = "Smart Platina Plus",
                premiumAmount = 8500.0,
                premiumFrequencyMonths = 1,
                dueDay = 20
            ),
            now = now
        )
        val termId = repo.savePolicy(
            instrument("HDFC - Click 2 Protect"),
            InsurancePolicy(
                0,
                InsuranceType.TERM,
                provider = "HDFC",
                policyName = "Click 2 Protect",
                premiumAmount = 12000.0,
                premiumFrequencyMonths = 12,
                dueDay = 1,
                dueMonth = 4
            ),
            now = now
        )
        val premium = repo.saveEntry(
            view(id),
            InsuranceEntry(
                InsuranceEntryKind.PREMIUM,
                LocalDate.of(2026, 11, 20),
                8500.0,
                "",
                bank
            ),
            categoryId = null,
            now = now
        )
        assertEquals(TransactionKind.INVEST, db.transactionDao().getById(premium)!!.kind)
        // An old term premium recorded as an expense under Others (before the categories existed).
        val old = db.transactionDao().insert(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.of(2026, 4, 1),
                kind = TransactionKind.EXPENSE,
                fromAccountId = bank,
                categoryId = others,
                instrumentId = termId,
                particulars = "term",
                amount = 12000.0
            )
        )
        assertEquals(1, Insurance.fileCategories(db, now))
        val insurance = db.categoryDao().getAll().single { it.name == Insurance.CATEGORY }
        assertEquals(insurance.id, db.transactionDao().getById(old)!!.categoryId)
        assertEquals(0, Insurance.fileCategories(db, now))
        assertEquals(1, db.categoryDao().getAll().count { it.name == Insurance.CATEGORY })

        // The term policy stays out of the Investment Summary; the life policy is in it.
        val summary = InvestmentSummaryBuilder.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllCategories(),
            db.transactionDao().getAllWithInstrument(),
            emptyList(),
            db.accountDao().getAll(),
            db.transactionDao().getAllWithInstrument(),
            asOf = today,
            insurancePolicies = db.instrumentDao().getAllInsurancePolicies(),
        )
        assertEquals(listOf(id), summary.rows.map { it.instrument.id })
        assertEquals(8500.0, summary.byType[InstrumentType.INSURANCE]!!.invested, 1e-9)
    }

    @Test
    fun deletingAVehicleTakesItsEntriesDatesAndTermsAndPutsTheBalancesBack() = runTest {
        val vehicleCat = Insurance.fileCategories(db, now).let { db.categoryDao().getAll() }
            .single { it.name == Insurance.VEHICLE_CATEGORY }
        val id = repo.savePolicy(
            vehicle("Test car"),
            null,
            manual = mapOf(ValidityKind.THIRD_PARTY to LocalDate.of(2026, 12, 25)),
            now = now,
            vehicle = Vehicle(registrationNo = "KA25AB1234"),
        )
        repo.saveEntry(
            view(id),
            InsuranceEntry(
                InsuranceEntryKind.SERVICE,
                LocalDate.of(2026, 11, 1),
                1500.0,
                "",
                bank,
                provider = "Shantesha Honda",
                validUntil = mapOf(ValidityKind.SERVICE to LocalDate.of(2027, 5, 1))
            ),
            categoryId = vehicleCat.id, now = now
        )
        assertEquals(98500.0, db.accountDao().getById(bank)!!.balance, 1e-9)
        assertEquals(2, Validity.getFor(db, id).size)

        repo.deletePolicy(id, now)
        assertNull(db.instrumentDao().getById(id))
        assertNull(db.vehicleDao().getVehicle(id))
        assertTrue(Validity.getFor(db, id).isEmpty())
        assertTrue(db.transactionDao().getForInstrument(id).isEmpty())
        assertEquals(100000.0, db.accountDao().getById(bank)!!.balance, 1e-9)
    }
}
