package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: Account): Long

    @Insert
    suspend fun insertAll(accounts: List<Account>): List<Long>

    @Update
    suspend fun update(account: Account)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE deleted = 0 ORDER BY display_order, id")
    fun observeVisible(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE deleted = 0 ORDER BY display_order, id")
    suspend fun getVisible(): List<Account>

    @Query("SELECT * FROM accounts WHERE deleted = 0 AND account_type = :type ORDER BY display_order, id")
    suspend fun getVisibleByType(type: AccountType): List<Account>

    @Query("SELECT * FROM accounts WHERE deleted = 1 ORDER BY display_order, id")
    suspend fun getDeleted(): List<Account>

    @Query("SELECT * FROM accounts ORDER BY display_order, id")
    suspend fun getAll(): List<Account>

    @Query("UPDATE accounts SET balance = balance + :delta, modified_time = :modifiedTime WHERE id = :id")
    suspend fun adjustBalance(id: Long, delta: Double, modifiedTime: String)

    @Query("UPDATE accounts SET display_order = :order WHERE id = :id")
    suspend fun setDisplayOrder(id: Long, order: Int)

    @Query("SELECT * FROM accounts ORDER BY display_order, id")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT COALESCE(MAX(display_order), 0) FROM accounts")
    suspend fun maxDisplayOrder(): Int

    @Query("UPDATE accounts SET deleted = :deleted, modified_time = :modifiedTime WHERE id = :id")
    suspend fun setDeleted(id: Long, deleted: Boolean, modifiedTime: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE from_account_id = :accountId OR to_account_id = :accountId")
    suspend fun transactionCount(accountId: Long): Int
}
