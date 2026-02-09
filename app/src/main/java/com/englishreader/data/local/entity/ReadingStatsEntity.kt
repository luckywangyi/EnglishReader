package com.englishreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_stats")
data class ReadingStatsEntity(
    @PrimaryKey
    val date: String,                      // yyyy-MM-dd format
    val articlesRead: Int = 0,
    val wordsRead: Int = 0,
    val timeSpentMinutes: Int = 0,
    val vocabularySaved: Int = 0,
    val lookupCount: Int = 0,              // 查词次数
    val averageWpm: Int = 0                // 平均每分钟阅读词数
)
