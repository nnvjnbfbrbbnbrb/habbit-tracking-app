package com.ansangha.craxxjxbdbf.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine_tasks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `timeMinutesFromMidnight` INTEGER NOT NULL,
                `repeatCount` INTEGER NOT NULL,
                `daysOfWeekMask` INTEGER NOT NULL,
                `enabled` INTEGER NOT NULL,
                `graceMinutes` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_tasks_enabled` ON `routine_tasks` (`enabled`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine_completions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `taskId` INTEGER NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `completedAt` INTEGER NOT NULL,
                `missed` INTEGER NOT NULL,
                FOREIGN KEY(`taskId`) REFERENCES `routine_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_completions_taskId` ON `routine_completions` (`taskId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_routine_completions_taskId_dateEpochDay` " +
                "ON `routine_completions` (`taskId`, `dateEpochDay`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_routine_progress` (
                `id` INTEGER NOT NULL PRIMARY KEY,
                `xp` INTEGER NOT NULL,
                `level` INTEGER NOT NULL,
                `lastUpdated` INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine_badges` (
                `badgeId` TEXT NOT NULL PRIMARY KEY,
                `unlockedAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
