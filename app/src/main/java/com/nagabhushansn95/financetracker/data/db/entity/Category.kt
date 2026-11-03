package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CategoryScope { EXPENSE, INCOME, BOTH }

/** Replaces v4 "expenditure types". Two levels (parent/child); income categories are new in v5. */
@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["parent_id"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("parent_id"), Index("display_order")]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "applies_to") val appliesTo: CategoryScope = CategoryScope.EXPENSE,
    val icon: String? = null,
    val color: Int? = null,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    val deleted: Boolean = false,
)
