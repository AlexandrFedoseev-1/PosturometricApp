package com.example.posturometricapp.data.dp

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Создаём новую таблицу psych_state c CASCADE-ключом
        db.execSQL("""
      CREATE TABLE IF NOT EXISTS `psych_state` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `sessionId` INTEGER NOT NULL,
        `stateName` TEXT NOT NULL,
        `startTime` INTEGER NOT NULL,
        `endTime` INTEGER NOT NULL DEFAULT 0,
        FOREIGN KEY(`sessionId`) REFERENCES `session`(`id`) ON DELETE CASCADE
      )
    """.trimIndent())
        // И создаём индекс для быстрого поиска по sessionId
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_psych_state_sessionId` ON `psych_state` (`sessionId`)")
    }
}