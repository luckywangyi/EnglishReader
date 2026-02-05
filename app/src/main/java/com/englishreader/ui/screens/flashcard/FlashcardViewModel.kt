package com.englishreader.ui.screens.flashcard

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SM-2 间隔重复算法
 * 
 * 评分：
 * 0 - 完全不记得
 * 1 - 不正确，但看到答案后记起来了
 * 2 - 不正确，但答案感觉熟悉
 * 3 - 正确，但需要很长时间想起来
 * 4 - 正确，有些犹豫
 * 5 - 完美记忆
 */
enum class ReviewQuality(val score: Int) {
    FORGOT(0),           // 完全忘记
    HARD(3),             // 想起来了，但困难
    GOOD(4),             // 正常记起来
    EASY(5)              // 轻松记起来
}

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val ttsManager: TtsManager
) : ViewModel() {
    
    // 需要复习的词汇列表
    val dueVocabulary: StateFlow<List<Vocabulary>> = vocabularyRepository.getVocabularyDueForReview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 当前卡片索引
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    // 是否显示答案（卡片是否翻转）
    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()
    
    // 复习统计
    private val _reviewStats = MutableStateFlow(ReviewStats())
    val reviewStats: StateFlow<ReviewStats> = _reviewStats.asStateFlow()
    
    // 复习是否完成
    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()
    
    /**
     * 获取当前词汇
     */
    fun getCurrentVocabulary(): Vocabulary? {
        val list = dueVocabulary.value
        val index = _currentIndex.value
        return if (index < list.size) list[index] else null
    }
    
    /**
     * 翻转卡片
     */
    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }
    
    /**
     * 显示答案
     */
    fun showAnswer() {
        _isFlipped.value = true
    }
    
    /**
     * 朗读当前单词
     */
    fun speakCurrentWord() {
        getCurrentVocabulary()?.let { vocab ->
            ttsManager.speakWord(vocab.word)
        }
    }
    
    /**
     * 评价当前卡片并进入下一张
     */
    fun rateCard(quality: ReviewQuality) {
        val currentVocab = getCurrentVocabulary() ?: return
        
        viewModelScope.launch {
            // 计算新的间隔重复参数
            val result = calculateNextReview(
                currentEaseFactor = currentVocab.easeFactor,
                currentInterval = currentVocab.interval,
                quality = quality
            )
            
            // 更新数据库
            vocabularyRepository.updateSpacedRepetition(
                id = currentVocab.id,
                nextReviewAt = result.nextReviewAt,
                easeFactor = result.newEaseFactor,
                interval = result.newInterval
            )
            
            // 如果评分为 EASY 且复习次数足够，标记为已掌握
            if (quality == ReviewQuality.EASY && result.newInterval >= 21) {
                vocabularyRepository.updateMasteredStatus(currentVocab.id, true)
            }
            
            // 更新统计
            _reviewStats.value = _reviewStats.value.copy(
                reviewed = _reviewStats.value.reviewed + 1,
                correct = if (quality.score >= 3) _reviewStats.value.correct + 1 else _reviewStats.value.correct,
                incorrect = if (quality.score < 3) _reviewStats.value.incorrect + 1 else _reviewStats.value.incorrect
            )
            
            // 进入下一张卡片
            moveToNext()
        }
    }
    
    /**
     * 进入下一张卡片
     */
    private fun moveToNext() {
        _isFlipped.value = false
        val nextIndex = _currentIndex.value + 1
        
        if (nextIndex >= dueVocabulary.value.size) {
            _isCompleted.value = true
        } else {
            _currentIndex.value = nextIndex
        }
    }
    
    /**
     * SM-2 算法核心
     */
    private fun calculateNextReview(
        currentEaseFactor: Float,
        currentInterval: Int,
        quality: ReviewQuality
    ): SpacedRepetitionResult {
        val q = quality.score
        
        // 计算新的 easeFactor
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        var newEaseFactor = currentEaseFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (newEaseFactor < 1.3f) newEaseFactor = 1.3f
        
        // 计算新的间隔
        val newInterval = when {
            q < 3 -> 1 // 回答错误，重新开始
            currentInterval == 0 -> 1
            currentInterval == 1 -> 6
            else -> (currentInterval * newEaseFactor).toInt()
        }
        
        // 计算下次复习时间
        val nextReviewAt = System.currentTimeMillis() + (newInterval * 24 * 60 * 60 * 1000L)
        
        return SpacedRepetitionResult(
            newEaseFactor = newEaseFactor,
            newInterval = newInterval,
            nextReviewAt = nextReviewAt
        )
    }
    
    /**
     * 重置复习（重新开始）
     */
    fun resetReview() {
        _currentIndex.value = 0
        _isFlipped.value = false
        _isCompleted.value = false
        _reviewStats.value = ReviewStats()
    }
}

data class SpacedRepetitionResult(
    val newEaseFactor: Float,
    val newInterval: Int,
    val nextReviewAt: Long
)

data class ReviewStats(
    val reviewed: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0
) {
    val accuracy: Float get() = if (reviewed > 0) correct.toFloat() / reviewed else 0f
}
