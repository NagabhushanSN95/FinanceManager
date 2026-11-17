package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** A vehicle (an instrument of kind VEHICLE): its insurance, services and emission tests are [VehicleRecord]s. */
@Entity(
    tableName = "vehicles",
    foreignKeys = [ForeignKey(
        entity = Instrument::class,
        parentColumns = ["id"],
        childColumns = ["instrument_id"],
        onDelete = ForeignKey.CASCADE
    )],
)
data class Vehicle(
    @PrimaryKey @ColumnInfo(name = "instrument_id") val instrumentId: Long = 0,
    @ColumnInfo(name = "registration_no") val registrationNo: String? = null,
    @ColumnInfo(name = "make_model") val makeModel: String? = null,
    @ColumnInfo(name = "bought_on") val boughtOn: LocalDate? = null,
)

/** What a vehicle record covers. */
enum class VehicleRecordKind(val validity: ValidityKind) {
    OWN_DAMAGE(ValidityKind.OWN_DAMAGE),
    THIRD_PARTY(ValidityKind.THIRD_PARTY),
    SERVICE(ValidityKind.SERVICE),
    EMISSION_TEST(ValidityKind.EMISSION_TEST);

    companion object {
        fun of(kind: ValidityKind): VehicleRecordKind =
            entries.firstOrNull { it.validity == kind }
                ?: throw IllegalArgumentException("$kind is not a vehicle record")
    }
}

/**
 * One thing done for a vehicle and how long it holds: an own-damage or third-party cover (with the insurer and the
 * policy number), a service (where, and when the next is due), an emission test (where, and until when). Usually
 * written with the entry that paid for it and removed with it; without one it was set by hand on the vehicle sheet.
 */
@Entity(
    tableName = "vehicle_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["instrument_id"],
            childColumns = ["instrument_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index(value = ["instrument_id", "kind"]), Index("transaction_id")]
)
data class VehicleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    val kind: VehicleRecordKind,
    @ColumnInfo(name = "start_date") val startDate: LocalDate? = null,
    @ColumnInfo(name = "end_date") val endDate: LocalDate,
    /** The insurer, the garage or the testing centre. */
    val provider: String? = null,
    /** The policy number, or the invoice. */
    val reference: String? = null,
    @ColumnInfo(name = "transaction_id") val transactionId: Long? = null,
) {
    fun toPeriod() = ValidityPeriod(
        id = id,
        instrumentId = instrumentId,
        kind = kind.validity,
        startDate = startDate,
        endDate = endDate,
        transactionId = transactionId,
        provider = provider,
        reference = reference,
    )
}

/**
 * One year's cover of a health policy: the insurer, the policy number and the sum insured that premium bought, and
 * how long it runs. Each renewal keeps its own terms; the policy row shows the latest.
 */
@Entity(
    tableName = "health_covers",
    foreignKeys = [
        ForeignKey(
            entity = InsurancePolicy::class,
            parentColumns = ["instrument_id"],
            childColumns = ["instrument_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index("instrument_id"), Index("transaction_id")]
)
data class HealthCover(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    @ColumnInfo(name = "start_date") val startDate: LocalDate? = null,
    @ColumnInfo(name = "end_date") val endDate: LocalDate,
    val provider: String? = null,
    val reference: String? = null,
    @ColumnInfo(name = "sum_insured") val sumInsured: Double? = null,
    @ColumnInfo(name = "transaction_id") val transactionId: Long? = null,
) {
    fun toPeriod() = ValidityPeriod(
        id = id,
        instrumentId = instrumentId,
        kind = ValidityKind.COVER,
        startDate = startDate,
        endDate = endDate,
        transactionId = transactionId,
        provider = provider,
        reference = reference,
        sumInsured = sumInsured,
    )
}
