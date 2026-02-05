package com.englishreader.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.repository.ArticleRepository
import com.englishreader.data.repository.FullContentFetchResult
import com.englishreader.data.repository.SentenceRepository
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.data.repository.TranslationRepository
import com.englishreader.data.repository.TranslationResult
import com.englishreader.data.repository.VocabularyRepository
import com.englishreader.domain.model.Article
import com.englishreader.util.TtsManager
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
    private val sentenceRepository: SentenceRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager
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

    private val _isFetchingFullContent = MutableStateFlow(false)
    val isFetchingFullContent: StateFlow<Boolean> = _isFetchingFullContent.asStateFlow()

    private val _fullContentFetchResult = MutableStateFlow<FullContentFetchResult?>(null)
    val fullContentFetchResult: StateFlow<FullContentFetchResult?> = _fullContentFetchResult.asStateFlow()
    
    private var readingStartTime: Long = 0
    
    init {
        markAsReading()
        fetchFullContentIfNeeded()
    }
    
    private fun markAsReading() {
        readingStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            articleRepository.updateReadStatus(articleId, true)
        }
    }

    private fun fetchFullContentIfNeeded() {
        if (articleId.isBlank()) return
        viewModelScope.launch {
            _isFetchingFullContent.value = true
            _fullContentFetchResult.value = articleRepository.fetchFullContentIfNeeded(articleId)
            _isFetchingFullContent.value = false
        }
    }

    fun clearFullContentFetchResult() {
        _fullContentFetchResult.value = null
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
                _translationState.value = TranslationState.SavedWord
            }
        }
    }
    
    fun saveSentence(sentence: String, translation: String?) {
        viewModelScope.launch {
            val currentArticle = article.value
            val saved = sentenceRepository.saveSentence(
                content = sentence,
                translation = translation,
                articleId = currentArticle?.id,
                articleTitle = currentArticle?.title
            )
            
            if (saved) {
                _translationState.value = TranslationState.SavedSentence
            }
        }
    }
    
    fun isMultipleWords(text: String): Boolean {
        return text.trim().split(Regex("\\s+")).size > 1
    }
    
    fun clearTranslation() {
        _translationState.value = TranslationState.Idle
    }
    
    /**
     * 朗读文本
     */
    fun speak(text: String) {
        if (isMultipleWords(text)) {
            ttsManager.speakSentence(text)
        } else {
            ttsManager.speakWord(text)
        }
    }
    
    /**
     * 停止朗读
     */
    fun stopSpeaking() {
        ttsManager.stop()
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
    
    /**
     * 保存阅读时长（在离开页面时调用）
     */
    fun saveReadingTime() {
        if (readingStartTime <= 0) return
        
        val readingTimeMs = System.currentTimeMillis() - readingStartTime
        val readingTimeMinutes = (readingTimeMs / 60000).toInt()
        
        // 至少阅读1分钟才记录
        if (readingTimeMinutes >= 1) {
            viewModelScope.launch {
                articleRepository.recordReadingTime(readingTimeMinutes)
            }
        }
        
        // 重置开始时间，避免重复记录
        readingStartTime = 0
    }
    
    override fun onCleared() {
        super.onCleared()
        // 在 ViewModel 销毁时尝试保存阅读时长
        if (readingStartTime > 0) {
            val readingTimeMs = System.currentTimeMillis() - readingStartTime
            val readingTimeMinutes = (readingTimeMs / 60000).toInt()
            if (readingTimeMinutes >= 1) {
                // 使用非阻塞方式保存
                kotlinx.coroutines.GlobalScope.launch {
                    articleRepository.recordReadingTime(readingTimeMinutes)
                }
            }
        }
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
    data object SavedWord : TranslationState()
    data object SavedSentence : TranslationState()
}
