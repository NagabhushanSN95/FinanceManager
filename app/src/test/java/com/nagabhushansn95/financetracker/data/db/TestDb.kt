package com.nagabhushansn95.financetracker.data.db

import android.content.Context
import androidx.room.Room

/** An in-memory database with the production configuration (driver, migrations), for tests only. */
object TestDb {
    fun inMemory(context: Context): AppDatabase = with(AppDatabase) {
        Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
            .configure().build()
    }
}

/** Inserts a stock or fund with its listing, as the app's HoldingsRepository.add does, and returns the id. */
suspend fun AppDatabase.insertListed(
    instrument: com.nagabhushansn95.financetracker.data.db.entity.Instrument,
    security: com.nagabhushansn95.financetracker.data.db.entity.Security =
        com.nagabhushansn95.financetracker.data.db.entity.Security(instrumentId = 0),
): Long {
    val id = instrumentDao().insert(instrument)
    instrumentDao().upsertSecurity(security.copy(instrumentId = id))
    return id
}
