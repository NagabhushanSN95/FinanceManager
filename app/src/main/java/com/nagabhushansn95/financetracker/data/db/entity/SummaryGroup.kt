package com.nagabhushansn95.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A line of the Investment Summary's third table, defined by what it holds: kinds of instrument (stocks, funds,
 * deposits, life insurance, provident funds) and investment categories, in any mix. "Liquid" and "In Zerodha" are
 * the two the app starts with; the user adds, edits and orders them under Manage › Summary groups.
 * [members] is the comma-separated list of tokens SummaryGroups reads: `type:STOCK`, `pf`, `category:12`.
 */
@Entity(tableName = "summary_groups", indices = [Index("display_order")])
data class SummaryGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    val members: String,
)
