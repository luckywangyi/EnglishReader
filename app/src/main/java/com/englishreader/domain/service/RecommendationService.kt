package com.englishreader.domain.service

import com.englishreader.data.local.dao.ArticleDao
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.domain.model.Article
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationService @Inject constructor(
    private val articleDao: ArticleDao,
    private val settingsRepository: SettingsRepository
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * 获取今日推荐文章
     * 优先使用缓存的今日推荐，如果没有则生成新推荐
     */
    suspend fun getTodayRecommendation(): Article? {
        val today = dateFormat.format(Date())
        val cachedId = settingsRepository.getTodayRecommendedId()
        val cachedDate = settingsRepository.getTodayRecommendedDate()
        
        // 如果今天已有推荐且文章存在，返回缓存的推荐
        if (cachedDate == today && cachedId != null) {
            val article = articleDao.getArticleById(cachedId)
            if (article != null) {
                return Article.fromEntity(article)
            }
        }
        
        // 否则生成新推荐
        return refreshRecommendation()
    }
    
    /**
     * 刷新推荐（换一篇）
     */
    suspend fun refreshRecommendation(): Article? {
        val today = dateFormat.format(Date())
        val currentRecommendedId = settingsRepository.getTodayRecommendedId()
        
        // 获取推荐文章
        val recommended = getRecommendedArticle(excludeId = currentRecommendedId)
        
        if (recommended != null) {
            // 保存新推荐
            settingsRepository.saveTodayRecommendation(recommended.id, today)
        }
        
        return recommended
    }
    
    /**
     * 推荐算法
     * 优先级：未读 > 最新发布
     * 排除指定ID的文章
     */
    private suspend fun getRecommendedArticle(excludeId: String? = null): Article? {
        // 获取所有未读文章
        val unreadArticles = articleDao.getUnreadArticles().first()
            .filter { it.id != excludeId }
            .map { Article.fromEntity(it) }
        
        if (unreadArticles.isNotEmpty()) {
            // 从未读文章中随机选择，增加多样性
            return unreadArticles.shuffled().first()
        }
        
        // 如果没有未读，从所有文章中选择最新的
        val allArticles = articleDao.getAllArticles().first()
            .filter { it.id != excludeId }
            .map { Article.fromEntity(it) }
        
        return allArticles.firstOrNull()
    }
    
    /**
     * 获取用于通知的推荐文章信息
     */
    suspend fun getRecommendationForNotification(): Pair<String, String>? {
        val article = getTodayRecommendation() ?: return null
        return Pair(article.id, article.title)
    }
}
