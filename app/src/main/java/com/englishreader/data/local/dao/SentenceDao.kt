package com.englishreader.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishreader.data.local.entity.SentenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    
    @Query("SELECT * FROM sentences ORDER BY savedAt DESC")
    fun getAllSentences(): Flow<List<SentenceEntity>>
    
    @Query("SELECT * FROM sentences WHERE isFavorite = 1 ORDER BY savedAt DESC")
    fun getFavoriteSentences(): Flow<List<SentenceEntity>>
    
    @Query("SELECT * FROM sentences WHERE articleId = :articleId ORDER BY savedAt DESC")
    fun getSentencesByArticle(articleId: String): Flow<List<SentenceEntity>>
    
    @Query("SELECT * FROM sentences WHERE id = :id")
    suspend fun getSentenceById(id: String): SentenceEntity?
    
    @Query("SELECT * FROM sentences WHERE content LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    fun searchSentences(query: String): Flow<List<SentenceEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentence(sentence: SentenceEntity)
    
    @Update
    suspend fun updateSentence(sentence: SentenceEntity)
    
    @Delete
    suspend fun deleteSentence(sentence: SentenceEntity)
    
    @Query("DELETE FROM sentences WHERE id = :id")
    suspend fun deleteSentenceById(id: String)
    
    @Query("UPDATE sentences SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)
    
    @Query("UPDATE sentences SET note = :note WHERE id = :id")
    suspend fun updateNote(id: String, note: String?)
    
    @Query("UPDATE sentences SET translation = :translation WHERE id = :id")
    suspend fun updateTranslation(id: String, translation: String?)
    
    @Query("SELECT COUNT(*) FROM sentences")
    suspend fun getSentenceCount(): Int
    
    @Query("SELECT COUNT(*) FROM sentences WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM sentences WHERE content = :content)")
    suspend fun sentenceExists(content: String): Boolean
}
