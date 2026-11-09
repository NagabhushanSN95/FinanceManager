package com.nagabhushansn95.financetracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Schema migrations, oldest first. Each one is covered by MigrationsTest against the schema JSON of the old version. */
object Migrations {

    /**
     * 1 → 2: risk levels become a table the user edits; the stored overall target goes (it is derived from the goal and
     * wealth percentages now); a settings table holds the goal amount; a "Provident Funds" category joins the tree.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS `risk_levels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_order` INTEGER NOT NULL)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_risk_levels_display_order` ON `risk_levels` (`display_order`)")
            connection.execSQL("INSERT INTO `risk_levels` (`id`, `name`, `display_order`) VALUES (1, 'No risk', 1), (2, 'Low risk', 2), (3, 'Medium risk', 3), (4, 'High risk', 4)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")

            connection.execSQL(
                "CREATE TABLE `investment_categories_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `parent_id` INTEGER, " +
                        "`risk_level_id` INTEGER, `tolerance_pct` REAL, `target_goal_pct` REAL, `target_wealth_pct` REAL, `rebalance_notes` TEXT, `display_order` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`parent_id`) REFERENCES `investment_categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                        "FOREIGN KEY(`risk_level_id`) REFERENCES `risk_levels`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
            )
            connection.execSQL(
                "INSERT INTO `investment_categories_new` (`id`, `name`, `parent_id`, `risk_level_id`, `tolerance_pct`, `target_goal_pct`, `target_wealth_pct`, `rebalance_notes`, `display_order`) " +
                        "SELECT `id`, `name`, `parent_id`, CASE `risk_bucket` WHEN 'NONE' THEN 1 WHEN 'LOW' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'HIGH' THEN 4 ELSE NULL END, " +
                        "`tolerance_pct`, `target_goal_pct`, `target_wealth_pct`, `rebalance_notes`, `display_order` FROM `investment_categories`"
            )
            // Foreign keys are off while Room migrates (it enables them in onOpen), so the old table can go.
            connection.execSQL("DROP TABLE `investment_categories`")
            connection.execSQL("ALTER TABLE `investment_categories_new` RENAME TO `investment_categories`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_investment_categories_parent_id` ON `investment_categories` (`parent_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_investment_categories_risk_level_id` ON `investment_categories` (`risk_level_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_investment_categories_display_order` ON `investment_categories` (`display_order`)")
            // Provident funds get a category of their own (targets and risk), only where the tree was already seeded.
            connection.execSQL(
                "INSERT INTO `investment_categories` (`name`, `risk_level_id`, `display_order`) " +
                        "SELECT 'Provident Funds', 1, (SELECT COALESCE(MAX(`display_order`), 0) + 1 FROM `investment_categories`) " +
                        "WHERE EXISTS (SELECT 1 FROM `investment_categories`) AND NOT EXISTS (SELECT 1 FROM `investment_categories` WHERE `name` = 'Provident Funds')"
            )
        }
    }

    /**
     * 2 → 3: templates carry a rate and a quantity instead of an amount. The old amount was in practice the rate
     * (Finance Manager templates were rate × 1), so it moves into `rate`; the quantity starts empty.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE `templates_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `particulars` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                        "`from_account_id` INTEGER, `to_account_id` INTEGER, `category_id` INTEGER, `instrument_id` INTEGER, `rate` REAL, `quantity` REAL, `hidden` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`from_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`to_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL(
                "INSERT INTO `templates_new` (`id`, `particulars`, `kind`, `from_account_id`, `to_account_id`, `category_id`, `instrument_id`, `rate`, `quantity`, `hidden`) " +
                        "SELECT `id`, `particulars`, `kind`, `from_account_id`, `to_account_id`, `category_id`, `instrument_id`, `amount`, NULL, `hidden` FROM `templates`"
            )
            connection.execSQL("DROP TABLE `templates`")
            connection.execSQL("ALTER TABLE `templates_new` RENAME TO `templates`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_from_account_id` ON `templates` (`from_account_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_to_account_id` ON `templates` (`to_account_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_category_id` ON `templates` (`category_id`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_instrument_id` ON `templates` (`instrument_id`)")
        }
    }

    /** Fixed deposit templates: named presets for "New fixed deposit". */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `fd_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`bank_name` TEXT, `branch` TEXT, `holder_name` TEXT, `cust_id` TEXT, `period_months` INTEGER, `period_days` INTEGER, " +
                        "`rate` REAL, `compounding_months` INTEGER, `tds_rate` REAL, `owner` TEXT NOT NULL, `display_order` INTEGER NOT NULL)"
            )
        }
    }

    /**
     * Deposits learn whether TDS is taken yearly; templates may carry the amount, the paying account and notes and
     * lose the TDS rate (owner becomes optional); the fd_events table records interest credited and TDS deducted.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `fixed_deposits` ADD COLUMN `tds_yearly` INTEGER")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `fd_templates_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`bank_name` TEXT, `branch` TEXT, `holder_name` TEXT, `cust_id` TEXT, `amount` REAL, `from_account_id` INTEGER, " +
                        "`period_months` INTEGER, `period_days` INTEGER, `rate` REAL, `compounding_months` INTEGER, `owner` TEXT, `notes` TEXT, " +
                        "`display_order` INTEGER NOT NULL)"
            )
            connection.execSQL(
                "INSERT INTO `fd_templates_new` (`id`, `name`, `bank_name`, `branch`, `holder_name`, `cust_id`, `period_months`, `period_days`, " +
                        "`rate`, `compounding_months`, `owner`, `display_order`) SELECT `id`, `name`, `bank_name`, `branch`, `holder_name`, `cust_id`, " +
                        "`period_months`, `period_days`, `rate`, `compounding_months`, `owner`, `display_order` FROM `fd_templates`"
            )
            connection.execSQL("DROP TABLE `fd_templates`")
            connection.execSQL("ALTER TABLE `fd_templates_new` RENAME TO `fd_templates`")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `fd_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `instrument_id` INTEGER NOT NULL, " +
                        "`date` TEXT NOT NULL, `kind` TEXT NOT NULL, `amount` REAL NOT NULL, `notes` TEXT, " +
                        "FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_fd_events_instrument_id_date` ON `fd_events` (`instrument_id`, `date`)")
        }
    }

    /** 5 -> 6: deposit type (regular or sweep-in) and the sweep-in date. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `fixed_deposits` ADD COLUMN `deposit_type` TEXT NOT NULL DEFAULT 'REGULAR'")
            connection.execSQL("ALTER TABLE `fixed_deposits` ADD COLUMN `sweep_in_date` TEXT")
        }
    }

    /**
     * 6 -> 7: insurance rebuilt. The type decides what counts as an investment (life), so the flag goes; the next-due
     * date becomes a due day and month (life and term premiums fall due by the calendar); a vehicle number joins for
     * motor rows; OTHER becomes TERM. The validity_periods table holds health cover, vehicle insurance cover, service
     * and emission-test validity, each tied to the entry that paid for it.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `insurance_policies_new` (`instrument_id` INTEGER NOT NULL, `insurance_type` TEXT NOT NULL, " +
                        "`provider` TEXT, `policy_name` TEXT, `policy_number` TEXT, `vehicle_number` TEXT, `sum_assured` REAL, `premium_amount` REAL, " +
                        "`premium_frequency_months` INTEGER, `due_day` INTEGER, `due_month` INTEGER, `start_date` TEXT, `maturity_date` TEXT, " +
                        "PRIMARY KEY(`instrument_id`), FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL(
                "INSERT INTO `insurance_policies_new` (`instrument_id`, `insurance_type`, `provider`, `policy_name`, `policy_number`, `vehicle_number`, " +
                        "`sum_assured`, `premium_amount`, `premium_frequency_months`, `due_day`, `due_month`, `start_date`, `maturity_date`) " +
                        "SELECT `instrument_id`, CASE `insurance_type` WHEN 'OTHER' THEN 'TERM' ELSE `insurance_type` END, `provider`, `policy_name`, " +
                        "`policy_number`, NULL, `sum_assured`, `premium_amount`, `premium_frequency_months`, " +
                        "CAST(substr(`next_due_date`, 9, 2) AS INTEGER), CAST(substr(`next_due_date`, 6, 2) AS INTEGER), `start_date`, `maturity_date` " +
                        "FROM `insurance_policies`"
            )
            connection.execSQL("DROP TABLE `insurance_policies`")
            connection.execSQL("ALTER TABLE `insurance_policies_new` RENAME TO `insurance_policies`")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `validity_periods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `instrument_id` INTEGER NOT NULL, " +
                        "`kind` TEXT NOT NULL, `start_date` TEXT, `end_date` TEXT NOT NULL, `transaction_id` INTEGER, `provider` TEXT, `reference` TEXT, " +
                        "FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_validity_periods_instrument_id_kind` ON `validity_periods` (`instrument_id`, `kind`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_validity_periods_transaction_id` ON `validity_periods` (`transaction_id`)")
        }
    }

    /** 7 → 8: a validity period carries the sum insured a health premium bought, since it may change at every renewal. */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `validity_periods` ADD COLUMN `sum_insured` REAL")
        }
    }

    val ALL = arrayOf<Migration>(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8
    )
}
