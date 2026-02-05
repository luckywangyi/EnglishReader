package com.englishreader.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishreader.data.local.entity.CustomRssSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRssSourceDao {
    
    /**
     * 获取所有自定义 RSS 源
     */
    @Query("SELECT * FROM custom_rss_sources ORDER BY createdAt DESC")
    fun getAllSources(): Flow<List<CustomRssSourceEntity>>
    
    /**
     * 获取启用的自定义 RSS 源
     */
    @Query("SELECT * FROM custom_rss_sources WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabledSources(): Flow<List<CustomRssSourceEntity>>
    
    /**
     * 获取启用的自定义 RSS 源（挂起函数版本）
     */
    @Query("SELECT * FROM custom_rss_sources WHERE isEnabled = 1")
    suspend fun getEnabledSourcesList(): List<CustomRssSourceEntity>
    
    /**
     * 根据 ID 获取 RSS 源
     */
    @Query("SELECT * FROM custom_rss_sources WHERE id = :id")
    suspend fun getSourceById(id: String): CustomRssSourceEntity?
    
    /**
     * 根据 URL 获取 RSS 源（用于去重）
     */
    @Query("SELECT * FROM custom_rss_sources WHERE url = :url LIMIT 1")
    suspend fun getSourceByUrl(url: String): CustomRssSourceEntity?
    
    /**
     * 插入新的 RSS 源
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: CustomRssSourceEntity)
    
    /**
     * 更新 RSS 源
     */
    @Update
    suspend fun updateSource(source: CustomRssSourceEntity)
    
    /**
     * 删除 RSS 源
     */
    @Delete
    suspend fun deleteSource(source: CustomRssSourceEntity)
    
    /**
     * 根据 ID 删除 RSS 源
     */
    @Query("DELETE FROM custom_rss_sources WHERE id = :id")
    suspend fun deleteSourceById(id: String)
    
    /**
     * 更新启用状态
     */
    @Query("UPDATE custom_rss_sources SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabledStatus(id: String, isEnabled: Boolean)
    
    /**
     * 更新最后抓取时间
     */
    @Query("UPDATE custom_rss_sources SET lastFetchedAt = :timestamp WHERE id = :id")
    suspend fun updateLastFetchedAt(id: String, timestamp: Long)
    
    /**
     * 更新文章数量
     */
    @Query("UPDATE custom_rss_sources SET articleCount = :count WHERE id = :id")
    suspend fun updateArticleCount(id: String, count: Int)
    
    /**
     * 获取自定义 RSS 源数量
     */
    @Query("SELECT COUNT(*) FROM custom_rss_sources")
    suspend fun getSourceCount(): Int
    
    /**
     * 检查 URL 是否已存在
     */
    @Query("SELECT EXISTS(SELECT 1 FROM custom_rss_sources WHERE url = :url)")
    suspend fun urlExists(url: String): Boolean
}
