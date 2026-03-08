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
    
    // 继续阅读文章（未读完的）
    private val _continueReadingArticle = MutableStateFlow<Article?>(null)
    val continueReadingArticle: StateFlow<Article?> = _continueReadingArticle.asStateFlow()
    
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
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    val sources: List<RssSource> = RssSources.sources
    val categories: List<Category> = Category.entries
    
    // Filtered articles based on selections and search
    val articles: StateFlow<List<Article>> = combine(
        articleRepository.getAllArticles(),
        _selectedCategory,
        _selectedDifficulty,
        _showOnlyUnread,
        _searchQuery
    ) { articles, category, difficulty, unreadOnly, query ->
        articles.filter { article ->
            val matchesCategory = category == null || 
                RssSources.getSourceById(article.sourceId)?.category == category
            val matchesDifficulty = difficulty == null || article.difficultyLevel?.level == difficulty
            val matchesUnread = !unreadOnly || !article.isRead
            val matchesSearch = query.isBlank() || 
                article.title.contains(query, ignoreCase = true) ||
                (article.description?.contains(query, ignoreCase = true) == true) ||
                article.sourceName.contains(query, ignoreCase = true)
            
            matchesCategory && matchesDifficulty && matchesUnread && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        viewModelScope.launch {
            articleRepository.cleanupOldArticles()
            
            val articleCount = articleRepository.getArticleCount()
            if (articleCount == 0) {
                refreshArticles()
            } else {
                // 已有文章，后台静默预加载内容不足的文章全文
                launch { articleRepository.prefetchAllFullContent() }
            }
            loadRecommendation()
            loadContinueReading()
        }
    }
    
    private fun loadContinueReading() {
        viewModelScope.launch {
            _continueReadingArticle.value = articleRepository.getLastInProgressArticle()
        }
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
                    // RSS 入库完成后，后台预加载全文
                    launch { articleRepository.prefetchAllFullContent() }
                },
                onFailure = { e ->
                    _error.value = e.message ?: "刷新文章失败，请检查网络连接"
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
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun clearFilters() {
        _selectedCategory.value = null
        _selectedSource.value = null
        _selectedDifficulty.value = null
        _showOnlyUnread.value = false
        _searchQuery.value = ""
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
