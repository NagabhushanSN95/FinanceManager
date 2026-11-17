package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AlertsTest {
    private val now = LocalDateTime.of(2026, 11, 7, 9, 0)
    private val today = LocalDate.of(2026, 11, 7)

    private fun instrument(id: Long, name: String, type: InstrumentType, active: Boolean = true) =
        Instrument(
            id = id,
            name = name,
            instrumentType = type,
            active = active,
            createdTime = now,
            modifiedTime = now
        )

    private fun row(
        id: Long,
        instrumentId: Long,
        kind: TransactionKind,
        date: LocalDate,
        amount: Double
    ) =
        TransactionEntity(
            id = id,
            createdTime = now,
            modifiedTime = now,
            date = date,
            kind = kind,
            fromAccountId = 35,
            instrumentId = instrumentId,
            particulars = "x",
            amount = amount
        )

    @Test
    fun thresholdsFollowHowOftenTheThingRecurs() {
        val t = AlertThresholds()
        assertEquals(7, t.daysFor(1))
        assertEquals(15, t.daysFor(3))
        assertEquals(15, t.daysFor(12))
        assertEquals(30, t.daysFor(24))
        assertEquals("due today", Alerts.phrase(0))
        assertEquals("due in 1 day", Alerts.phrase(1))
        assertEquals("overdue by 3 days", Alerts.phrase(-3))
        assertEquals("ends in 2 days", Alerts.phrase(2, "ends", "ended"))
    }

    @Test
    fun maturedDepositsDuePremiumsAndEndingValidityAreListedSoonestFirst() {
        val fdOpen = instrument(1, "FD #1", InstrumentType.FIXED_DEPOSIT)
        val fdAhead = instrument(2, "FD #2", InstrumentType.FIXED_DEPOSIT)
        val deposits = FixedDeposits.build(
            listOf(fdOpen, fdAhead),
            listOf(
                FixedDeposit(
                    instrumentId = 1,
                    bankName = "SBI",
                    periodMonths = 12,
                    rate = 7.0,
                    maturityDate = LocalDate.of(2026, 11, 4)
                ),
                FixedDeposit(
                    instrumentId = 2,
                    bankName = "SBI",
                    periodMonths = 12,
                    rate = 7.0,
                    maturityDate = LocalDate.of(2027, 11, 4)
                ),
            ),
            listOf(
                row(1, 1, TransactionKind.INVEST, LocalDate.of(2025, 11, 4), 100000.0),
                row(2, 2, TransactionKind.INVEST, LocalDate.of(2026, 11, 4), 100000.0),
            ),
            today
        )
        val monthly = PolicyView(
            instrument(3, "SBI Life - Smart Platina Plus", InstrumentType.INSURANCE),
            InsurancePolicy(
                3,
                InsuranceType.LIFE,
                provider = "SBI Life",
                policyName = "Smart Platina Plus",
                premiumFrequencyMonths = 1,
                dueDay = 12
            ),
            listOf(row(3, 3, TransactionKind.INVEST, LocalDate.of(2026, 10, 12), 8500.0)),
            emptyList(),
            today
        ) // due 12 November: 5 days, within the 7-day run-up
        val yearlyFar = PolicyView(
            instrument(4, "LIC - Micro Bachat", InstrumentType.INSURANCE),
            InsurancePolicy(
                4,
                InsuranceType.LIFE,
                provider = "LIC",
                policyName = "Micro Bachat",
                premiumFrequencyMonths = 12,
                dueDay = 10,
                dueMonth = 3
            ),
            listOf(row(4, 4, TransactionKind.INVEST, LocalDate.of(2026, 3, 10), 5500.0)),
            emptyList(),
            today
        ) // due 10 March 2027: not listed
        val health = PolicyView(
            instrument(5, "Star - Comprehensive", InstrumentType.INSURANCE),
            InsurancePolicy(
                5,
                InsuranceType.HEALTH,
                provider = "Star",
                policyName = "Comprehensive"
            ),
            listOf(row(5, 5, TransactionKind.EXPENSE, LocalDate.of(2024, 12, 1), 24000.0)),
            listOf(
                ValidityPeriod(
                    1,
                    5,
                    ValidityKind.COVER,
                    LocalDate.of(2024, 12, 1),
                    LocalDate.of(2026, 11, 30),
                    5
                )
            ),
            today
        ) // two-year cover ending in 23 days: within the 30-day run-up
        val vehicle = PolicyView(
            instrument(6, "Activa", InstrumentType.VEHICLE),
            null,
            listOf(
                row(6, 6, TransactionKind.EXPENSE, LocalDate.of(2025, 11, 1), 1200.0),
                row(7, 6, TransactionKind.EXPENSE, LocalDate.of(2026, 5, 1), 800.0),
            ),
            listOf(
                ValidityPeriod(
                    2,
                    6,
                    ValidityKind.THIRD_PARTY,
                    LocalDate.of(2025, 11, 1),
                    LocalDate.of(2026, 10, 31),
                    6
                ),
                ValidityPeriod(
                    3,
                    6,
                    ValidityKind.OWN_DAMAGE,
                    LocalDate.of(2025, 11, 1),
                    LocalDate.of(2027, 10, 31),
                    6
                ),
                ValidityPeriod(
                    4,
                    6,
                    ValidityKind.SERVICE,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 11, 15),
                    7
                ),
            ),
            today,
            vehicle = com.nagabhushansn95.financetracker.data.db.entity.Vehicle(6, "KA01AB1234"),
        ) // third-party cover ended a week ago, service due in 8 days, own damage fine
        val sold = PolicyView(
            instrument(7, "Old car", InstrumentType.VEHICLE, active = false),
            null,
            emptyList(),
            listOf(
                ValidityPeriod(
                    5,
                    7,
                    ValidityKind.EMISSION_TEST,
                    null,
                    LocalDate.of(2026, 1, 1)
                )
            ),
            today
        )

        val alerts =
            Alerts.build(deposits, listOf(monthly, yearlyFar, health, vehicle, sold), today)
        assertEquals(
            listOf(
                AlertKind.COVER_ENDS to -7L,       // Activa third-party cover ended
                AlertKind.DEPOSIT_MATURED to -3L,  // FD #1 matured, still open
                AlertKind.PREMIUM_DUE to 5L,       // SBI Life premium
                AlertKind.SERVICE_DUE to 8L,       // Activa service
                AlertKind.COVER_ENDS to 23L,       // Star cover ends
            ),
            alerts.map { it.kind to it.daysLeft }
        )
        assertEquals("Activa · KA01AB1234", alerts[0].title)
        assertEquals("Third-party cover ended by 7 days", alerts[0].detail)
        assertEquals("FD #1 · SBI", alerts[1].title)
        assertEquals("Matured 3 days ago, still open", alerts[1].detail)
        assertEquals("Premium due in 5 days", alerts[2].detail)
        assertEquals(LocalDate.of(2026, 11, 12), alerts[2].date)
        assertTrue(alerts.none { it.instrumentId == 2L || it.instrumentId == 4L || it.instrumentId == 7L })

        // A tighter run-up for monthly premiums drops the premium line.
        val tight = Alerts.build(deposits, listOf(monthly), today, AlertThresholds(monthly = 3))
        assertEquals(listOf(AlertKind.DEPOSIT_MATURED), tight.map { it.kind })
    }
}
