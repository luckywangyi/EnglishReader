package com.englishreader.ui.screens.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.repository.VocabularyRepository
import com.englishreader.domain.model.Vocabulary
import com.englishreader.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VocabularyFilter {
    ALL, UNMASTERED, MASTERED
}

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val ttsManager: TtsManager
) : ViewModel() {
    
    private val _filter = MutableStateFlow(VocabularyFilter.ALL)
    val filter: StateFlow<VocabularyFilter> = _filter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val allVocabulary = vocabularyRepository.getAllVocabulary()
    private val unmasteredVocabulary = vocabularyRepository.getUnmasteredVocabulary()
    private val masteredVocabulary = vocabularyRepository.getMasteredVocabulary()
    
    val vocabulary: StateFlow<List<Vocabulary>> = combine(
        allVocabulary,
        _filter,
        _searchQuery
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
    
    private val _stats = MutableStateFlow(VocabularyStats(0, 0))
    val stats: StateFlow<VocabularyStats> = _stats.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            val total = vocabularyRepository.getVocabularyCount()
            val mastered = vocabularyRepository.getMasteredCount()
            _stats.value = VocabularyStats(total, mastered)
        }
    }
    
    fun setFilter(filter: VocabularyFilter) {
        _filter.value = filter
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleMastered(vocabulary: Vocabulary) {
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
    
    fun markAsReviewed(vocabulary: Vocabulary) {
        viewModelScope.launch {
            vocabularyRepository.updateReviewStatus(vocabulary.id)
        }
    }
    
    /**
     * 朗读单词
     */
    fun speakWord(word: String) {
        ttsManager.speakWord(word)
    }
    
    fun getExportText(): String {
        val vocabList = vocabulary.value
        return buildString {
            appendLine("# English Reader 词汇本")
            appendLine("# 导出时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("# 共 ${vocabList.size} 个词汇")
            appendLine()
            vocabList.forEach { vocab ->
                appendLine("${vocab.word}\t${vocab.meaning}")
                vocab.context?.let { appendLine("  例句: $it") }
                appendLine()
            }
        }
    }
}

data class VocabularyStats(
    val total: Int,
    val mastered: Int
) {
    val unmastered: Int get() = total - mastered
    val masteredPercent: Float get() = if (total > 0) mastered.toFloat() / total else 0f
}
