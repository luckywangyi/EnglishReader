package com.englishreader.data.repository

import com.englishreader.data.local.dao.ArticleDao
import com.englishreader.data.local.dao.CustomRssSourceDao
import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.local.entity.ArticleEntity
import com.englishreader.data.local.entity.ReadingStatsEntity
import com.englishreader.data.remote.ai.AiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.englishreader.data.remote.rss.HtmlParser
import com.englishreader.data.remote.rss.RssService
import com.englishreader.data.remote.rss.RssSources
import com.englishreader.domain.model.Article
import com.englishreader.domain.model.Category
import com.englishreader.domain.model.RssSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class FullContentFetchResult {
    UPDATED,
    SKIPPED,
    TOO_SHORT,
    FAILED
}

@Singleton
class ArticleRepository @Inject constructor(
    private val rssService: RssService,
    private val articleDao: ArticleDao,
    private val aiService: AiService,
    private val htmlParser: HtmlParser,
    private val readingStatsDao: ReadingStatsDao,
    private val customRssSourceDao: CustomRssSourceDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val minWordCount = 500
    fun getAllArticles(): Flow<List<Article>> {
        return articleDao.getAllArticles().map { list ->
            list.map { Article.fromEntity(it) }
        }
    }
    
    fun getArticlesBySource(sourceId: String): Flow<List<Article>> {
        return articleDao.getArticlesBySource(sourceId).map { list ->
            list.map { Article.fromEntity(it) }
        }
    }
    
    fun getArticlesByDifficulty(level: Int): Flow<List<Article>> {
        return articleDao.getArticlesByDifficulty(level).map { list ->
            list.map { Article.fromEntity(it) }
        }
    }
    
    fun getFavoriteArticles(): Flow<List<Article>> {
        return articleDao.getFavoriteArticles().map { list ->
            list.map { Article.fromEntity(it) }
        }
    }
    
    fun getUnreadArticles(): Flow<List<Article>> {
        return articleDao.getUnreadArticles().map { list ->
            list.map { Article.fromEntity(it) }
        }
    }
    
    fun getArticleByIdFlow(id: String): Flow<Article?> {
        return articleDao.getArticleByIdFlow(id).map { entity ->
            entity?.let { Article.fromEntity(it) }
        }
    }
    
    suspend fun getArticleById(id: String): Article? {
        return articleDao.getArticleById(id)?.let { Article.fromEntity(it) }
    }

    suspend fun fetchFullContentIfNeeded(articleId: String): FullContentFetchResult {
        return try {
            val entity = articleDao.getArticleById(articleId) ?: return FullContentFetchResult.SKIPPED
            if (!shouldFetchFullContent(entity)) return FullContentFetchResult.SKIPPED

            val result = htmlParser.fetchArticleContent(entity.originalUrl)
            val content = result.getOrNull()?.content?.trim().orEmpty()
            val wordCount = content.split(Regex("\\s+")).size

            if (content.isBlank() || content.length <= entity.content.length) {
                return FullContentFetchResult.SKIPPED
            }
            
            if (wordCount < minWordCount) {
                articleDao.deleteArticleById(entity.id)
                return FullContentFetchResult.TOO_SHORT
            }

            val description = entity.description ?: createExcerpt(content)

            articleDao.updateArticle(
                entity.copy(
                    content = content,
                    description = description,
                    imageUrl = entity.imageUrl ?: result.getOrNull()?.imageUrl,
                    author = entity.author ?: result.getOrNull()?.author,
                    wordCount = wordCount
                )
            )

            FullContentFetchResult.UPDATED
        } catch (e: Exception) {
            FullContentFetchResult.FAILED
        }
    }
    
    suspend fun refreshArticles(sources: List<RssSource> = RssSources.getEnabledSources()): Result<Int> {
        return try {
            // 合并内置源和自定义源
            val customSources = customRssSourceDao.getEnabledSourcesList().map { entity ->
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
            
            val allSources = sources + customSources
            val articles = rssService.fetchAllArticles(allSources)
            articleDao.insertArticles(articles)
            Result.success(articles.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun refreshFromSource(source: RssSource): Result<Int> {
        return try {
            val result = rssService.fetchArticles(source)
            result.fold(
                onSuccess = { articles ->
                    articleDao.insertArticles(articles)
                    Result.success(articles.size)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateReadStatus(id: String, isRead: Boolean) {
        val article = articleDao.getArticleById(id)
        val wasRead = article?.isRead ?: false
        
        articleDao.updateReadStatus(id, isRead, System.currentTimeMillis())
        
        // 如果是首次标记为已读，记录阅读统计
        if (isRead && !wasRead && article != null) {
            recordReadingStats(article.wordCount)
        }
    }
    
    /**
     * 记录每日阅读统计
     */
    private suspend fun recordReadingStats(wordCount: Int) {
        val today = dateFormat.format(Date())
        val existingStats = readingStatsDao.getStatsForDate(today)
        
        if (existingStats != null) {
            // 更新现有记录
            readingStatsDao.incrementStats(
                date = today,
                articles = 1,
                words = wordCount,
                minutes = 0,
                vocabulary = 0
            )
        } else {
            // 创建新记录
            readingStatsDao.insertOrUpdateStats(
                ReadingStatsEntity(
                    date = today,
                    articlesRead = 1,
                    wordsRead = wordCount,
                    timeSpentMinutes = 0,
                    vocabularySaved = 0
                )
            )
        }
    }
    
    suspend fun updateReadProgress(id: String, progress: Float) {
        articleDao.updateReadProgress(id, progress)
    }
    
    /**
     * 记录阅读时长（分钟）
     */
    suspend fun recordReadingTime(minutes: Int) {
        if (minutes <= 0) return
        
        val today = dateFormat.format(Date())
        val existingStats = readingStatsDao.getStatsForDate(today)
        
        if (existingStats != null) {
            readingStatsDao.incrementStats(
                date = today,
                articles = 0,
                words = 0,
                minutes = minutes,
                vocabulary = 0
            )
        } else {
            readingStatsDao.insertOrUpdateStats(
                ReadingStatsEntity(
                    date = today,
                    articlesRead = 0,
                    wordsRead = 0,
                    timeSpentMinutes = minutes,
                    vocabularySaved = 0
                )
            )
        }
    }
    
    /**
     * 记录查词次数和阅读速度
     */
    suspend fun recordLookupAndWpm(lookupCount: Int, wpm: Int) {
        val today = dateFormat.format(Date())
        val existingStats = readingStatsDao.getStatsForDate(today)
        
        if (existingStats != null) {
            if (lookupCount > 0) {
                readingStatsDao.incrementLookupCount(today, lookupCount)
            }
            if (wpm > 0) {
                readingStatsDao.updateAverageWpm(today, wpm)
            }
        } else {
            readingStatsDao.insertOrUpdateStats(
                ReadingStatsEntity(
                    date = today,
                    lookupCount = lookupCount,
                    averageWpm = wpm
                )
            )
        }
    }
    
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean) {
        articleDao.updateFavoriteStatus(id, isFavorite)
    }
    
    suspend fun analyzeArticle(article: Article): Result<Unit> {
        return try {
            val analysis = aiService.analyzeArticle(article.content, article.title)
            
            analysis.fold(
                onSuccess = { result ->
                    articleDao.updateAnalysis(
                        id = article.id,
                        difficultyLevel = result.difficultyLevel,
                        topics = result.topicsJson,
                        summaryEn = result.summaryEn,
                        summaryCn = result.summaryCn,
                        keyVocabulary = result.keyVocabularyJson,
                        questions = result.questionsJson
                    )
                    Result.success(Unit)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLastInProgressArticle(): Article? {
        return articleDao.getLastInProgressArticle()?.let { Article.fromEntity(it) }
    }
    
    suspend fun getArticleCount(): Int = articleDao.getArticleCount()
    
    suspend fun getReadArticleCount(): Int = articleDao.getReadArticleCount()
    
    suspend fun getTotalWordsRead(): Int = articleDao.getTotalWordsRead() ?: 0
    
    fun getSources(): List<RssSource> = RssSources.sources
    
    fun getSourcesByCategory(category: Category): List<RssSource> = 
        RssSources.getSourcesByCategory(category)
    
    private fun shouldFetchFullContent(article: ArticleEntity): Boolean {
        val content = article.content.trim()
        if (content.isBlank()) return true
        
        val wordCount = content.split(Regex("\\s+")).size
        if (wordCount < 200 || content.length < 800) return true
        
        val lower = content.lowercase()
        return lower.contains("continue reading") ||
            lower.contains("read more") ||
            lower.contains("continue on medium")
    }
    
    private fun createExcerpt(content: String): String {
        val firstParagraph = content.split(Regex("\\n\\s*\\n"))
            .firstOrNull()
            ?.trim()
            .orEmpty()
        
        return if (firstParagraph.length > 200) {
            firstParagraph.take(200) + "..."
        } else {
            firstParagraph
        }
    }
}
