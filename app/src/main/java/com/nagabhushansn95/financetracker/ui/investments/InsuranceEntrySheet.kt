package com.nagabhushansn95.financetracker.ui.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.investments.FdMath
import com.nagabhushansn95.financetracker.data.investments.Insurance
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntry
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntryKind
import com.nagabhushansn95.financetracker.data.investments.PolicyView
import com.nagabhushansn95.financetracker.ui.common.AccountField
import com.nagabhushansn95.financetracker.ui.common.AutocompleteField
import com.nagabhushansn95.financetracker.ui.common.ChoiceDropdown
import com.nagabhushansn95.financetracker.ui.common.DateField
import com.nagabhushansn95.financetracker.ui.common.FormSheet
import com.nagabhushansn95.financetracker.ui.common.NumberField
import java.time.LocalDate

/** The field label for the end of a validity period an entry buys. */
fun validityLabel(kind: ValidityKind) = when (kind) {
    ValidityKind.COVER -> "Covered until"
    ValidityKind.THIRD_PARTY -> "Third-party cover until"
    ValidityKind.OWN_DAMAGE -> "Own-damage cover until"
    ValidityKind.SERVICE -> "Next service due"
    ValidityKind.EMISSION_TEST -> "Emission test valid until"
}

/**
 * One entry on a policy or vehicle: the kind (from the "+" menu, changeable here), date, amount, the bank account
 * the money moves against, what it keeps valid and until when, and the particulars (filled in from the kind unless
 * typed). A premium starts with the policy's premium amount (or the last one paid), a service or test with the last
 * one's amount, the validity with the last period's length, so a routine payment is a glance and a tap. The button
 * reads "Record" for a new entry and "Save" for an existing one, which can also be deleted from here.
 */
@Composable
fun InsuranceEntrySheet(
    view: PolicyView,
    banks: List<Account>,
    defaultBankId: Long?,
    /** Service and testing centres named on any vehicle so far, offered as the place is typed. */
    places: List<String> = emptyList(),
    initialKind: InsuranceEntryKind,
    existing: TransactionEntity?,
    onSave: (InsuranceEntry) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val kinds = Insurance.kindsFor(view.type)
    var kind by remember { mutableStateOf(initialKind) }
    var date by remember { mutableStateOf<LocalDate?>(existing?.date ?: LocalDate.now()) }
    var amount by remember { mutableStateOf(existing?.amount?.let(FdMath::plain) ?: "") }
    var amountTyped by remember { mutableStateOf(existing != null) }
    var bankId by remember {
        mutableStateOf(
            existing?.let { it.fromAccountId ?: it.toAccountId } ?: defaultBankId
            ?: banks.firstOrNull()?.id
        )
    }
    val existingPeriods = existing?.let { view.periodsOf(it) }.orEmpty()
    val lastPremiumPeriods = view.premiums.lastOrNull()?.let { view.periodsOf(it) }
    var provider by remember {
        mutableStateOf(
            existingPeriods.firstNotNullOfOrNull { it.provider }
                ?: lastPremiumPeriods?.firstNotNullOfOrNull { it.provider } ?: view.policy?.provider
                ?: "")
    }
    var reference by remember {
        mutableStateOf(existingPeriods.firstNotNullOfOrNull { it.reference } ?: "")
    }
    // Where a service or test was done lives on the validity it bought, like an insurer on a premium's; a new entry
    // starts with the place of the last one of its kind on this vehicle.
    val placeLabel = Insurance.placeLabel(kind)
    var place by remember(kind) {
        mutableStateOf(
            if (existing != null && existing.let { view.entryKindOf(it) } == kind) existingPeriods.firstNotNullOfOrNull { it.provider }
                ?: ""
            else view.rowsOf(kind).lastOrNull()?.let { Insurance.placeOf(view, it) } ?: ""
        )
    }
    // A health premium names its insurer, policy number and sum insured, since a renewal may change any of them.
    val namesInsurer =
        kind == InsuranceEntryKind.PREMIUM && (view.type == InsuranceType.MOTOR || view.type == InsuranceType.HEALTH)
    var sumInsured by remember {
        mutableStateOf(
            (existingPeriods.firstNotNullOfOrNull { it.sumInsured }
                ?: lastPremiumPeriods?.firstNotNullOfOrNull { it.sumInsured }
                ?: view.policy?.sumAssured)?.let(FdMath::plain) ?: ""
        )
    }
    // Validity ends: typed ones stick; the rest follow the date by the last period's length (a year to begin with).
    val typedEnds = remember {
        mutableStateMapOf<ValidityKind, LocalDate?>().also { m ->
            existingPeriods.forEach {
                m[it.kind] = it.endDate
            }
        }
    }
    val validityKinds = Insurance.validityKindsFor(view.type, kind)
    fun defaultEnd(k: ValidityKind): LocalDate? {
        val from = date ?: return null
        // A vehicle whose last premium bought only one of the two covers keeps that shape.
        if (lastPremiumPeriods != null && (k == ValidityKind.THIRD_PARTY || k == ValidityKind.OWN_DAMAGE) &&
            lastPremiumPeriods.none { it.kind == k }
        ) return null
        val months = view.current(k)?.let { Insurance.periodMonths(it) } ?: 12
        return from.plusMonths(months)
    }

    fun endOf(k: ValidityKind): LocalDate? =
        if (typedEnds.containsKey(k)) typedEnds[k] else defaultEnd(k)

    val defaultAmount: Double? = when (kind) {
        InsuranceEntryKind.PREMIUM -> view.policy?.premiumAmount
            ?: view.premiums.lastOrNull()?.amount

        InsuranceEntryKind.PAYOUT -> null
        else -> view.rowsOf(kind).lastOrNull()?.amount
    }
    val amountShown = if (amountTyped) amount else defaultAmount?.let(FdMath::plain) ?: ""
    var particulars by remember { mutableStateOf(existing?.particulars ?: "") }
    var typed by remember { mutableStateOf(existing != null) }
    val suggested = Insurance.suggestedParticulars(kind, view)
    val value = amountShown.toDoubleOrNull()
    val ends = validityKinds.associateWith { endOf(it) }
    val validityOk = when {
        validityKinds.isEmpty() -> true
        view.type == InsuranceType.MOTOR && kind == InsuranceEntryKind.PREMIUM -> ends.values.any { it != null }
        else -> ends.values.all { it != null }
    }
    val valid = date != null && value != null && value >= 0 && bankId != null && validityOk &&
            (kind != InsuranceEntryKind.PREMIUM || value > 0)
    FormSheet(
        title = if (existing == null) kind.title else "Edit ${kind.title.lowercase()}",
        onDismiss = onDismiss
    ) {
        if (kinds.size > 1) ChoiceDropdown("Entry", kinds, kind, { it.title }) { kind = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Date", date, { date = it }, Modifier.weight(1f))
            NumberField(
                amountShown,
                "Amount",
                { amount = it; amountTyped = true },
                Modifier.weight(1f)
            )
        }
        AccountField(kind.bankLabel, banks, bankId, { bankId = it }, Modifier.fillMaxWidth())
        if (namesInsurer) Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = provider,
                onValueChange = { provider = it },
                label = { Text("Insurer") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Policy number") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (view.type == InsuranceType.HEALTH && kind == InsuranceEntryKind.PREMIUM) {
            NumberField(sumInsured, "Sum insured", { sumInsured = it }, Modifier.fillMaxWidth())
        }
        if (placeLabel != null) AutocompleteField(
            value = place,
            label = placeLabel,
            suggestions = places.filter { it != place.trim() },
            onChange = { place = it },
            modifier = Modifier.fillMaxWidth(),
        )
        validityKinds.forEach { k ->
            DateField(
                validityLabel(k),
                ends[k],
                { typedEnds[k] = it },
                Modifier.fillMaxWidth(),
                clearable = view.type == InsuranceType.MOTOR && kind == InsuranceEntryKind.PREMIUM,
            )
        }
        OutlinedTextField(
            value = if (typed) particulars else suggested,
            onValueChange = { particulars = it; typed = true },
            label = { Text("Particulars") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            when {
                kind == InsuranceEntryKind.PAYOUT -> "Money the policy pays back (survival benefit, bonus, maturity), into the account chosen above."
                kind == InsuranceEntryKind.SERVICE -> "A vehicle expense; the next due date drives the alert on Home. The service centre shows with the row, so the particulars can carry anything else (an AMC bought, say)."
                kind == InsuranceEntryKind.EMISSION_TEST -> "A vehicle expense; the certificate's end date drives the alert on Home. The testing centre shows with the row."
                view.type == InsuranceType.LIFE -> "Paid from the account chosen above into the policy; counts as an investment."
                view.type == InsuranceType.MOTOR -> "An insurance expense. Leave a cover empty when this policy does not include it; at least one is needed."
                view.type == InsuranceType.HEALTH -> "An insurance expense; the cover's end date drives the alert on Home. The sum insured and policy number are this year's; the policy shows the latest. Two or three years paid at once is one entry with a later date."
                else -> "An insurance expense; protection, not an investment."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    InsuranceEntry(
                        kind = kind,
                        date = date!!,
                        amount = value!!,
                        particulars = if (typed) particulars else suggested,
                        bankAccountId = bankId,
                        provider = if (placeLabel != null) place else provider.takeIf { namesInsurer },
                        reference = reference.takeIf { namesInsurer },
                        validUntil = ends,
                        sumInsured = sumInsured.toDoubleOrNull()
                            .takeIf { view.type == InsuranceType.HEALTH && kind == InsuranceEntryKind.PREMIUM },
                    )
                )
            },
        ) { Text(if (existing == null) "Record" else "Save") }
        if (onDelete != null) OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete entry", color = MaterialTheme.colorScheme.error)
        }
    }
}
