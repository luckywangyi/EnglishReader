package com.englishreader.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.repository.ArticleRepository
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.data.repository.TranslationRepository
import com.englishreader.data.repository.TranslationResult
import com.englishreader.data.repository.VocabularyRepository
import com.englishreader.domain.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val translationRepository: TranslationRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val articleId: String = savedStateHandle.get<String>("articleId") ?: ""
    
    val article: StateFlow<Article?> = articleRepository.getArticleByIdFlow(articleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val fontSize: StateFlow<Int> = settingsRepository.readingFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18)
    
    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()
    
    private val _showSummary = MutableStateFlow(false)
    val showSummary: StateFlow<Boolean> = _showSummary.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private var readingStartTime: Long = 0
    
    init {
        markAsReading()
    }
    
    private fun markAsReading() {
        readingStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            articleRepository.updateReadStatus(articleId, true)
        }
    }
    
    fun updateReadProgress(progress: Float) {
        viewModelScope.launch {
            articleRepository.updateReadProgress(articleId, progress)
        }
    }
    
    fun translate(text: String) {
        viewModelScope.launch {
            _translationState.value = TranslationState.Loading
            
            val result = translationRepository.translate(text)
            
            _translationState.value = when (result) {
                is TranslationResult.LocalDict -> TranslationState.Success(
                    original = result.word,
                    translation = result.displayText,
                    phonetic = result.phonetic,
                    isFromDict = true
                )
                is TranslationResult.AITranslation -> TranslationState.Success(
                    original = result.original,
                    translation = result.translation,
                    phonetic = null,
                    isFromDict = false
                )
                is TranslationResult.Error -> TranslationState.Error(result.message)
            }
        }
    }
    
    fun saveVocabulary(word: String, meaning: String, context: String? = null) {
        viewModelScope.launch {
            val currentArticle = article.value
            val saved = vocabularyRepository.saveVocabulary(
                word = word,
                meaning = meaning,
                context = context,
                articleId = currentArticle?.id,
                articleTitle = currentArticle?.title
            )
            
            if (saved) {
                _translationState.value = TranslationState.Saved
            }
        }
    }
    
    fun clearTranslation() {
        _translationState.value = TranslationState.Idle
    }
    
    fun toggleFavorite() {
        viewModelScope.launch {
            article.value?.let { art ->
                articleRepository.updateFavoriteStatus(art.id, !art.isFavorite)
            }
        }
    }
    
    fun toggleSummary() {
        _showSummary.value = !_showSummary.value
    }
    
    fun analyzeArticle() {
        viewModelScope.launch {
            article.value?.let { art ->
                if (!art.isAnalyzed) {
                    _isAnalyzing.value = true
                    articleRepository.analyzeArticle(art)
                    _isAnalyzing.value = false
                }
            }
        }
    }
    
    fun increaseFontSize() {
        viewModelScope.launch {
            val current = fontSize.value
            if (current < 28) {
                settingsRepository.setReadingFontSize(current + 2)
            }
        }
    }
    
    fun decreaseFontSize() {
        viewModelScope.launch {
            val current = fontSize.value
            if (current > 14) {
                settingsRepository.setReadingFontSize(current - 2)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Could save reading time stats here
    }
}

sealed class TranslationState {
    data object Idle : TranslationState()
    data object Loading : TranslationState()
    data class Success(
        val original: String,
        val translation: String,
        val phonetic: String?,
        val isFromDict: Boolean
    ) : TranslationState()
    data class Error(val message: String) : TranslationState()
    data object Saved : TranslationState()
}
