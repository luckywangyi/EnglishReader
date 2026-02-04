package com.englishreader.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.remote.rss.RssSources
import com.englishreader.data.repository.ArticleRepository
import com.englishreader.domain.model.Article
import com.englishreader.domain.model.Category
import com.englishreader.domain.model.RssSource
import com.englishreader.domain.service.RecommendationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val recommendationService: RecommendationService
) : ViewModel() {
    
    // 今日推荐文章
    private val _recommendedArticle = MutableStateFlow<Article?>(null)
    val recommendedArticle: StateFlow<Article?> = _recommendedArticle.asStateFlow()
    
    private val _isLoadingRecommendation = MutableStateFlow(false)
    val isLoadingRecommendation: StateFlow<Boolean> = _isLoadingRecommendation.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()
    
    private val _selectedSource = MutableStateFlow<RssSource?>(null)
    val selectedSource: StateFlow<RssSource?> = _selectedSource.asStateFlow()
    
    private val _selectedDifficulty = MutableStateFlow<Int?>(null)
    val selectedDifficulty: StateFlow<Int?> = _selectedDifficulty.asStateFlow()
    
    private val _showOnlyUnread = MutableStateFlow(false)
    val showOnlyUnread: StateFlow<Boolean> = _showOnlyUnread.asStateFlow()
    
    val sources: List<RssSource> = RssSources.sources
    val categories: List<Category> = Category.entries
    
    // Filtered articles based on selections
    val articles: StateFlow<List<Article>> = combine(
        articleRepository.getAllArticles(),
        _selectedCategory,
        _selectedSource,
        _selectedDifficulty,
        _showOnlyUnread
    ) { articles, category, source, difficulty, unreadOnly ->
        articles.filter { article ->
            val matchesCategory = category == null || 
                RssSources.getSourceById(article.sourceId)?.category == category
            val matchesSource = source == null || article.sourceId == source.id
            val matchesDifficulty = difficulty == null || article.difficultyLevel?.level == difficulty
            val matchesUnread = !unreadOnly || !article.isRead
            
            matchesCategory && matchesSource && matchesDifficulty && matchesUnread
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        refreshArticles()
        loadRecommendation()
    }
    
    private fun loadRecommendation() {
        viewModelScope.launch {
            _isLoadingRecommendation.value = true
            _recommendedArticle.value = recommendationService.getTodayRecommendation()
            _isLoadingRecommendation.value = false
        }
    }
    
    fun refreshRecommendation() {
        viewModelScope.launch {
            _isLoadingRecommendation.value = true
            _recommendedArticle.value = recommendationService.refreshRecommendation()
            _isLoadingRecommendation.value = false
        }
    }
    
    fun refreshArticles() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            
            val result = articleRepository.refreshArticles()
            result.fold(
                onSuccess = { count ->
                    // Success - articles are automatically updated via Flow
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to refresh articles"
                }
            )
            
            _isRefreshing.value = false
        }
    }
    
    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
        _selectedSource.value = null  // Reset source when category changes
    }
    
    fun selectSource(source: RssSource?) {
        _selectedSource.value = source
    }
    
    fun selectDifficulty(level: Int?) {
        _selectedDifficulty.value = level
    }
    
    fun toggleUnreadOnly() {
        _showOnlyUnread.value = !_showOnlyUnread.value
    }
    
    fun clearFilters() {
        _selectedCategory.value = null
        _selectedSource.value = null
        _selectedDifficulty.value = null
        _showOnlyUnread.value = false
    }
    
    fun toggleFavorite(article: Article) {
        viewModelScope.launch {
            articleRepository.updateFavoriteStatus(article.id, !article.isFavorite)
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
