package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.nagabhushansn95.financetracker.data.db.entity.FdTemplate
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.Listed
import com.nagabhushansn95.financetracker.data.db.entity.PriceSource
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel
import com.nagabhushansn95.financetracker.data.db.entity.Security
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentDao {
    @Insert
    suspend fun insert(instrument: Instrument): Long

    @Update
    suspend fun update(instrument: Instrument)

    @Query("SELECT * FROM instruments WHERE id = :id")
    suspend fun getById(id: Long): Instrument?

    @Query("SELECT * FROM instruments ORDER BY id")
    suspend fun getAll(): List<Instrument>

    @Query("SELECT * FROM instruments WHERE kind = :type ORDER BY id")
    suspend fun getByType(type: InstrumentType): List<Instrument>

    @Query("SELECT * FROM instruments ORDER BY id")
    fun observeAll(): Flow<List<Instrument>>

    // --- securities (stocks, ETFs, funds)
    @Upsert
    suspend fun upsertSecurity(security: Security)

    @Query("SELECT * FROM securities WHERE instrument_id = :instrumentId")
    suspend fun getSecurity(instrumentId: Long): Security?

    @Query("SELECT * FROM securities")
    suspend fun getAllSecurities(): List<Security>

    @Query("SELECT * FROM securities")
    fun observeSecurities(): Flow<List<Security>>

    @Query("SELECT i.*, s.* FROM instruments i JOIN securities s ON s.instrument_id = i.id ORDER BY i.id")
    suspend fun getListed(): List<Listed>

    @Query("SELECT i.*, s.* FROM instruments i JOIN securities s ON s.instrument_id = i.id WHERE i.id = :id")
    suspend fun getListed(id: Long): Listed?

    @Query("SELECT i.*, s.* FROM instruments i JOIN securities s ON s.instrument_id = i.id ORDER BY i.id")
    fun observeListed(): Flow<List<Listed>>

    @Query("UPDATE securities SET last_price = :price, last_price_date = :date, last_price_source = :source, last_price_fetched_at = :fetchedAt WHERE instrument_id = :instrumentId")
    suspend fun setPrice(
        instrumentId: Long,
        price: Double,
        date: String,
        source: PriceSource,
        fetchedAt: String
    )

    @Query("UPDATE instruments SET active = :active, modified_time = :modifiedTime WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, modifiedTime: String)

    @Query("DELETE FROM instruments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE fixed_deposits SET previous_fd_id = :into WHERE previous_fd_id = :from")
    suspend fun reassignPreviousFd(from: Long, into: Long)

    @Query("UPDATE fixed_deposits SET next_fd_id = :into WHERE next_fd_id = :from")
    suspend fun reassignNextFd(from: Long, into: Long)

    // --- fixed deposits
    @Upsert
    suspend fun upsertFixedDeposit(fd: FixedDeposit)

    @Query("SELECT * FROM fixed_deposits WHERE instrument_id = :instrumentId")
    suspend fun getFixedDeposit(instrumentId: Long): FixedDeposit?

    @Query("SELECT * FROM fixed_deposits")
    suspend fun getAllFixedDeposits(): List<FixedDeposit>

    @Query("SELECT * FROM fixed_deposits")
    fun observeFixedDeposits(): Flow<List<FixedDeposit>>

    // --- fixed deposit templates
    @Insert
    suspend fun insertFdTemplate(template: FdTemplate): Long

    @Update
    suspend fun updateFdTemplate(template: FdTemplate)

    @Query("DELETE FROM fd_templates WHERE id = :id")
    suspend fun deleteFdTemplate(id: Long)

    @Query("SELECT * FROM fd_templates ORDER BY display_order, name")
    suspend fun getAllFdTemplates(): List<FdTemplate>

    @Query("SELECT * FROM fd_templates ORDER BY display_order, name")
    fun observeFdTemplates(): Flow<List<FdTemplate>>

    @Query("SELECT COALESCE(MAX(display_order), 0) FROM fd_templates")
    suspend fun maxFdTemplateOrder(): Int

    // --- insurance
    @Upsert
    suspend fun upsertInsurancePolicy(policy: InsurancePolicy)

    @Query("SELECT * FROM insurance_policies WHERE instrument_id = :instrumentId")
    suspend fun getInsurancePolicy(instrumentId: Long): InsurancePolicy?

    @Query("SELECT * FROM insurance_policies")
    suspend fun getAllInsurancePolicies(): List<InsurancePolicy>

    @Query("SELECT * FROM insurance_policies")
    fun observeInsurancePolicies(): Flow<List<InsurancePolicy>>

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

    // --- fx
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFxRate(rate: FxRate)

    @Query("SELECT * FROM fx_rates ORDER BY date")
    fun observeFxRates(): Flow<List<FxRate>>

    @Query("SELECT * FROM fx_rates ORDER BY date")
    suspend fun getAllFxRates(): List<FxRate>
}
