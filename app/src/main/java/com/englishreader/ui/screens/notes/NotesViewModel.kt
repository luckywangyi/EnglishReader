package com.englishreader.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.repository.SentenceRepository
import com.englishreader.data.repository.VocabularyRepository
import com.englishreader.domain.model.Sentence
import com.englishreader.domain.model.Vocabulary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotesTab {
    VOCABULARY, SENTENCES
}

enum class VocabularyFilter {
    ALL, UNMASTERED, MASTERED
}

enum class SentenceFilter {
    ALL, FAVORITES
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val sentenceRepository: SentenceRepository
) : ViewModel() {
    
    // Tab state
    private val _currentTab = MutableStateFlow(NotesTab.VOCABULARY)
    val currentTab: StateFlow<NotesTab> = _currentTab.asStateFlow()
    
    // Vocabulary state
    private val _vocabularyFilter = MutableStateFlow(VocabularyFilter.ALL)
    val vocabularyFilter: StateFlow<VocabularyFilter> = _vocabularyFilter.asStateFlow()
    
    private val _vocabularySearchQuery = MutableStateFlow("")
    val vocabularySearchQuery: StateFlow<String> = _vocabularySearchQuery.asStateFlow()
    
    private val allVocabulary = vocabularyRepository.getAllVocabulary()
    
    val vocabulary: StateFlow<List<Vocabulary>> = combine(
        allVocabulary,
        _vocabularyFilter,
        _vocabularySearchQuery
    ) { all, filter, query ->
        val filtered = when (filter) {
            VocabularyFilter.ALL -> all
            VocabularyFilter.UNMASTERED -> all.filter { !it.isMastered }
            VocabularyFilter.MASTERED -> all.filter { it.isMastered }
        }
        
        if (query.isBlank()) {
            filtered
        } else {
            filtered.filter { vocab ->
                vocab.word.contains(query, ignoreCase = true) ||
                vocab.meaning.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _vocabularyStats = MutableStateFlow(VocabularyStats(0, 0))
    val vocabularyStats: StateFlow<VocabularyStats> = _vocabularyStats.asStateFlow()
    
    // Sentence state
    private val _sentenceFilter = MutableStateFlow(SentenceFilter.ALL)
    val sentenceFilter: StateFlow<SentenceFilter> = _sentenceFilter.asStateFlow()
    
    private val _sentenceSearchQuery = MutableStateFlow("")
    val sentenceSearchQuery: StateFlow<String> = _sentenceSearchQuery.asStateFlow()
    
    private val allSentences = sentenceRepository.getAllSentences()
    
    val sentences: StateFlow<List<Sentence>> = combine(
        allSentences,
        _sentenceFilter,
        _sentenceSearchQuery
    ) { all, filter, query ->
        val filtered = when (filter) {
            SentenceFilter.ALL -> all
            SentenceFilter.FAVORITES -> all.filter { it.isFavorite }
        }
        
        if (query.isBlank()) {
            filtered
        } else {
            filtered.filter { sentence ->
                sentence.content.contains(query, ignoreCase = true) ||
                (sentence.translation?.contains(query, ignoreCase = true) == true) ||
                (sentence.note?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _sentenceStats = MutableStateFlow(SentenceStats(0, 0))
    val sentenceStats: StateFlow<SentenceStats> = _sentenceStats.asStateFlow()
    
    // 需要复习的词汇数量
    private val _dueReviewCount = MutableStateFlow(0)
    val dueReviewCount: StateFlow<Int> = _dueReviewCount.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            // Vocabulary stats
            val vocabTotal = vocabularyRepository.getVocabularyCount()
            val vocabMastered = vocabularyRepository.getMasteredCount()
            _vocabularyStats.value = VocabularyStats(vocabTotal, vocabMastered)
            
            // Sentence stats
            val sentenceTotal = sentenceRepository.getSentenceCount()
            val sentenceFavorite = sentenceRepository.getFavoriteCount()
            _sentenceStats.value = SentenceStats(sentenceTotal, sentenceFavorite)
            
            // 需要复习的词汇数量
            _dueReviewCount.value = vocabularyRepository.getDueReviewCount()
        }
    }
    
    // Tab actions
    fun setCurrentTab(tab: NotesTab) {
        _currentTab.value = tab
    }
    
    // Vocabulary actions
    fun setVocabularyFilter(filter: VocabularyFilter) {
        _vocabularyFilter.value = filter
    }
    
    fun setVocabularySearchQuery(query: String) {
        _vocabularySearchQuery.value = query
    }
    
    fun toggleVocabularyMastered(vocabulary: Vocabulary) {
        viewModelScope.launch {
            vocabularyRepository.updateMasteredStatus(vocabulary.id, !vocabulary.isMastered)
            loadStats()
        }
    }
    
    fun deleteVocabulary(vocabulary: Vocabulary) {
        viewModelScope.launch {
            vocabularyRepository.deleteVocabulary(vocabulary.id)
            loadStats()
        }
    }
    
    // Sentence actions
    fun setSentenceFilter(filter: SentenceFilter) {
        _sentenceFilter.value = filter
    }
    
    fun setSentenceSearchQuery(query: String) {
        _sentenceSearchQuery.value = query
    }
    
    fun toggleSentenceFavorite(sentence: Sentence) {
        viewModelScope.launch {
            sentenceRepository.updateFavoriteStatus(sentence.id, !sentence.isFavorite)
            loadStats()
        }
    }
    
    fun deleteSentence(sentence: Sentence) {
        viewModelScope.launch {
            sentenceRepository.deleteSentence(sentence.id)
            loadStats()
        }
    }
    
    fun updateSentenceNote(sentence: Sentence, note: String?) {
        viewModelScope.launch {
            sentenceRepository.updateNote(sentence.id, note)
        }
    }
    
    // Export
    fun getExportText(): String {
        return when (_currentTab.value) {
            NotesTab.VOCABULARY -> getVocabularyExportText()
            NotesTab.SENTENCES -> getSentenceExportText()
        }
    }
    
    private fun getVocabularyExportText(): String {
        val vocabList = vocabulary.value
        return buildString {
            appendLine("# English Reader 生词本")
            appendLine("# 共 ${vocabList.size} 个词汇")
            appendLine()
            vocabList.forEach { vocab ->
                appendLine("${vocab.word}\t${vocab.meaning}")
                vocab.context?.let { appendLine("  例句: $it") }
                appendLine()
            }
        }
    }
    
    private fun getSentenceExportText(): String {
        val sentenceList = sentences.value
        return buildString {
            appendLine("# English Reader 句子摘抄")
            appendLine("# 共 ${sentenceList.size} 个句子")
            appendLine()
            sentenceList.forEach { sentence ->
                appendLine(sentence.content)
                sentence.translation?.let { appendLine("翻译: $it") }
                sentence.note?.let { appendLine("笔记: $it") }
                sentence.articleTitle?.let { appendLine("来源: $it") }
                appendLine()
            }
        }
    }
    
    fun refresh() {
        loadStats()
    }
}

data class VocabularyStats(
    val total: Int,
    val mastered: Int
) {
    val unmastered: Int get() = total - mastered
    val masteredPercent: Float get() = if (total > 0) mastered.toFloat() / total else 0f
}

data class SentenceStats(
    val total: Int,
    val favorites: Int
)
