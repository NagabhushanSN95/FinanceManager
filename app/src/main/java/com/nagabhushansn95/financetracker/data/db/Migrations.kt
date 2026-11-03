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

    val ALL = arrayOf<Migration>(MIGRATION_1_2)
}
