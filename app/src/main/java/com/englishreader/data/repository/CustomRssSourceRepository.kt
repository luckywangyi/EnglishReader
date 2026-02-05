package com.englishreader.data.repository

import com.englishreader.data.local.dao.CustomRssSourceDao
import com.englishreader.data.local.entity.CustomRssSourceEntity
import com.englishreader.domain.model.RssSource
import com.englishreader.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomRssSourceRepository @Inject constructor(
    private val customRssSourceDao: CustomRssSourceDao
) {
    /**
     * 获取所有自定义 RSS 源
     */
    fun getAllSources(): Flow<List<CustomRssSourceEntity>> {
        return customRssSourceDao.getAllSources()
    }
    
    /**
     * 获取启用的自定义 RSS 源
     */
    fun getEnabledSources(): Flow<List<CustomRssSourceEntity>> {
        return customRssSourceDao.getEnabledSources()
    }
    
    /**
     * 获取启用的自定义 RSS 源（转换为 RssSource）
     */
    suspend fun getEnabledRssSources(): List<RssSource> {
        return customRssSourceDao.getEnabledSourcesList().map { entity ->
            RssSource(
                id = entity.id,
                name = entity.name,
                url = entity.url,
                category = try { Category.valueOf(entity.category) } catch (e: Exception) { Category.CUSTOM },
                icon = null,
                iconUrl = entity.iconUrl,
                isEnabled = entity.isEnabled
            )
        }
    }
    
    /**
     * 添加新的 RSS 源
     * @return true 如果添加成功，false 如果 URL 已存在
     */
    suspend fun addSource(
        name: String,
        url: String,
        category: String = "CUSTOM"
    ): Boolean {
        // 检查 URL 是否已存在
        if (customRssSourceDao.urlExists(url)) {
            return false
        }
        
        val entity = CustomRssSourceEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            url = url,
            category = category
        )
        
        customRssSourceDao.insertSource(entity)
        return true
    }
    
    /**
     * 更新 RSS 源
     */
    suspend fun updateSource(source: CustomRssSourceEntity) {
        customRssSourceDao.updateSource(source)
    }
    
    /**
     * 删除 RSS 源
     */
    suspend fun deleteSource(id: String) {
        customRssSourceDao.deleteSourceById(id)
    }
    
    /**
     * 切换启用状态
     */
    suspend fun toggleEnabled(id: String, isEnabled: Boolean) {
        customRssSourceDao.updateEnabledStatus(id, isEnabled)
    }
    
    /**
     * 获取源数量
     */
    suspend fun getSourceCount(): Int {
        return customRssSourceDao.getSourceCount()
    }
}
