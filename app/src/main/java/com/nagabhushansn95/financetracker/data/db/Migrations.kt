package com.nagabhushansn95.financetracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * One step per schema version, kept for good: Room runs every step from a file's version up to the app's, so a
 * backup from any earlier version restores on any later app (11 → 12 → 13 …). A step's SQL is copied from the
 * `createSql` lines of the exported schema in `app/schemas` for that version; the migration test builds a database
 * of the older version from its exported schema and lets Room validate the result.
 * Versions below 11 (the pre-cut-over files) have no path by design.
 */
object Migrations {
    /** 5.1.0: bills with their payments, credit agencies with their scores; CIBIL seeded as the first agency. */
    val M11_12: Migration = object : Migration(11, 12) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `from_account_id` INTEGER, `to_account_id` INTEGER, `category_id` INTEGER, `amount` REAL, `particulars` TEXT, `schedule` TEXT NOT NULL, `every_months` INTEGER NOT NULL, `first_due` TEXT NOT NULL, `covers_previous` INTEGER NOT NULL, `validity_days` INTEGER, `ended_on` TEXT, `notes` TEXT, `created_time` TEXT NOT NULL, `modified_time` TEXT NOT NULL, FOREIGN KEY(`from_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`to_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_from_account_id` ON `bills` (`from_account_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_to_account_id` ON `bills` (`to_account_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_category_id` ON `bills` (`category_id`)")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `bill_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bill_id` INTEGER NOT NULL, `period_start` TEXT NOT NULL, `period_end` TEXT NOT NULL, `due_date` TEXT NOT NULL, `transaction_id` INTEGER, `note` TEXT, FOREIGN KEY(`bill_id`) REFERENCES `bills`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_payments_bill_id_period_start` ON `bill_payments` (`bill_id`, `period_start`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_payments_transaction_id` ON `bill_payments` (`transaction_id`)")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `credit_agencies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_order` INTEGER NOT NULL, `notes` TEXT)"
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `credit_scores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `agency_id` INTEGER NOT NULL, `date` TEXT NOT NULL, `score` INTEGER NOT NULL, `notes` TEXT, FOREIGN KEY(`agency_id`) REFERENCES `credit_agencies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_credit_scores_agency_id_date` ON `credit_scores` (`agency_id`, `date`)")
            connection.execSQL("INSERT INTO `credit_agencies` (`name`, `display_order`) VALUES ('CIBIL', 1)")
        }
    }

    /** 5.1.0: a bill may alert only once overdue rather than in the run-up to its due date. */
    val M12_13: Migration = object : Migration(12, 13) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `bills` ADD COLUMN `alert_when_overdue` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(M11_12, M12_13)
}
