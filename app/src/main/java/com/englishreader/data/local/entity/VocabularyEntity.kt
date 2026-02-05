package com.englishreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val meaning: String,
    val phonetic: String? = null,
    val context: String? = null,           // Example sentence from article
    val articleId: String? = null,
    val articleTitle: String? = null,
    val savedAt: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0,
    val lastReviewAt: Long? = null,
    val isMastered: Boolean = false,
    // 间隔重复算法 (Spaced Repetition) 字段
    val nextReviewAt: Long = System.currentTimeMillis(),  // 下次复习时间
    val easeFactor: Float = 2.5f,                         // 难度因子 (SM-2 默认 2.5)
    val interval: Int = 0                                  // 复习间隔（天）
)
