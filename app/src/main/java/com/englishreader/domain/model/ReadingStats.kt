package com.englishreader.domain.model

import com.englishreader.data.local.entity.ReadingStatsEntity

data class ReadingStats(
    val date: String,
    val articlesRead: Int,
    val wordsRead: Int,
    val timeSpentMinutes: Int,
    val vocabularySaved: Int
) {
    companion object {
        fun fromEntity(entity: ReadingStatsEntity): ReadingStats {
            return ReadingStats(
                date = entity.date,
                articlesRead = entity.articlesRead,
                wordsRead = entity.wordsRead,
                timeSpentMinutes = entity.timeSpentMinutes,
                vocabularySaved = entity.vocabularySaved
            )
        }
    }
}

data class OverallStats(
    val totalArticlesRead: Int,
    val totalWordsRead: Int,
    val totalTimeSpentMinutes: Int,
    val totalVocabularySaved: Int,
    val currentStreak: Int,
    val longestStreak: Int
)
