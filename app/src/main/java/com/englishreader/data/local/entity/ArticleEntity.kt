package com.englishreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
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
    val fetchedAt: Long,
    val wordCount: Int = 0,
    
    // AI Analysis Results (lazy loaded)
    val difficultyLevel: Int? = null,
    val topics: String? = null,          // JSON array ["tech", "science"]
    val summaryEn: String? = null,
    val summaryCn: String? = null,
    val keyVocabulary: String? = null,   // JSON array [{"word": "...", "meaning": "..."}]
    val questions: String? = null,        // JSON array [{"q": "...", "a": "..."}]
    val isAnalyzed: Boolean = false,
    
    // Reading State
    val isRead: Boolean = false,
    val readProgress: Float = 0f,
    val lastReadAt: Long? = null,
    val isFavorite: Boolean = false,
    val readTimeMinutes: Int = 0
)
