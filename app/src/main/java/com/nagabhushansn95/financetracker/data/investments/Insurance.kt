package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * The entries a policy or vehicle takes from its "+" menu. A premium is an INVEST row on a life policy and an
 * EXPENSE row elsewhere; a payout (life only) is a REDEEM row; a service and an emission test are EXPENSE rows on
 * the vehicle. Money always moves against a bank account. They are recorded and edited only from the policy's
 * screen under Investments; the editor never offers an insurance instrument.
 */
enum class InsuranceEntryKind(val title: String, val outflow: Boolean = false) {
    PREMIUM("Premium"),
    PAYOUT("Payout", outflow = true),
    SERVICE("Service"),
    EMISSION_TEST("Emission test");

    val bankLabel: String get() = if (outflow) "Paid to" else "Paid from"
}

/** A policy (or, for MOTOR, a vehicle) as the screens and the alerts see it. */
data class PolicyView(
    val instrument: Instrument,
    val policy: InsurancePolicy?,
    val rows: List<TransactionEntity>,
    val periods: List<ValidityPeriod>,
    val asOf: LocalDate,
) {
    val type: InsuranceType get() = policy?.insuranceType ?: InsuranceType.LIFE
    private val periodsByTransaction =
        periods.filter { it.transactionId != null }.groupBy { it.transactionId!! }

    /** Which entry a ledger row is, by its kind and the validity it bought; null for a row the sheet cannot describe. */
    fun entryKindOf(t: TransactionEntity): InsuranceEntryKind? = when (t.kind) {
        TransactionKind.REDEEM, TransactionKind.INCOME -> InsuranceEntryKind.PAYOUT
        TransactionKind.INVEST, TransactionKind.EXPENSE -> {
            val kinds = periodsByTransaction[t.id].orEmpty().map { it.kind }
            when {
                ValidityKind.SERVICE in kinds -> InsuranceEntryKind.SERVICE
                ValidityKind.EMISSION_TEST in kinds -> InsuranceEntryKind.EMISSION_TEST
                else -> InsuranceEntryKind.PREMIUM
            }
        }

        else -> null
    }

    fun rowsOf(kind: InsuranceEntryKind): List<TransactionEntity> =
        rows.filter { entryKindOf(it) == kind }

    val premiums: List<TransactionEntity> get() = rowsOf(InsuranceEntryKind.PREMIUM)
    val paid: Double get() = premiums.sumOf { it.amountInr }
    val received: Double get() = rowsOf(InsuranceEntryKind.PAYOUT).sumOf { it.amountInr }

    /** Services and emission tests on a vehicle. */
    val upkeep: Double
        get() = (rowsOf(InsuranceEntryKind.SERVICE) + rowsOf(InsuranceEntryKind.EMISSION_TEST)).sumOf { it.amountInr }
    val lastPremium: LocalDate? get() = premiums.maxOfOrNull { it.date }

    /** Validity bought by [t], by kind. */
    fun periodsOf(t: TransactionEntity): List<ValidityPeriod> = periodsByTransaction[t.id].orEmpty()

    /** The period whose end is furthest out: the one in force (or the last one, once it has ended). */
    fun current(kind: ValidityKind): ValidityPeriod? =
        periods.filter { it.kind == kind }.maxWithOrNull(compareBy({ it.endDate }, { it.id }))

    fun validUntil(kind: ValidityKind): LocalDate? = current(kind)?.endDate

    /** A health policy's sum insured: the cover in force says it (each renewal may change it), else the policy row. */
    val sumInsured: Double? get() = current(ValidityKind.COVER)?.sumInsured ?: policy?.sumAssured

    /** The policy number: a health policy's cover in force carries its year's number, else the policy row's. */
    val policyNumber: String? get() = current(ValidityKind.COVER)?.reference ?: policy?.policyNumber

    /** When the next life or term premium falls due, by the frequency, due day and month and the last premium paid. */
    val nextDue: LocalDate?
        get() {
            val p = policy ?: return null
            if (!type.scheduled) return null
            val day = p.dueDay ?: return null
            return Insurance.nextDue(
                p.premiumFrequencyMonths ?: 12,
                day,
                p.dueMonth,
                lastPremium,
                asOf
            )
        }
    val dueIn: Long? get() = nextDue?.let { ChronoUnit.DAYS.between(asOf, it) }

    /** A life policy past maturity or a term policy past its cover end. */
    val expired: Boolean get() = type.scheduled && policy?.maturityDate?.isBefore(asOf) == true
    val active: Boolean get() = instrument.active && !expired

    val title: String
        get() = if (type == InsuranceType.MOTOR) instrument.name
        else listOfNotNull(policy?.provider, policy?.policyName).joinToString(" · ")
            .ifEmpty { instrument.name }

    /** The vehicle's registration number, or the policy number. */
    val reference: String? get() = if (type == InsuranceType.MOTOR) policy?.vehicleNumber else policyNumber
}

/** What the entry sheet hands back: the row and the validity it buys. */
data class InsuranceEntry(
    val kind: InsuranceEntryKind,
    val date: LocalDate,
    val amount: Double,
    val particulars: String,
    val bankAccountId: Long?,
    /** Insurer of a motor or health premium, or where a service or emission test was done; the policy number. */
    val provider: String? = null,
    val reference: String? = null,
    /** How long what was paid for stays valid, by kind; a null end means that kind was not bought. */
    val validUntil: Map<ValidityKind, LocalDate?> = emptyMap(),
    /** The sum insured a health premium buys for its cover period. */
    val sumInsured: Double? = null,
)

object Insurance {
    /** Expense category of term, health and motor premiums. */
    const val CATEGORY = "Insurance"

    /** Expense category of a vehicle's services and emission tests. */
    const val VEHICLE_CATEGORY = "Vehicle"

    val FREQUENCIES = listOf(1, 3, 6, 12)

    fun frequencyLabel(months: Int?) = when (months) {
        null -> ""
        1 -> "monthly"
        3 -> "quarterly"
        6 -> "half-yearly"
        12 -> "yearly"
        else -> "every $months months"
    }

    /** The entries a policy of [type] offers from its "+", in menu order. */
    fun kindsFor(type: InsuranceType): List<InsuranceEntryKind> = when (type) {
        InsuranceType.LIFE -> listOf(InsuranceEntryKind.PREMIUM, InsuranceEntryKind.PAYOUT)
        InsuranceType.TERM, InsuranceType.HEALTH -> listOf(InsuranceEntryKind.PREMIUM)
        InsuranceType.MOTOR -> listOf(
            InsuranceEntryKind.PREMIUM,
            InsuranceEntryKind.SERVICE,
            InsuranceEntryKind.EMISSION_TEST
        )
    }

    /** The validity an entry of [kind] on a policy of [type] buys, in the order the sheet asks for it. */
    fun validityKindsFor(type: InsuranceType, kind: InsuranceEntryKind): List<ValidityKind> = when {
        type == InsuranceType.HEALTH && kind == InsuranceEntryKind.PREMIUM -> listOf(ValidityKind.COVER)
        type == InsuranceType.MOTOR && kind == InsuranceEntryKind.PREMIUM -> listOf(
            ValidityKind.THIRD_PARTY,
            ValidityKind.OWN_DAMAGE
        )

        type == InsuranceType.MOTOR && kind == InsuranceEntryKind.SERVICE -> listOf(ValidityKind.SERVICE)
        type == InsuranceType.MOTOR && kind == InsuranceEntryKind.EMISSION_TEST -> listOf(
            ValidityKind.EMISSION_TEST
        )

        else -> emptyList()
    }

    /** The validity the policy sheet lets the user set by hand (for policies whose last entry predates the app). */
    fun manualKindsFor(type: InsuranceType): List<ValidityKind> = when (type) {
        InsuranceType.HEALTH -> listOf(ValidityKind.COVER)
        InsuranceType.MOTOR -> listOf(
            ValidityKind.THIRD_PARTY,
            ValidityKind.OWN_DAMAGE,
            ValidityKind.SERVICE,
            ValidityKind.EMISSION_TEST
        )

        else -> emptyList()
    }

    /** The expense category an entry is filed under; null for the kinds that are not expenses. */
    fun categoryNameFor(type: InsuranceType, kind: InsuranceEntryKind): String? = when (kind) {
        InsuranceEntryKind.PREMIUM -> if (type.investment) null else CATEGORY
        InsuranceEntryKind.PAYOUT -> null
        InsuranceEntryKind.SERVICE, InsuranceEntryKind.EMISSION_TEST -> VEHICLE_CATEGORY
    }

    private fun withDay(month: YearMonth, day: Int): LocalDate =
        month.atDay(day.coerceIn(1, month.lengthOfMonth()))

    /**
     * The first due date after the last premium, by the calendar: every month on [dueDay], or every [frequencyMonths]
     * months on [dueDay] of the months aligned with [dueMonth]. A premium paid up to half a cycle early or late still
     * counts for its own due date, so the next one is the first anchor after `lastPremium + half a cycle`. With no
     * premium recorded, the first anchor on or after [today].
     */
    fun nextDue(
        frequencyMonths: Int,
        dueDay: Int,
        dueMonth: Int?,
        lastPremium: LocalDate?,
        today: LocalDate,
    ): LocalDate {
        val step = frequencyMonths.coerceAtLeast(1)
        val after = lastPremium?.plusDays(step * 15L) ?: today.minusDays(1)
        val anchorMonth = dueMonth?.takeIf { step > 1 && it in 1..12 }
        var candidate =
            if (anchorMonth == null) withDay(YearMonth.from(after), dueDay)
            else withDay(YearMonth.of(after.year - 1, anchorMonth), dueDay)
        while (!candidate.isAfter(after)) candidate =
            withDay(YearMonth.from(candidate).plusMonths(step.toLong()), dueDay)
        return candidate
    }

    /**
     * How long a validity period runs, to the nearest month (at least one), so a cover that ends the day before its
     * anniversary still counts as a full year; a period with no start is taken as a year.
     */
    fun periodMonths(period: ValidityPeriod): Long =
        period.startDate?.let { Math.round(ChronoUnit.DAYS.between(it, period.endDate) / 30.4375) }
            ?.coerceAtLeast(1) ?: 12

    /**
     * What the particulars say unless the user types something, in the ledger's house style (the 4.x wording, capital
     * first letters, no date): "Life Insurance Premium - SBI Life - Smart Platina Plus", "Vehicle Service - Honda Brio".
     */
    fun defaultParticulars(kind: InsuranceEntryKind, view: PolicyView): String {
        val name = view.instrument.name.trim()
        return when (kind) {
            InsuranceEntryKind.PREMIUM -> when (view.type) {
                InsuranceType.LIFE -> "Life Insurance Premium - $name"
                InsuranceType.TERM -> "Term Insurance Premium - $name"
                InsuranceType.HEALTH -> "Health Insurance Premium - $name"
                InsuranceType.MOTOR -> "Motor Insurance Premium - $name"
            }

            InsuranceEntryKind.PAYOUT -> "Life Insurance Payout - $name"
            InsuranceEntryKind.SERVICE -> "Vehicle Service - $name"
            InsuranceEntryKind.EMISSION_TEST -> "Vehicle Emission Test - $name"
        }
    }

    /** The field that names where a service or test was done, or null for the other kinds. */
    fun placeLabel(kind: InsuranceEntryKind): String? = when (kind) {
        InsuranceEntryKind.SERVICE -> "Service centre"
        InsuranceEntryKind.EMISSION_TEST -> "Testing centre"
        else -> null
    }

    /** Where the row's service or test was done: the provider of the validity it bought. */
    fun placeOf(view: PolicyView, row: TransactionEntity): String? =
        view.periodsOf(row)
            .firstOrNull { it.kind == ValidityKind.SERVICE || it.kind == ValidityKind.EMISSION_TEST }?.provider

    /**
     * The particulars a new entry starts with: the wording of the last entry of the same kind on this policy, so a
     * series keeps reading the same way whatever it was called, else [defaultParticulars].
     */
    fun suggestedParticulars(kind: InsuranceEntryKind, view: PolicyView): String =
        view.rowsOf(kind).lastOrNull()?.particulars?.trim()?.ifBlank { null } ?: defaultParticulars(
            kind,
            view
        )

    /**
     * The ledger row for an entry. [categoryId] is the expense category (see [categoryNameFor]) and is required for the
     * kinds that are expenses. An [existing] row keeps its id, timestamps and flags; the rest is replaced.
     */
    fun toTransaction(
        view: PolicyView,
        entry: InsuranceEntry,
        categoryId: Long? = null,
        existing: TransactionEntity? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): TransactionEntity {
        val instrument = view.instrument
        require(instrument.instrumentType == InstrumentType.INSURANCE) { "${instrument.name} is not a policy" }
        require(entry.kind in kindsFor(view.type)) { "${entry.kind.title} is not an entry of a ${view.type.title.lowercase()} policy" }
        require(entry.amount >= 0) { "Amount cannot be negative" }
        require(entry.bankAccountId != null) { "Choose the bank account" }
        val kind = when {
            entry.kind == InsuranceEntryKind.PAYOUT -> TransactionKind.REDEEM
            entry.kind == InsuranceEntryKind.PREMIUM && view.type.investment -> TransactionKind.INVEST
            else -> TransactionKind.EXPENSE
        }
        if (kind == TransactionKind.EXPENSE) require(categoryId != null) {
            "The \"${categoryNameFor(view.type, entry.kind)}\" expense category is missing"
        }
        val base = existing ?: TransactionEntity(
            createdTime = now,
            modifiedTime = now,
            date = entry.date,
            kind = kind,
            particulars = entry.particulars,
            amount = entry.amount,
        )
        return base.copy(
            date = entry.date,
            kind = kind,
            fromAccountId = if (entry.kind.outflow) null else entry.bankAccountId,
            toAccountId = if (entry.kind.outflow) entry.bankAccountId else null,
            categoryId = if (kind == TransactionKind.EXPENSE) categoryId else null,
            instrumentId = instrument.id,
            particulars = entry.particulars.trim().ifBlank { defaultParticulars(entry.kind, view) },
            quantity = null,
            rate = null,
            amount = entry.amount,
            currency = instrument.currency,
            fxRate = null,
            contributor = null,
        )
    }

    /** The validity rows an entry buys, tied to the transaction [transactionId]. */
    fun toPeriods(
        view: PolicyView,
        entry: InsuranceEntry,
        transactionId: Long
    ): List<ValidityPeriod> =
        validityKindsFor(view.type, entry.kind).mapNotNull { kind ->
            val end = entry.validUntil[kind] ?: return@mapNotNull null
            ValidityPeriod(
                instrumentId = view.instrument.id,
                kind = kind,
                startDate = entry.date,
                endDate = end,
                transactionId = transactionId,
                provider = entry.provider?.trim()?.ifBlank { null },
                reference = entry.reference?.trim()?.ifBlank { null },
                sumInsured = entry.sumInsured?.takeIf { kind == ValidityKind.COVER && it > 0 },
            )
        }

    /** Builds the views for every INSURANCE instrument. */
    fun build(
        instruments: List<Instrument>,
        policies: List<InsurancePolicy>,
        ledgerWithInstrument: List<TransactionEntity>,
        periods: List<ValidityPeriod>,
        asOf: LocalDate,
    ): List<PolicyView> {
        val byId = policies.associateBy { it.instrumentId }
        val rows = ledgerWithInstrument.groupBy { it.instrumentId }
        val periodsById = periods.groupBy { it.instrumentId }
        return instruments.filter { it.instrumentType == InstrumentType.INSURANCE }.map {
            PolicyView(
                it,
                byId[it.id],
                rows[it.id].orEmpty().sortedWith(compareBy({ r -> r.date }, { r -> r.id })),
                periodsById[it.id].orEmpty(),
                asOf
            )
        }
    }

    private fun List<Category>.expenseCategory(name: String): Category? =
        firstOrNull {
            !it.deleted && it.appliesTo != CategoryScope.INCOME && it.name.equals(
                name,
                ignoreCase = true
            )
        }

    /**
     * Makes sure the [CATEGORY] and [VEHICLE_CATEGORY] expense categories exist and files the premiums on insurance
     * instruments that carry no category or "Others" under [CATEGORY] (services and emission tests under
     * [VEHICLE_CATEGORY]). A category chosen by hand is left alone. Safe to run on every open; returns how many rows
     * were filed.
     */
    suspend fun fileCategories(db: AppDatabase, now: LocalDateTime = LocalDateTime.now()): Int {
        val categories = db.categoryDao().getAll()
        suspend fun ensure(name: String): Long =
            categories.expenseCategory(name)?.id ?: db.categoryDao().insert(
                Category(
                    name = name,
                    appliesTo = CategoryScope.EXPENSE,
                    displayOrder = db.categoryDao().maxDisplayOrder() + 1,
                )
            )

        val insuranceId = ensure(CATEGORY)
        val vehicleId = ensure(VEHICLE_CATEGORY)
        val others =
            categories.filter { it.name.equals("Others", ignoreCase = true) }.map { it.id }.toSet()
        val periods = db.instrumentDao().getAllValidityPeriods()
        val views = build(
            db.instrumentDao().getByType(InstrumentType.INSURANCE),
            db.instrumentDao().getAllInsurancePolicies(),
            db.transactionDao().getAllWithInstrument(),
            periods,
            now.toLocalDate()
        )
        var filed = 0
        for (view in views) for (t in view.rows) {
            if (t.kind != TransactionKind.EXPENSE || (t.categoryId != null && t.categoryId !in others)) continue
            val target = when (view.entryKindOf(t)) {
                InsuranceEntryKind.SERVICE, InsuranceEntryKind.EMISSION_TEST -> vehicleId
                else -> insuranceId
            }
            if (target == t.categoryId) continue
            db.transactionDao().update(t.copy(categoryId = target, modifiedTime = now))
            filed++
        }
        return filed
    }
}
