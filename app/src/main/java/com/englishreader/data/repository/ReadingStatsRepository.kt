package com.englishreader.data.repository

import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.local.entity.ReadingStatsEntity
import com.englishreader.domain.model.ReadingStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingStatsRepository @Inject constructor(
    private val readingStatsDao: ReadingStatsDao
) {
    
    fun getAllStats(): Flow<List<ReadingStats>> {
        return readingStatsDao.getAllStats().map { list ->
            list.map { ReadingStats.fromEntity(it) }
        }
    }
    
    fun getRecentStats(days: Int): Flow<List<ReadingStats>> {
        return readingStatsDao.getRecentStats(days).map { list ->
            list.map { ReadingStats.fromEntity(it) }
        }
    }
    
    suspend fun getStatsForDate(date: String): ReadingStatsEntity? {
        return readingStatsDao.getStatsForDate(date)
    }
    
    suspend fun insertOrUpdateStats(stats: ReadingStatsEntity) {
        readingStatsDao.insertOrUpdateStats(stats)
    }
    
    suspend fun getTotalArticlesRead(): Int {
        return readingStatsDao.getTotalArticlesRead() ?: 0
    }
    
    suspend fun getTotalWordsRead(): Int {
        return readingStatsDao.getTotalWordsRead() ?: 0
    }
    
    suspend fun getTotalTimeSpent(): Int {
        return readingStatsDao.getTotalTimeSpent() ?: 0
    }
    
    suspend fun getReadingDays(): List<ReadingStatsEntity> {
        return readingStatsDao.getReadingDays()
    }
    
    /**
     * 计算连续阅读天数
     */
    suspend fun calculateStreak(): Int {
        val readingDays = readingStatsDao.getReadingDays()
        if (readingDays.isEmpty()) return 0
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { dateFormat.format(it.time) }
        
        val sortedDates = readingDays.map { it.date }.sortedDescending()
        if (sortedDates.isEmpty()) return 0
        
        val latestDate = sortedDates.first()
        if (latestDate != today && latestDate != yesterday) return 0
        
        var streak = 0
        var currentDate = if (latestDate == today) today else yesterday
        
        for (date in sortedDates) {
            if (date == currentDate) {
                streak++
                currentDate = Calendar.getInstance().apply {
                    time = dateFormat.parse(currentDate) ?: return@apply
                    add(Calendar.DAY_OF_YEAR, -1)
                }.let { dateFormat.format(it.time) }
            } else if (date < currentDate) {
                break
            }
        }
        
        return streak
    }
    
    /**
     * 获取本周阅读数据
     */
    suspend fun getWeeklyData(): List<DailyStatsData> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("E", Locale.CHINESE)
        
        return (0..6).map { daysAgo ->
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dateStr = dateFormat.format(calendar.time)
            val dayName = dayFormat.format(calendar.time)
            
            val stats = readingStatsDao.getStatsForDate(dateStr)
            
            DailyStatsData(
                date = dateStr,
                dayName = dayName,
                articlesRead = stats?.articlesRead ?: 0,
                wordsRead = stats?.wordsRead ?: 0,
                timeSpentMinutes = stats?.timeSpentMinutes ?: 0
            )
        }.reversed()
    }
    
    /**
     * 迁移已有的阅读记录到统计表
     */
    suspend fun migrateExistingReadings(articlesRead: Int, wordsRead: Int) {
        val existingStats = readingStatsDao.getReadingDays()
        if (existingStats.isNotEmpty()) return
        
        if (articlesRead > 0) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            readingStatsDao.insertOrUpdateStats(
                ReadingStatsEntity(
                    date = today,
                    articlesRead = articlesRead,
                    wordsRead = wordsRead,
                    timeSpentMinutes = 0,
                    vocabularySaved = 0
                )
            )
        }
    }
}

data class DailyStatsData(
    val date: String,
    val dayName: String,
    val articlesRead: Int,
    val wordsRead: Int,
    val timeSpentMinutes: Int
)
