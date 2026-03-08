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
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun readingStatsDao(): ReadingStatsDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun customRssSourceDao(): CustomRssSourceDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN nextReviewAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN easeFactor REAL NOT NULL DEFAULT 2.5")
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN interval INTEGER NOT NULL DEFAULT 0")
            }
        }
        
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
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "articles", "readTimeMinutes", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "articles", "isDownloaded", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "articles", "localImagePath", "TEXT")
                addColumnIfNotExists(db, "articles", "downloadedAt", "INTEGER")
            }
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_sourceId ON articles(sourceId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_publishedAt ON articles(publishedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_isRead ON articles(isRead)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_isFavorite ON articles(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_articles_difficultyLevel ON articles(difficultyLevel)")
                
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_word ON vocabulary(word)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_articleId ON vocabulary(articleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_isMastered_nextReviewAt ON vocabulary(isMastered, nextReviewAt)")
                
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentences_articleId ON sentences(articleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentences_isFavorite ON sentences(isFavorite)")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "reading_stats", "lookupCount", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "reading_stats", "averageWpm", "INTEGER NOT NULL DEFAULT 0")
            }
        }
        
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
