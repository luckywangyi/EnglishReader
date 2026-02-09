package com.englishreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishreader.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>
    
    @Query("SELECT * FROM articles WHERE sourceId = :sourceId ORDER BY publishedAt DESC")
    fun getArticlesBySource(sourceId: String): Flow<List<ArticleEntity>>
    
    @Query("SELECT * FROM articles WHERE difficultyLevel = :level ORDER BY publishedAt DESC")
    fun getArticlesByDifficulty(level: Int): Flow<List<ArticleEntity>>
    
    @Query("SELECT * FROM articles WHERE isFavorite = 1 ORDER BY publishedAt DESC")
    fun getFavoriteArticles(): Flow<List<ArticleEntity>>
    
    @Query("SELECT * FROM articles WHERE isRead = 0 ORDER BY publishedAt DESC")
    fun getUnreadArticles(): Flow<List<ArticleEntity>>
    
    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: String): ArticleEntity?
    
    @Query("SELECT * FROM articles WHERE id = :id")
    fun getArticleByIdFlow(id: String): Flow<ArticleEntity?>
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: ArticleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>)
    
    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteArticleById(id: String)
    
    @Query("UPDATE articles SET isRead = :isRead, lastReadAt = :lastReadAt WHERE id = :id")
    suspend fun updateReadStatus(id: String, isRead: Boolean, lastReadAt: Long)
    
    @Query("UPDATE articles SET readProgress = :progress WHERE id = :id")
    suspend fun updateReadProgress(id: String, progress: Float)
    
    @Query("UPDATE articles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)
    
    @Query("""
        UPDATE articles SET 
            difficultyLevel = :difficultyLevel,
            topics = :topics,
            summaryEn = :summaryEn,
            summaryCn = :summaryCn,
            keyVocabulary = :keyVocabulary,
            questions = :questions,
            isAnalyzed = 1
        WHERE id = :id
    """)
    suspend fun updateAnalysis(
        id: String,
        difficultyLevel: Int,
        topics: String,
        summaryEn: String,
        summaryCn: String,
        keyVocabulary: String,
        questions: String
    )
    
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getArticleCount(): Int
    
    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 1")
    suspend fun getReadArticleCount(): Int
    
    @Query("SELECT SUM(wordCount) FROM articles WHERE isRead = 1")
    suspend fun getTotalWordsRead(): Int?
    
    @Query("DELETE FROM articles WHERE fetchedAt < :timestamp")
    suspend fun deleteOldArticles(timestamp: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE originalUrl = :url)")
    suspend fun articleExists(url: String): Boolean
    
    /**
     * 获取最近未读完的文章（有进度但未完成）
     */
    @Query("SELECT * FROM articles WHERE readProgress > 0.05 AND readProgress < 0.95 ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun getLastInProgressArticle(): ArticleEntity?
}
