package com.englishreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.englishreader.data.local.dao.ArticleDao
import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.local.dao.SentenceDao
import com.englishreader.data.local.dao.VocabularyDao
import com.englishreader.data.local.entity.ArticleEntity
import com.englishreader.data.local.entity.ReadingStatsEntity
import com.englishreader.data.local.entity.SentenceEntity
import com.englishreader.data.local.entity.VocabularyEntity

@Database(
    entities = [
        ArticleEntity::class,
        VocabularyEntity::class,
        ReadingStatsEntity::class,
        SentenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun readingStatsDao(): ReadingStatsDao
    abstract fun sentenceDao(): SentenceDao
}
