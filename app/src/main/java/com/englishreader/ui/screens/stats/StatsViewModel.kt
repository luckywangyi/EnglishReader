package com.englishreader.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.repository.ArticleRepository
import com.englishreader.data.repository.ReadingStatsRepository
import com.englishreader.data.repository.VocabularyRepository
import com.englishreader.domain.model.ReadingStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val readingStatsRepository: ReadingStatsRepository
) : ViewModel() {
    
    private val _overallStats = MutableStateFlow(OverallStatsUi())
    val overallStats: StateFlow<OverallStatsUi> = _overallStats.asStateFlow()
    
    val recentStats: StateFlow<List<ReadingStats>> = readingStatsRepository.getRecentStats(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _weeklyData = MutableStateFlow<List<DailyStats>>(emptyList())
    val weeklyData: StateFlow<List<DailyStats>> = _weeklyData.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            // 先迁移历史数据（如果需要）
            val articlesRead = articleRepository.getReadArticleCount()
            val wordsRead = articleRepository.getTotalWordsRead()
            readingStatsRepository.migrateExistingReadings(articlesRead, wordsRead)
            
            // Load overall stats
            val vocabularyCount = vocabularyRepository.getVocabularyCount()
            val masteredCount = vocabularyRepository.getMasteredCount()
            val totalTimeSpent = readingStatsRepository.getTotalTimeSpent()
            
            // Calculate streak
            val streak = readingStatsRepository.calculateStreak()
            
            _overallStats.value = OverallStatsUi(
                totalArticlesRead = articlesRead,
                totalWordsRead = wordsRead,
                totalVocabulary = vocabularyCount,
                masteredVocabulary = masteredCount,
                currentStreak = streak,
                totalTimeSpentMinutes = totalTimeSpent,
                isLoaded = true
            )
            
            // Load weekly data
            loadWeeklyData()
        }
    }
    
    private suspend fun loadWeeklyData() {
        val weekData = readingStatsRepository.getWeeklyData().map { data ->
            DailyStats(
                date = data.date,
                dayName = data.dayName,
                articlesRead = data.articlesRead,
                wordsRead = data.wordsRead,
                timeSpentMinutes = data.timeSpentMinutes
            )
        }
        _weeklyData.value = weekData
    }
    
    fun refresh() {
        loadStats()
    }
}

data class OverallStatsUi(
    val totalArticlesRead: Int = 0,
    val totalWordsRead: Int = 0,
    val totalVocabulary: Int = 0,
    val masteredVocabulary: Int = 0,
    val currentStreak: Int = 0,
    val totalTimeSpentMinutes: Int = 0,
    val isLoaded: Boolean = false
)

data class DailyStats(
    val date: String,
    val dayName: String,
    val articlesRead: Int,
    val wordsRead: Int,
    val timeSpentMinutes: Int = 0
)
