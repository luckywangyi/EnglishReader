package com.englishreader.data.repository

import com.englishreader.data.local.dao.ArticleDao
import com.englishreader.data.local.entity.ArticleEntity
import com.englishreader.data.remote.gemini.GeminiService
import com.englishreader.data.remote.rss.RssService
import com.englishreader.data.remote.rss.RssSources
import com.englishreader.domain.model.Article
import com.englishreader.domain.model.Category
import com.englishreader.domain.model.RssSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val rssService: RssService,
    private val articleDao: ArticleDao,
    private val geminiService: GeminiService
) {
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
    
    suspend fun refreshArticles(sources: List<RssSource> = RssSources.getEnabledSources()): Result<Int> {
        return try {
            val articles = rssService.fetchAllArticles(sources)
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
        articleDao.updateReadStatus(id, isRead, System.currentTimeMillis())
    }
    
    suspend fun updateReadProgress(id: String, progress: Float) {
        articleDao.updateReadProgress(id, progress)
    }
    
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean) {
        articleDao.updateFavoriteStatus(id, isFavorite)
    }
    
    suspend fun analyzeArticle(article: Article): Result<Unit> {
        return try {
            val analysis = geminiService.analyzeArticle(article.content, article.title)
            
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
    
    suspend fun getArticleCount(): Int = articleDao.getArticleCount()
    
    suspend fun getReadArticleCount(): Int = articleDao.getReadArticleCount()
    
    suspend fun getTotalWordsRead(): Int = articleDao.getTotalWordsRead() ?: 0
    
    fun getSources(): List<RssSource> = RssSources.sources
    
    fun getSourcesByCategory(category: Category): List<RssSource> = 
        RssSources.getSourcesByCategory(category)
}
