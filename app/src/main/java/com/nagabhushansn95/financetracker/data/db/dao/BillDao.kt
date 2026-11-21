package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillPayment
import com.nagabhushansn95.financetracker.data.db.entity.CreditAgency
import com.nagabhushansn95.financetracker.data.db.entity.CreditScore
import kotlinx.coroutines.flow.Flow

/** Bills with their payments, and the credit agencies with their scores. */
@Dao
interface BillDao {
    // --- bills
    @Insert
    suspend fun insert(bill: Bill): Long

    @Update
    suspend fun update(bill: Bill)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Long): Bill?

    @Query("SELECT * FROM bills ORDER BY name")
    suspend fun getAll(): List<Bill>

    @Query("SELECT * FROM bills ORDER BY name")
    fun observeAll(): Flow<List<Bill>>

    // --- payments
    @Insert
    suspend fun insertPayment(payment: BillPayment): Long

    @Update
    suspend fun updatePayment(payment: BillPayment)

    @Query("DELETE FROM bill_payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query("SELECT * FROM bill_payments WHERE id = :id")
    suspend fun getPayment(id: Long): BillPayment?

    @Query("SELECT * FROM bill_payments WHERE bill_id = :billId ORDER BY period_start, id")
    suspend fun getPayments(billId: Long): List<BillPayment>

    @Query("SELECT * FROM bill_payments WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getPaymentOf(transactionId: Long): BillPayment?

    @Query("SELECT COUNT(*) FROM bill_payments WHERE bill_id = :billId")
    suspend fun paymentCount(billId: Long): Int

    @Query("SELECT * FROM bill_payments ORDER BY period_start, id")
    suspend fun getAllPayments(): List<BillPayment>

    @Query("SELECT * FROM bill_payments ORDER BY period_start, id")
    fun observePayments(): Flow<List<BillPayment>>

    // --- credit agencies and scores
    @Insert
    suspend fun insertAgency(agency: CreditAgency): Long

    @Update
    suspend fun updateAgency(agency: CreditAgency)

    @Query("DELETE FROM credit_agencies WHERE id = :id")
    suspend fun deleteAgency(id: Long)

    @Query("SELECT * FROM credit_agencies ORDER BY display_order, id")
    suspend fun getAgencies(): List<CreditAgency>

    @Query("SELECT * FROM credit_agencies ORDER BY display_order, id")
    fun observeAgencies(): Flow<List<CreditAgency>>

    @Query("SELECT COALESCE(MAX(display_order), 0) FROM credit_agencies")
    suspend fun maxAgencyOrder(): Int

    @Insert
    suspend fun insertScore(score: CreditScore): Long

    @Update
    suspend fun updateScore(score: CreditScore)

    @Query("DELETE FROM credit_scores WHERE id = :id")
    suspend fun deleteScore(id: Long)

    @Query("SELECT COUNT(*) FROM credit_scores WHERE agency_id = :agencyId")
    suspend fun scoreCount(agencyId: Long): Int

    @Query("SELECT * FROM credit_scores ORDER BY date, id")
    suspend fun getScores(): List<CreditScore>

    @Query("SELECT * FROM credit_scores ORDER BY date, id")
    fun observeScores(): Flow<List<CreditScore>>
}
