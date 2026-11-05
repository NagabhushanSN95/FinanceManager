package com.nagabhushansn95.financetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nagabhushansn95.financetracker.data.db.dao.AccountDao
import com.nagabhushansn95.financetracker.data.db.dao.CategoryDao
import com.nagabhushansn95.financetracker.data.db.dao.InstrumentDao
import com.nagabhushansn95.financetracker.data.db.dao.SettingsDao
import com.nagabhushansn95.financetracker.data.db.dao.StatisticsDao
import com.nagabhushansn95.financetracker.data.db.dao.TransactionDao
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.FdEvent
import com.nagabhushansn95.financetracker.data.db.entity.FdTemplate
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.RiskLevel
import com.nagabhushansn95.financetracker.data.db.entity.Setting
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import java.io.File

@Database(
    entities = [
        Account::class,
        Category::class,
        RiskLevel::class,
        InvestmentCategory::class,
        Instrument::class,
        FixedDeposit::class,
        FdTemplate::class,
        FdEvent::class,
        InsurancePolicy::class,
        InstrumentValuation::class,
        FxRate::class,
        TransactionEntity::class,
        Template::class,
        Setting::class,
    ],
    version = 6, // keep in sync with SCHEMA_VERSION (KSP cannot read the constant here)
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun transactionDao(): TransactionDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "finance_tracker.db"

        /** Same value as the @Database version; restore refuses files from a newer schema. */
        const val SCHEMA_VERSION = 6

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: run {
                    // A restore chosen under Extras while the app was running is swapped in here, on the next start.
                    com.nagabhushansn95.financetracker.data.backup.DatabaseRestore.applyPending(
                        context
                    )
                    create(context, DATABASE_NAME)
                }.also { instance = it }
            }

        /**
         * Closes the shared instance and swaps the database file for [source] (used by restore). Callers must make
         * sure nothing is observing the old instance; the next [getInstance] opens the new file.
         */
        fun replaceDatabaseFile(context: Context, source: File) = synchronized(this) {
            instance?.close()
            instance = null
            val target = context.applicationContext.getDatabasePath(DATABASE_NAME)
            target.parentFile?.mkdirs()
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            File(target.path + "-journal").delete()
            source.copyTo(target, overwrite = true)
        }

        /** A file-backed database with the production configuration (also used by tests that need a real file). */
        fun create(context: Context, name: String): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                .configure().build()

        /** An in-memory database with the production configuration, for tests. */
        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
                .configure().build()

        /**
         * Bundled SQLite instead of the framework's: the same engine on every Android version (VACUUM INTO, window
         * functions), and prepared statements are reused across rows. The framework compatibility path re-prepares
         * per row and measured ~8x slower on bulk inserts. With a driver configured, transactions must use
         * [androidx.room.useWriterConnection], not the legacy `withTransaction`.
         */
        private fun RoomDatabase.Builder<AppDatabase>.configure(): RoomDatabase.Builder<AppDatabase> =
            setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(*Migrations.ALL)
    }
}
