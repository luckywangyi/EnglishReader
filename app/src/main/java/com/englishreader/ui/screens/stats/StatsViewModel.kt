package com.englishreader.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.repository.ArticleRepository
import com.englishreader.data.repository.VocabularyRepository
import com.englishreader.domain.model.ReadingStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val readingStatsDao: ReadingStatsDao
) : ViewModel() {
    
    private val _overallStats = MutableStateFlow(OverallStatsUi())
    val overallStats: StateFlow<OverallStatsUi> = _overallStats.asStateFlow()
    
    val recentStats: StateFlow<List<ReadingStats>> = readingStatsDao.getRecentStats(30)
        .map { list -> list.map { ReadingStats.fromEntity(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _weeklyData = MutableStateFlow<List<DailyStats>>(emptyList())
    val weeklyData: StateFlow<List<DailyStats>> = _weeklyData.asStateFlow()
    
    private val _heatmapData = MutableStateFlow<List<HeatmapDay>>(emptyList())
    val heatmapData: StateFlow<List<HeatmapDay>> = _heatmapData.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            // 先迁移历史数据（如果需要）
            migrateExistingReadings()
            
            // Load overall stats
            val articlesRead = articleRepository.getReadArticleCount()
            val wordsRead = articleRepository.getTotalWordsRead()
            val vocabularyCount = vocabularyRepository.getVocabularyCount()
            val masteredCount = vocabularyRepository.getMasteredCount()
            
            // Calculate streak
            val streak = calculateStreak()
            
            _overallStats.value = OverallStatsUi(
                totalArticlesRead = articlesRead,
                totalWordsRead = wordsRead,
                totalVocabulary = vocabularyCount,
                masteredVocabulary = masteredCount,
                currentStreak = streak,
                isLoaded = true
            )
            
            // Load weekly data
            loadWeeklyData()
            
            // Load heatmap data (90 days)
            loadHeatmapData()
        }
    }
    
    /**
     * 迁移已有的阅读记录到统计表（仅执行一次）
     */
    private suspend fun migrateExistingReadings() {
        // 检查是否已有统计数据
        val existingStats = readingStatsDao.getReadingDays()
        if (existingStats.isNotEmpty()) return // 已有数据，无需迁移
        
        // 获取已读文章数量和总词数
        val articlesRead = articleRepository.getReadArticleCount()
        val wordsRead = articleRepository.getTotalWordsRead()
        
        if (articlesRead > 0) {
            // 将历史数据记录到今天
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            readingStatsDao.insertOrUpdateStats(
                com.englishreader.data.local.entity.ReadingStatsEntity(
                    date = today,
                    articlesRead = articlesRead,
                    wordsRead = wordsRead,
                    timeSpentMinutes = 0,
                    vocabularySaved = 0
                )
            )
        }
    }
    
    private suspend fun calculateStreak(): Int {
        val readingDays = readingStatsDao.getReadingDays()
        if (readingDays.isEmpty()) return 0
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { dateFormat.format(it.time) }
        
        // Check if today or yesterday has reading
        val sortedDates = readingDays.map { it.date }.sortedDescending()
        if (sortedDates.isEmpty()) return 0
        
        val latestDate = sortedDates.first()
        if (latestDate != today && latestDate != yesterday) return 0
        
        // Count consecutive days
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
    
    private suspend fun loadWeeklyData() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("E", Locale.CHINESE)
        
        val weekData = (0..6).map { daysAgo ->
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dateStr = dateFormat.format(calendar.time)
            val dayName = dayFormat.format(calendar.time)
            
            val stats = readingStatsDao.getStatsForDate(dateStr)
            
            DailyStats(
                date = dateStr,
                dayName = dayName,
                articlesRead = stats?.articlesRead ?: 0,
                wordsRead = stats?.wordsRead ?: 0
            )
        }.reversed()
        
        _weeklyData.value = weekData
    }
    
    private suspend fun loadHeatmapData() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -89)
        val startDate = dateFormat.format(calendar.time)
        
        val statsMap = readingStatsDao.getStatsFromDateSync(startDate)
            .associateBy { it.date }
        
        val days = (0..89).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -(89 - daysAgo))
            }
            val dateStr = dateFormat.format(cal.time)
            val stats = statsMap[dateStr]
            val intensity = when {
                stats == null -> 0
                stats.timeSpentMinutes >= 30 || stats.articlesRead >= 3 -> 4
                stats.timeSpentMinutes >= 15 || stats.articlesRead >= 2 -> 3
                stats.timeSpentMinutes >= 5 || stats.articlesRead >= 1 -> 2
                stats.vocabularySaved > 0 -> 1
                else -> 0
            }
            HeatmapDay(
                date = dateStr,
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK), // 1=Sun, 7=Sat
                intensity = intensity
            )
        }
        
        _heatmapData.value = days
    }
    
    fun refresh() {
        loadStats()
    }
}

data class HeatmapDay(
    val date: String,
    val dayOfWeek: Int, // 1=Sunday, 7=Saturday
    val intensity: Int  // 0-4
)

data class OverallStatsUi(
    val totalArticlesRead: Int = 0,
    val totalWordsRead: Int = 0,
    val totalVocabulary: Int = 0,
    val masteredVocabulary: Int = 0,
    val currentStreak: Int = 0,
    val isLoaded: Boolean = false
)

data class DailyStats(
    val date: String,
    val dayName: String,
    val articlesRead: Int,
    val wordsRead: Int
)
