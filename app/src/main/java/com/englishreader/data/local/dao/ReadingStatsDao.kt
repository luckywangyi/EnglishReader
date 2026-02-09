package com.englishreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.englishreader.data.local.entity.ReadingStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStatsDao {
    
    @Query("SELECT * FROM reading_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<ReadingStatsEntity>>
    
    @Query("SELECT * FROM reading_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): ReadingStatsEntity?
    
    @Query("SELECT * FROM reading_stats WHERE date >= :startDate ORDER BY date DESC")
    fun getStatsFromDate(startDate: String): Flow<List<ReadingStatsEntity>>
    
    @Query("SELECT * FROM reading_stats ORDER BY date DESC LIMIT :days")
    fun getRecentStats(days: Int): Flow<List<ReadingStatsEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: ReadingStatsEntity)
    
    @Query("""
        UPDATE reading_stats SET 
            articlesRead = articlesRead + :articles,
            wordsRead = wordsRead + :words,
            timeSpentMinutes = timeSpentMinutes + :minutes,
            vocabularySaved = vocabularySaved + :vocabulary
        WHERE date = :date
    """)
    suspend fun incrementStats(date: String, articles: Int, words: Int, minutes: Int, vocabulary: Int)
    
    @Query("UPDATE reading_stats SET lookupCount = lookupCount + :count WHERE date = :date")
    suspend fun incrementLookupCount(date: String, count: Int)
    
    @Query("UPDATE reading_stats SET averageWpm = :wpm WHERE date = :date")
    suspend fun updateAverageWpm(date: String, wpm: Int)
    
    @Query("SELECT SUM(articlesRead) FROM reading_stats")
    suspend fun getTotalArticlesRead(): Int?
    
    @Query("SELECT SUM(wordsRead) FROM reading_stats")
    suspend fun getTotalWordsRead(): Int?
    
    @Query("SELECT SUM(timeSpentMinutes) FROM reading_stats")
    suspend fun getTotalTimeSpent(): Int?
    
    // Calculate streak days (consecutive days with at least 1 article read)
    @Query("SELECT COUNT(*) FROM reading_stats WHERE articlesRead > 0")
    suspend fun getTotalReadingDays(): Int
    
    @Query("SELECT * FROM reading_stats WHERE articlesRead > 0 ORDER BY date DESC")
    suspend fun getReadingDays(): List<ReadingStatsEntity>
    
    @Query("SELECT * FROM reading_stats WHERE date >= :startDate ORDER BY date ASC")
    suspend fun getStatsFromDateSync(startDate: String): List<ReadingStatsEntity>
}
