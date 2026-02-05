package com.englishreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.englishreader.data.local.dao.ArticleDao
import com.englishreader.data.local.dao.CustomRssSourceDao
import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.local.dao.SentenceDao
import com.englishreader.data.local.dao.VocabularyDao
import com.englishreader.data.local.entity.ArticleEntity
import com.englishreader.data.local.entity.CustomRssSourceEntity
import com.englishreader.data.local.entity.ReadingStatsEntity
import com.englishreader.data.local.entity.SentenceEntity
import com.englishreader.data.local.entity.VocabularyEntity

@Database(
    entities = [
        ArticleEntity::class,
        VocabularyEntity::class,
        ReadingStatsEntity::class,
        SentenceEntity::class,
        CustomRssSourceEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun readingStatsDao(): ReadingStatsDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun customRssSourceDao(): CustomRssSourceDao
    
    companion object {
        // 数据库迁移：版本 1 -> 2
        // 添加间隔重复算法字段到 vocabulary 表
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加 nextReviewAt 列
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN nextReviewAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                // 添加 easeFactor 列
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN easeFactor REAL NOT NULL DEFAULT 2.5")
                // 添加 interval 列
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN interval INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // 数据库迁移：版本 2 -> 3
        // 添加自定义 RSS 源表
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_rss_sources (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'CUSTOM',
                        iconUrl TEXT,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        lastFetchedAt INTEGER,
                        articleCount INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
        
        // 数据库迁移：版本 3 -> 4
        // 添加文章离线阅读字段和阅读时长字段
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 安全添加列（如果不存在才添加）
                addColumnIfNotExists(db, "articles", "readTimeMinutes", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "articles", "isDownloaded", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "articles", "localImagePath", "TEXT")
                addColumnIfNotExists(db, "articles", "downloadedAt", "INTEGER")
            }
        }
        
        /**
         * 安全添加列 - 如果列不存在才添加
         */
        private fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            val cursor = db.query("PRAGMA table_info($tableName)")
            val columnExists = cursor.use {
                while (it.moveToNext()) {
                    val nameIndex = it.getColumnIndex("name")
                    if (nameIndex >= 0 && it.getString(nameIndex) == columnName) {
                        return@use true
                    }
                }
                false
            }
            
            if (!columnExists) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDefinition")
            }
        }
    }
}
