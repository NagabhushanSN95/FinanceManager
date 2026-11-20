package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.nagabhushansn95.financetracker.data.db.entity.HealthCover
import com.nagabhushansn95.financetracker.data.db.entity.Vehicle
import com.nagabhushansn95.financetracker.data.db.entity.VehicleRecord
import kotlinx.coroutines.flow.Flow

/** Vehicles with their records, and the per-year covers of health policies. */
@Dao
interface VehicleDao {
    // --- vehicles
    @Upsert
    suspend fun upsertVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM vehicles WHERE instrument_id = :instrumentId")
    suspend fun getVehicle(instrumentId: Long): Vehicle?

    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehicles(): List<Vehicle>

    @Query("SELECT * FROM vehicles")
    fun observeVehicles(): Flow<List<Vehicle>>

    // --- vehicle records (insurance cover, service, emission test)
    @Insert
    suspend fun insertRecord(record: VehicleRecord): Long

    @Update
    suspend fun updateRecord(record: VehicleRecord)

    @Query("DELETE FROM vehicle_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM vehicle_records WHERE transaction_id = :transactionId")
    suspend fun deleteRecordsOf(transactionId: Long)

    @Query("SELECT * FROM vehicle_records WHERE instrument_id = :instrumentId ORDER BY end_date, id")
    suspend fun getRecords(instrumentId: Long): List<VehicleRecord>

    @Query("SELECT * FROM vehicle_records ORDER BY end_date, id")
    suspend fun getAllRecords(): List<VehicleRecord>

    @Query("SELECT * FROM vehicle_records ORDER BY end_date, id")
    fun observeRecords(): Flow<List<VehicleRecord>>

    // --- health covers
    @Insert
    suspend fun insertCover(cover: HealthCover): Long

    @Update
    suspend fun updateCover(cover: HealthCover)

    @Query("DELETE FROM health_covers WHERE id = :id")
    suspend fun deleteCover(id: Long)

    @Query("DELETE FROM health_covers WHERE transaction_id = :transactionId")
    suspend fun deleteCoversOf(transactionId: Long)

    @Query("SELECT * FROM health_covers WHERE instrument_id = :instrumentId ORDER BY end_date, id")
    suspend fun getCovers(instrumentId: Long): List<HealthCover>

    @Query("SELECT * FROM health_covers ORDER BY end_date, id")
    suspend fun getAllCovers(): List<HealthCover>

    @Query("SELECT * FROM health_covers ORDER BY end_date, id")
    fun observeCovers(): Flow<List<HealthCover>>
}
