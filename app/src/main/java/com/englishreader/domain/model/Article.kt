package com.englishreader.domain.model

import com.englishreader.data.local.entity.ArticleEntity

data class Article(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val description: String?,
    val content: String,
    val originalUrl: String,
    val imageUrl: String?,
    val author: String?,
    val publishedAt: Long,
    val wordCount: Int,
    val difficultyLevel: DifficultyLevel?,
    val topics: List<String>,
    val summaryEn: String?,
    val summaryCn: String?,
    val keyVocabulary: List<KeyWord>,
    val questions: List<ComprehensionQuestion>,
    val isAnalyzed: Boolean,
    val isRead: Boolean,
    val readProgress: Float,
    val lastReadAt: Long?,
    val isFavorite: Boolean
) {
    companion object {
        fun fromEntity(entity: ArticleEntity): Article {
            return Article(
                id = entity.id,
                sourceId = entity.sourceId,
                sourceName = entity.sourceName,
                title = entity.title,
                description = entity.description,
                content = entity.content,
                originalUrl = entity.originalUrl,
                imageUrl = entity.imageUrl,
                author = entity.author,
                publishedAt = entity.publishedAt,
                wordCount = entity.wordCount,
                difficultyLevel = entity.difficultyLevel?.let { DifficultyLevel.fromInt(it) },
                topics = entity.topics?.let { parseTopics(it) } ?: emptyList(),
                summaryEn = entity.summaryEn,
                summaryCn = entity.summaryCn,
                keyVocabulary = entity.keyVocabulary?.let { parseKeywords(it) } ?: emptyList(),
                questions = entity.questions?.let { parseQuestions(it) } ?: emptyList(),
                isAnalyzed = entity.isAnalyzed,
                isRead = entity.isRead,
                readProgress = entity.readProgress,
                lastReadAt = entity.lastReadAt,
                isFavorite = entity.isFavorite
            )
        }
        
        private fun parseTopics(json: String): List<String> {
            return try {
                json.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        private fun parseKeywords(json: String): List<KeyWord> {
            // Simplified parsing - in production use Gson
            return emptyList()
        }
        
        private fun parseQuestions(json: String): List<ComprehensionQuestion> {
            // Simplified parsing - in production use Gson
            return emptyList()
        }
    }
}

enum class DifficultyLevel(val level: Int, val label: String, val labelCn: String) {
    EASY(1, "Easy", "初级"),
    MEDIUM(2, "Medium", "中级"),
    HARD(3, "Hard", "中高级"),
    ADVANCED(4, "Advanced", "高级");
    
    companion object {
        fun fromInt(level: Int): DifficultyLevel {
            return entries.find { it.level == level } ?: MEDIUM
        }
    }
}

data class KeyWord(
    val word: String,
    val meaning: String,
    val example: String? = null
)

data class ComprehensionQuestion(
    val question: String,
    val answer: String
)
