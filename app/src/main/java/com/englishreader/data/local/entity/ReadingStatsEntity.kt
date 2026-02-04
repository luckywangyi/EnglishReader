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
    val vocabularySaved: Int = 0
)
