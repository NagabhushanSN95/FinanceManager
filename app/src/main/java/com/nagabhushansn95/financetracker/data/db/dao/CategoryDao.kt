package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nagabhushansn95.financetracker.data.db.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>): List<Long>

    @Update
    suspend fun update(category: Category)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY display_order, id")
    fun observeVisible(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY display_order, id")
    suspend fun getVisible(): List<Category>

    @Query("SELECT * FROM categories ORDER BY display_order, id")
    suspend fun getAll(): List<Category>

    @Query("SELECT COALESCE(MAX(display_order), 0) FROM categories")
    suspend fun maxDisplayOrder(): Int

    @Query("UPDATE categories SET display_order = :order WHERE id = :id")
    suspend fun setDisplayOrder(id: Long, order: Int)

    @Query("UPDATE categories SET deleted = :deleted WHERE id = :id")
    suspend fun setDeleted(id: Long, deleted: Boolean)
}
