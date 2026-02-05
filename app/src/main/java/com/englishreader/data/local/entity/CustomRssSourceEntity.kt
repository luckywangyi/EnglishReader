package com.englishreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 用户自定义的 RSS 源
 */
@Entity(tableName = "custom_rss_sources")
data class CustomRssSourceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,                    // 订阅源名称
    val url: String,                     // RSS 订阅地址
    val category: String = "CUSTOM",     // 分类
    val iconUrl: String? = null,         // 图标 URL
    val isEnabled: Boolean = true,       // 是否启用
    val createdAt: Long = System.currentTimeMillis(),
    val lastFetchedAt: Long? = null,     // 最后抓取时间
    val articleCount: Int = 0            // 文章数量
)
