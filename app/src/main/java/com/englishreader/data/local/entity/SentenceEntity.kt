package com.englishreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sentences")
data class SentenceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val content: String,                    // 英文原句
    val translation: String? = null,        // 中文翻译
    val note: String? = null,               // 用户笔记/备注
    val articleId: String? = null,          // 来源文章ID
    val articleTitle: String? = null,       // 来源文章标题
    val savedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
