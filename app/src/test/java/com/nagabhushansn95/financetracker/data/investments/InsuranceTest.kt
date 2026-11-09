package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InsuranceTest {
    private val now = LocalDateTime.of(2026, 11, 7, 9, 0)
    private val today = LocalDate.of(2026, 11, 7)

    private fun instrument(id: Long, name: String) = Instrument(
        id = id,
        name = name,
        instrumentType = InstrumentType.INSURANCE,
        displayOrder = 1,
        createdTime = now,
        modifiedTime = now
    )

    private fun row(
        id: Long,
        instrumentId: Long,
        kind: TransactionKind,
        date: LocalDate,
        amount: Double,
        from: Long? = 35,
        to: Long? = null
    ) = TransactionEntity(
        id = id,
        createdTime = now,
        modifiedTime = now,
        date = date,
        kind = kind,
        fromAccountId = from,
        toAccountId = to,
        instrumentId = instrumentId,
        particulars = "x",
        amount = amount
    )

    private val life = PolicyView(
        instrument(3, "SBI Life - Smart Platina Plus"),
        InsurancePolicy(
            3,
            InsuranceType.LIFE,
            provider = "SBI Life",
            policyName = "Smart Platina Plus",
            premiumAmount = 8500.0,
            premiumFrequencyMonths = 1,
            dueDay = 20
        ),
        listOf(row(1, 3, TransactionKind.INVEST, LocalDate.of(2026, 10, 20), 8500.0)),
        emptyList(),
        today
    )
    private val yearly = PolicyView(
        instrument(47, "LIC - Micro Bachat"),
        InsurancePolicy(
            47,
            InsuranceType.LIFE,
            provider = "LIC",
            policyName = "Micro Bachat",
            premiumAmount = 5500.0,
            premiumFrequencyMonths = 12,
            dueDay = 10,
            dueMonth = 3
        ),
        listOf(row(2, 47, TransactionKind.INVEST, LocalDate.of(2026, 3, 8), 5500.0)),
        emptyList(),
        today
    )
    private val health = PolicyView(
        instrument(50, "Star - Comprehensive"),
        InsurancePolicy(50, InsuranceType.HEALTH, provider = "Star", policyName = "Comprehensive"),
        listOf(row(3, 50, TransactionKind.EXPENSE, LocalDate.of(2025, 1, 10), 24000.0)),
        listOf(
            ValidityPeriod(
                1,
                50,
                ValidityKind.COVER,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2027, 1, 9),
                transactionId = 3
            )
        ),
        today
    )
    private val vehicle = PolicyView(
        instrument(60, "Activa"),
        InsurancePolicy(60, InsuranceType.MOTOR, vehicleNumber = "KA01AB1234"),
        listOf(
            row(
                4,
                60,
                TransactionKind.EXPENSE,
                LocalDate.of(2026, 1, 5),
                1200.0
            ),  // premium, TP only
            row(5, 60, TransactionKind.EXPENSE, LocalDate.of(2026, 6, 1), 800.0),   // service
            row(6, 60, TransactionKind.EXPENSE, LocalDate.of(2026, 9, 1), 100.0),   // emission test
        ),
        listOf(
            ValidityPeriod(
                2,
                60,
                ValidityKind.THIRD_PARTY,
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2027, 1, 4),
                4,
                "New India",
                "P1"
            ),
            ValidityPeriod(
                3,
                60,
                ValidityKind.SERVICE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2027, 6, 1),
                5
            ),
            ValidityPeriod(
                4,
                60,
                ValidityKind.EMISSION_TEST,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 3, 1),
                6
            ),
        ),
        today
    )

    @Test
    fun premiumsFallDueByTheCalendarWithHalfACycleOfGrace() {
        // Monthly on the 20th: the October premium (paid on time) makes the next one 20 November.
        assertEquals(
            LocalDate.of(2026, 11, 20),
            Insurance.nextDue(1, 20, null, LocalDate.of(2026, 10, 20), today)
        )
        // Paid early (15th) or late (25th), it still counts for the 20th: next is 20 December.
        assertEquals(
            LocalDate.of(2026, 12, 20),
            Insurance.nextDue(1, 20, null, LocalDate.of(2026, 11, 15), today)
        )
        assertEquals(
            LocalDate.of(2026, 12, 20),
            Insurance.nextDue(1, 20, null, LocalDate.of(2026, 11, 25), today)
        )
        // Day 31 clamps in short months.
        assertEquals(
            LocalDate.of(2026, 11, 30),
            Insurance.nextDue(1, 31, null, LocalDate.of(2026, 10, 31), today)
        )
        // Yearly on 10 March, last paid 8 March 2026: next is 10 March 2027.
        assertEquals(
            LocalDate.of(2027, 3, 10),
            Insurance.nextDue(12, 10, 3, LocalDate.of(2026, 3, 8), today)
        )
        // Quarterly anchored on 10 March: Mar, Jun, Sep, Dec. Last paid 12 September: next 10 December.
        assertEquals(
            LocalDate.of(2026, 12, 10),
            Insurance.nextDue(3, 10, 3, LocalDate.of(2026, 9, 12), today)
        )
        // Half-yearly anchored on 1 April: Apr and Oct. Nothing paid yet: the first anchor on or after today.
        assertEquals(LocalDate.of(2027, 4, 1), Insurance.nextDue(6, 1, 4, null, today))
        assertEquals(LocalDate.of(2026, 11, 7), Insurance.nextDue(1, 7, null, null, today))
        // Through the view.
        assertEquals(LocalDate.of(2026, 11, 20), life.nextDue)
        assertEquals(13L, life.dueIn)
        assertEquals(LocalDate.of(2027, 3, 10), yearly.nextDue)
        assertNull(health.nextDue)
    }

    @Test
    fun eachTypeOffersItsOwnEntriesAndValidity() {
        assertEquals(
            listOf(InsuranceEntryKind.PREMIUM, InsuranceEntryKind.PAYOUT),
            Insurance.kindsFor(InsuranceType.LIFE)
        )
        assertEquals(listOf(InsuranceEntryKind.PREMIUM), Insurance.kindsFor(InsuranceType.TERM))
        assertEquals(listOf(InsuranceEntryKind.PREMIUM), Insurance.kindsFor(InsuranceType.HEALTH))
        assertEquals(
            listOf(
                InsuranceEntryKind.PREMIUM,
                InsuranceEntryKind.SERVICE,
                InsuranceEntryKind.EMISSION_TEST
            ),
            Insurance.kindsFor(InsuranceType.MOTOR)
        )
        assertEquals(
            listOf(ValidityKind.COVER),
            Insurance.validityKindsFor(InsuranceType.HEALTH, InsuranceEntryKind.PREMIUM)
        )
        assertEquals(
            listOf(ValidityKind.THIRD_PARTY, ValidityKind.OWN_DAMAGE),
            Insurance.validityKindsFor(InsuranceType.MOTOR, InsuranceEntryKind.PREMIUM)
        )
        assertEquals(
            emptyList<ValidityKind>(),
            Insurance.validityKindsFor(InsuranceType.LIFE, InsuranceEntryKind.PREMIUM)
        )
        assertNull(Insurance.categoryNameFor(InsuranceType.LIFE, InsuranceEntryKind.PREMIUM))
        assertEquals(
            "Insurance",
            Insurance.categoryNameFor(InsuranceType.TERM, InsuranceEntryKind.PREMIUM)
        )
        assertEquals(
            "Insurance",
            Insurance.categoryNameFor(InsuranceType.MOTOR, InsuranceEntryKind.PREMIUM)
        )
        assertEquals(
            "Vehicle",
            Insurance.categoryNameFor(InsuranceType.MOTOR, InsuranceEntryKind.SERVICE)
        )
    }

    @Test
    fun rowsAreRecognisedByKindAndByTheValidityTheyBought() {
        assertEquals(InsuranceEntryKind.PREMIUM, vehicle.entryKindOf(vehicle.rows[0]))
        assertEquals(InsuranceEntryKind.SERVICE, vehicle.entryKindOf(vehicle.rows[1]))
        assertEquals(InsuranceEntryKind.EMISSION_TEST, vehicle.entryKindOf(vehicle.rows[2]))
        assertEquals(1200.0, vehicle.paid, 1e-9)
        assertEquals(900.0, vehicle.upkeep, 1e-9)
        assertEquals(LocalDate.of(2027, 1, 4), vehicle.validUntil(ValidityKind.THIRD_PARTY))
        assertNull(vehicle.validUntil(ValidityKind.OWN_DAMAGE))
        assertEquals("New India", vehicle.current(ValidityKind.THIRD_PARTY)!!.provider)
        assertEquals(LocalDate.of(2027, 1, 9), health.validUntil(ValidityKind.COVER))
        assertEquals("SBI Life · Smart Platina Plus", life.title)
        assertEquals("Activa", vehicle.title)
        assertEquals("KA01AB1234", vehicle.reference)
    }

    @Test
    fun premiumsAreInvestRowsOnLifeAndExpensesElsewhere() {
        val date = LocalDate.of(2026, 11, 20)
        val lifePremium = Insurance.toTransaction(
            life,
            InsuranceEntry(InsuranceEntryKind.PREMIUM, date, 8500.0, "", 35),
            now = now
        )
        assertEquals(TransactionKind.INVEST, lifePremium.kind)
        assertEquals(35L, lifePremium.fromAccountId)
        assertNull(lifePremium.toAccountId)
        assertEquals(3L, lifePremium.instrumentId)
        assertNull(lifePremium.categoryId)
        assertEquals(
            "Life Insurance Premium - SBI Life - Smart Platina Plus",
            lifePremium.particulars
        )
        // A new entry continues the wording of the policy's last entry of the same kind.
        assertEquals(
            life.rows.last().particulars,
            Insurance.suggestedParticulars(InsuranceEntryKind.PREMIUM, life)
        )
        assertEquals(
            "Life Insurance Payout - SBI Life - Smart Platina Plus",
            Insurance.suggestedParticulars(InsuranceEntryKind.PAYOUT, life)
        )

        val payout = Insurance.toTransaction(
            life,
            InsuranceEntry(InsuranceEntryKind.PAYOUT, date, 20000.0, "", 35),
            now = now
        )
        assertEquals(TransactionKind.REDEEM, payout.kind)
        assertEquals(35L, payout.toAccountId)
        assertNull(payout.fromAccountId)

        val healthEntry = InsuranceEntry(
            InsuranceEntryKind.PREMIUM,
            date,
            25000.0,
            "",
            35,
            provider = "Star",
            reference = "H-2026",
            validUntil = mapOf(ValidityKind.COVER to LocalDate.of(2028, 11, 19)),
            sumInsured = 1500000.0
        )
        val healthPremium = Insurance.toTransaction(health, healthEntry, categoryId = 90, now = now)
        assertEquals(TransactionKind.EXPENSE, healthPremium.kind)
        assertEquals(90L, healthPremium.categoryId)
        assertEquals(35L, healthPremium.fromAccountId)
        // The cover period carries the year's sum insured and policy number; the view reads the one in force.
        val cover = Insurance.toPeriods(health, healthEntry, 78).single()
        assertEquals(1500000.0, cover.sumInsured!!, 1e-9)
        assertEquals("H-2026", cover.reference)
        assertNull(health.sumInsured)   // nothing on the policy row or its periods yet
        val renewed = health.copy(periods = health.periods + cover)
        assertEquals(1500000.0, renewed.sumInsured!!, 1e-9)
        assertEquals("H-2026", renewed.policyNumber)
        assertEquals("H-2026", renewed.reference)

        val motorEntry = InsuranceEntry(
            InsuranceEntryKind.PREMIUM,
            date,
            1300.0,
            "",
            35,
            provider = "New India",
            reference = "P2",
            validUntil = mapOf(
                ValidityKind.THIRD_PARTY to LocalDate.of(2027, 11, 19),
                ValidityKind.OWN_DAMAGE to null
            )
        )
        val periods = Insurance.toPeriods(vehicle, motorEntry, 77)
        assertEquals(1, periods.size)
        assertEquals(ValidityKind.THIRD_PARTY, periods[0].kind)
        assertEquals(77L, periods[0].transactionId)
        assertEquals(date, periods[0].startDate)
        assertEquals("New India", periods[0].provider)
        assertEquals("P2", periods[0].reference)
        assertEquals(
            "Motor Insurance Premium - Activa",
            Insurance.toTransaction(vehicle, motorEntry, 90, now = now).particulars
        )

        // A service names where it was done; that rides on its validity row and reads back through placeOf.
        val serviceEntry = InsuranceEntry(
            InsuranceEntryKind.SERVICE, date, 650.0, "", 35, provider = "Shantesha Honda",
            validUntil = mapOf(ValidityKind.SERVICE to date.plusMonths(6))
        )
        val servicePeriods = Insurance.toPeriods(vehicle, serviceEntry, vehicle.rows[1].id)
        assertEquals(listOf(ValidityKind.SERVICE), servicePeriods.map { it.kind })
        assertEquals("Shantesha Honda", servicePeriods[0].provider)
        val served =
            vehicle.copy(periods = vehicle.periods.filter { it.transactionId != vehicle.rows[1].id } + servicePeriods)
        assertEquals("Shantesha Honda", Insurance.placeOf(served, vehicle.rows[1]))
        assertEquals(null, Insurance.placeOf(served, vehicle.rows[0]))
        assertEquals("Service centre", Insurance.placeLabel(InsuranceEntryKind.SERVICE))
        assertEquals(null, Insurance.placeLabel(InsuranceEntryKind.PREMIUM))

        // Editing keeps the id and rewrites the rest.
        val edited = Insurance.toTransaction(
            life,
            InsuranceEntry(InsuranceEntryKind.PREMIUM, date.plusDays(1), 9000.0, "typed", 36),
            existing = life.rows[0],
            now = now
        )
        assertEquals(1L, edited.id)
        assertEquals(9000.0, edited.amount, 1e-9)
        assertEquals("typed", edited.particulars)
        assertEquals(36L, edited.fromAccountId)

        // No bank, no expense category, or an entry the type does not take.
        assertThrows(IllegalArgumentException::class.java) {
            Insurance.toTransaction(
                life,
                InsuranceEntry(InsuranceEntryKind.PREMIUM, date, 100.0, "", null),
                now = now
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Insurance.toTransaction(
                health,
                InsuranceEntry(InsuranceEntryKind.PREMIUM, date, 100.0, "", 35),
                now = now
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Insurance.toTransaction(
                health,
                InsuranceEntry(InsuranceEntryKind.PAYOUT, date, 100.0, "", 35),
                now = now
            )
        }
    }

    @Test
    fun validityLengthDrivesTheThresholdBucket() {
        assertEquals(24L, Insurance.periodMonths(health.periods[0]))
        assertEquals(6L, Insurance.periodMonths(vehicle.periods[2]))
        assertEquals(
            12L,
            Insurance.periodMonths(
                ValidityPeriod(
                    instrumentId = 1,
                    kind = ValidityKind.COVER,
                    endDate = today
                )
            )
        )
    }
}
