package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentDao {
    @Insert
    suspend fun insert(instrument: Instrument): Long

    @Update
    suspend fun update(instrument: Instrument)

    @Query("SELECT * FROM instruments WHERE id = :id")
    suspend fun getById(id: Long): Instrument?

    @Query("SELECT * FROM instruments WHERE isin = :isin LIMIT 1")
    suspend fun getByIsin(isin: String): Instrument?

    @Query("SELECT * FROM instruments WHERE symbol = :symbol AND instrument_type = :type LIMIT 1")
    suspend fun getBySymbol(symbol: String, type: InstrumentType): Instrument?

    @Query("SELECT * FROM instruments WHERE name = :name AND instrument_type = :type LIMIT 1")
    suspend fun getByName(name: String, type: InstrumentType): Instrument?

    @Query("SELECT * FROM instruments ORDER BY display_order, id")
    suspend fun getAll(): List<Instrument>

    @Query("SELECT * FROM instruments WHERE active = 1 ORDER BY display_order, id")
    fun observeActive(): Flow<List<Instrument>>

    @Query("SELECT * FROM instruments WHERE instrument_type = :type ORDER BY display_order, id")
    suspend fun getByType(type: InstrumentType): List<Instrument>

    @Query("SELECT * FROM instruments ORDER BY display_order, id")
    fun observeAll(): Flow<List<Instrument>>

    @Query("SELECT COALESCE(MAX(display_order), 0) FROM instruments")
    suspend fun maxDisplayOrder(): Int

    @Query("UPDATE instruments SET display_order = :order WHERE id = :id")
    suspend fun setDisplayOrder(id: Long, order: Int)

    @Query("UPDATE instruments SET active = :active, modified_time = :modifiedTime WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, modifiedTime: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE instrument_id = :instrumentId")
    suspend fun transactionCount(instrumentId: Long): Int

    // --- fixed deposits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFixedDeposit(fd: FixedDeposit)

    @Query("SELECT * FROM fixed_deposits WHERE instrument_id = :instrumentId")
    suspend fun getFixedDeposit(instrumentId: Long): FixedDeposit?

    @Query("SELECT * FROM fixed_deposits")
    suspend fun getAllFixedDeposits(): List<FixedDeposit>

    // --- insurance
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInsurancePolicy(policy: InsurancePolicy)

    @Query("SELECT * FROM insurance_policies WHERE instrument_id = :instrumentId")
    suspend fun getInsurancePolicy(instrumentId: Long): InsurancePolicy?

    // --- categories
    @Insert
    suspend fun insertCategory(category: InvestmentCategory): Long

    @Update
    suspend fun updateCategory(category: InvestmentCategory)

    @Query("SELECT * FROM investment_categories ORDER BY display_order, id")
    suspend fun getAllCategories(): List<InvestmentCategory>

    @Query("SELECT * FROM investment_categories ORDER BY display_order, id")
    fun observeCategories(): Flow<List<InvestmentCategory>>

    @Query("SELECT COUNT(*) FROM investment_categories")
    suspend fun categoryCount(): Int

    @Query("UPDATE investment_categories SET display_order = :order WHERE id = :id")
    suspend fun setCategoryDisplayOrder(id: Long, order: Int)

    @Query("SELECT COUNT(*) FROM instruments WHERE category_id = :categoryId")
    suspend fun instrumentCountInCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM investment_categories WHERE parent_id = :parentId")
    suspend fun childCategoryCount(parentId: Long): Int

    @Query("DELETE FROM investment_categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT * FROM instrument_valuations WHERE instrument_id = :instrumentId ORDER BY date DESC, id DESC")
    suspend fun valuationHistory(instrumentId: Long): List<InstrumentValuation>

    @Query("SELECT * FROM instrument_valuations ORDER BY date DESC, id DESC")
    fun observeValuations(): Flow<List<InstrumentValuation>>

    @Query("SELECT * FROM investment_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): InvestmentCategory?

    @Query("SELECT COUNT(*) FROM investment_categories WHERE risk_level_id = :riskLevelId")
    suspend fun categoryCountWithRiskLevel(riskLevelId: Long): Int

    // --- risk levels
    @Insert
    suspend fun insertRiskLevel(level: RiskLevel): Long

    @Update
    suspend fun updateRiskLevel(level: RiskLevel)

    @Query("DELETE FROM risk_levels WHERE id = :id")
    suspend fun deleteRiskLevel(id: Long)

    @Query("SELECT * FROM risk_levels ORDER BY display_order, id")
    suspend fun getAllRiskLevels(): List<RiskLevel>

    @Query("SELECT * FROM risk_levels ORDER BY display_order, id")
    fun observeRiskLevels(): Flow<List<RiskLevel>>

    @Query("SELECT COUNT(*) FROM risk_levels")
    suspend fun riskLevelCount(): Int

    @Query("UPDATE risk_levels SET display_order = :order WHERE id = :id")
    suspend fun setRiskLevelDisplayOrder(id: Long, order: Int)

    // --- valuations
    @Insert
    suspend fun insertValuation(valuation: InstrumentValuation): Long

    @Query("SELECT * FROM instrument_valuations WHERE instrument_id = :instrumentId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun latestValuation(instrumentId: Long): InstrumentValuation?

    @Query(
        "SELECT v.* FROM instrument_valuations v " +
                "JOIN (SELECT instrument_id, MAX(date) AS max_date FROM instrument_valuations GROUP BY instrument_id) m " +
                "ON v.instrument_id = m.instrument_id AND v.date = m.max_date"
    )
    suspend fun latestValuations(): List<InstrumentValuation>

    // --- fx
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFxRate(rate: FxRate)

    @Query("SELECT * FROM fx_rates WHERE base = :base AND quote = :quote ORDER BY date DESC LIMIT 1")
    suspend fun latestFxRate(base: String, quote: String): FxRate?
}
