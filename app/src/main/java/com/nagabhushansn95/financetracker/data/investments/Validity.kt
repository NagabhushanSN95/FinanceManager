package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The screens' one view of the two validity tables: a health policy's covers (`health_covers`) and a vehicle's
 * records (`vehicle_records`). Reads merge them; writes go to the table the period's kind belongs to.
 */
object Validity {
    fun observeAll(db: AppDatabase): Flow<List<ValidityPeriod>> =
        combine(
            db.vehicleDao().observeCovers(),
            db.vehicleDao().observeRecords()
        ) { covers, records ->
            covers.map { it.toPeriod() } + records.map { it.toPeriod() }
        }

    suspend fun getAll(db: AppDatabase): List<ValidityPeriod> =
        db.vehicleDao().getAllCovers().map { it.toPeriod() } + db.vehicleDao().getAllRecords()
            .map { it.toPeriod() }

    suspend fun getFor(db: AppDatabase, instrumentId: Long): List<ValidityPeriod> =
        db.vehicleDao().getCovers(instrumentId).map { it.toPeriod() } +
                db.vehicleDao().getRecords(instrumentId).map { it.toPeriod() }

    suspend fun insert(db: AppDatabase, period: ValidityPeriod): Long =
        if (period.kind == ValidityKind.COVER) db.vehicleDao().insertCover(period.toHealthCover())
        else db.vehicleDao().insertRecord(period.toVehicleRecord())

    suspend fun update(db: AppDatabase, period: ValidityPeriod) =
        if (period.kind == ValidityKind.COVER) db.vehicleDao().updateCover(period.toHealthCover())
        else db.vehicleDao().updateRecord(period.toVehicleRecord())

    suspend fun delete(db: AppDatabase, period: ValidityPeriod) =
        if (period.kind == ValidityKind.COVER) db.vehicleDao().deleteCover(period.id)
        else db.vehicleDao().deleteRecord(period.id)

    /** Removes every period the entry [transactionId] bought, whichever table it is in. */
    suspend fun deleteOf(db: AppDatabase, transactionId: Long) {
        db.vehicleDao().deleteCoversOf(transactionId)
        db.vehicleDao().deleteRecordsOf(transactionId)
    }
}
