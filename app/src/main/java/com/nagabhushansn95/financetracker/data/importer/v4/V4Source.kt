package com.nagabhushansn95.financetracker.data.importer.v4

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.Closeable

/** Raw rows of the v4 (Finance Manager 4.x) `expenditureManager` SQLite file. */
data class V4Wallet(val id: Int, val name: String, val amount: Double, val deleted: Boolean)
data class V4Bank(
    val id: Int,
    val name: String,
    val accountNumber: String?,
    val balance: Double,
    val deleted: Boolean
)

data class V4ExpenditureType(val id: Int, val name: String, val deleted: Boolean)
data class V4Transaction(
    val id: Int,
    val createdTime: String,
    val modifiedTime: String,
    val date: String,
    val type: String,
    val particulars: String,
    val rate: Double,
    val quantity: Double,
    val amount: Double,
    val hidden: Boolean,
    val includeInCounters: Boolean,
)

data class V4Template(
    val id: Int,
    val particulars: String,
    val type: String,
    val amount: Double,
    val hidden: Boolean
)

interface V4Source : Closeable {
    fun wallets(): List<V4Wallet>
    fun banks(): List<V4Bank>
    fun expenditureTypes(): List<V4ExpenditureType>
    fun transactions(): List<V4Transaction>
    fun templates(): List<V4Template>
}

/** Reads a v4 database file read-only. The file is never modified. */
class SqliteV4Source(path: String) : V4Source {
    private val db: SQLiteDatabase =
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)

    override fun wallets(): List<V4Wallet> =
        query("SELECT id, name, amount, deleted FROM wallets ORDER BY id") { c ->
            V4Wallet(c.getInt(0), c.getString(1), c.getDouble(2), c.getInt(3) != 0)
        }

    override fun banks(): List<V4Bank> =
        query("SELECT id, name, account_number, balance, deleted FROM banks ORDER BY id") { c ->
            V4Bank(
                c.getInt(0),
                c.getString(1),
                c.getStringOrNull(2),
                c.getDouble(3),
                c.getInt(4) != 0
            )
        }

    override fun expenditureTypes(): List<V4ExpenditureType> =
        query("SELECT id, name, deleted FROM expenditure_types ORDER BY id") { c ->
            V4ExpenditureType(c.getInt(0), c.getString(1), c.getInt(2) != 0)
        }

    override fun transactions(): List<V4Transaction> = query(
        "SELECT id, created_time, modified_time, date, type, particulars, rate, quantity, amount, hidden, include_in_counters " +
                "FROM transactions ORDER BY id"
    ) { c ->
        V4Transaction(
            id = c.getInt(0),
            createdTime = c.getString(1),
            modifiedTime = c.getString(2),
            date = c.getString(3),
            type = c.getString(4),
            particulars = c.getStringOrNull(5) ?: "",
            rate = c.getDouble(6),
            quantity = c.getDouble(7),
            amount = c.getDouble(8),
            hidden = c.getInt(9) != 0,
            includeInCounters = c.getInt(10) != 0,
        )
    }

    override fun templates(): List<V4Template> =
        query("SELECT id, particulars, type, amount, hidden FROM templates ORDER BY id") { c ->
            V4Template(
                c.getInt(0),
                c.getStringOrNull(1) ?: "",
                c.getString(2),
                c.getDouble(3),
                c.getInt(4) != 0
            )
        }

    override fun close() = db.close()

    private fun <T> query(sql: String, map: (Cursor) -> T): List<T> =
        db.rawQuery(sql, null).use { c ->
            val out = ArrayList<T>(c.count)
            while (c.moveToNext()) out.add(map(c))
            out
        }

    private fun Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)
}
