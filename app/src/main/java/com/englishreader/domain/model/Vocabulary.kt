package com.englishreader.domain.model

import com.englishreader.data.local.entity.VocabularyEntity

data class Vocabulary(
    val id: String,
    val word: String,
    val meaning: String,
    val phonetic: String?,
    val context: String?,
    val articleId: String?,
    val articleTitle: String?,
    val savedAt: Long,
    val reviewCount: Int,
    val lastReviewAt: Long?,
    val isMastered: Boolean
) {
    companion object {
        fun fromEntity(entity: VocabularyEntity): Vocabulary {
            return Vocabulary(
                id = entity.id,
                word = entity.word,
                meaning = entity.meaning,
                phonetic = entity.phonetic,
                context = entity.context,
                articleId = entity.articleId,
                articleTitle = entity.articleTitle,
                savedAt = entity.savedAt,
                reviewCount = entity.reviewCount,
                lastReviewAt = entity.lastReviewAt,
                isMastered = entity.isMastered
            )
        }
    }
    
    fun toEntity(): VocabularyEntity {
        return VocabularyEntity(
            id = id,
            word = word,
            meaning = meaning,
            phonetic = phonetic,
            context = context,
            articleId = articleId,
            articleTitle = articleTitle,
            savedAt = savedAt,
            reviewCount = reviewCount,
            lastReviewAt = lastReviewAt,
            isMastered = isMastered
        )
    }
}
