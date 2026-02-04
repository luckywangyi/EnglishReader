package com.englishreader.domain.model

import com.englishreader.data.local.entity.SentenceEntity

data class Sentence(
    val id: String,
    val content: String,
    val translation: String?,
    val note: String?,
    val articleId: String?,
    val articleTitle: String?,
    val savedAt: Long,
    val isFavorite: Boolean
) {
    companion object {
        fun fromEntity(entity: SentenceEntity): Sentence {
            return Sentence(
                id = entity.id,
                content = entity.content,
                translation = entity.translation,
                note = entity.note,
                articleId = entity.articleId,
                articleTitle = entity.articleTitle,
                savedAt = entity.savedAt,
                isFavorite = entity.isFavorite
            )
        }
    }
    
    fun toEntity(): SentenceEntity {
        return SentenceEntity(
            id = id,
            content = content,
            translation = translation,
            note = note,
            articleId = articleId,
            articleTitle = articleTitle,
            savedAt = savedAt,
            isFavorite = isFavorite
        )
    }
}
