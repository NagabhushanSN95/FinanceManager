package com.nagabhushansn95.financetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nagabhushansn95.financetracker.data.db.dao.AccountDao
import com.nagabhushansn95.financetracker.data.db.dao.CategoryDao
import com.nagabhushansn95.financetracker.data.db.dao.InstrumentDao
import com.nagabhushansn95.financetracker.data.db.dao.TransactionDao
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity

@Database(
    entities = [
        Account::class,
        Category::class,
        InvestmentCategory::class,
        Instrument::class,
        FixedDeposit::class,
        InsurancePolicy::class,
        InstrumentValuation::class,
        FxRate::class,
        TransactionEntity::class,
        Template::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "finance_tracker.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .build()
                    .also { instance = it }
            }
    }
}
