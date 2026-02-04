package com.englishreader.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishreader.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    
    @Query("SELECT * FROM vocabulary ORDER BY savedAt DESC")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>
    
    @Query("SELECT * FROM vocabulary WHERE isMastered = 0 ORDER BY savedAt DESC")
    fun getUnmasteredVocabulary(): Flow<List<VocabularyEntity>>
    
    @Query("SELECT * FROM vocabulary WHERE isMastered = 1 ORDER BY savedAt DESC")
    fun getMasteredVocabulary(): Flow<List<VocabularyEntity>>
    
    @Query("SELECT * FROM vocabulary WHERE articleId = :articleId ORDER BY savedAt DESC")
    fun getVocabularyByArticle(articleId: String): Flow<List<VocabularyEntity>>
    
    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun getVocabularyById(id: String): VocabularyEntity?
    
    @Query("SELECT * FROM vocabulary WHERE word = :word LIMIT 1")
    suspend fun getVocabularyByWord(word: String): VocabularyEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(vocabulary: VocabularyEntity)
    
    @Update
    suspend fun updateVocabulary(vocabulary: VocabularyEntity)
    
    @Delete
    suspend fun deleteVocabulary(vocabulary: VocabularyEntity)
    
    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteVocabularyById(id: String)
    
    @Query("UPDATE vocabulary SET reviewCount = reviewCount + 1, lastReviewAt = :reviewAt WHERE id = :id")
    suspend fun updateReviewStatus(id: String, reviewAt: Long)
    
    @Query("UPDATE vocabulary SET isMastered = :isMastered WHERE id = :id")
    suspend fun updateMasteredStatus(id: String, isMastered: Boolean)
    
    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun getVocabularyCount(): Int
    
    @Query("SELECT COUNT(*) FROM vocabulary WHERE isMastered = 1")
    suspend fun getMasteredCount(): Int
    
    @Query("SELECT COUNT(*) FROM vocabulary WHERE savedAt >= :startOfDay AND savedAt < :endOfDay")
    suspend fun getVocabularyCountForDay(startOfDay: Long, endOfDay: Long): Int
    
    @Query("SELECT * FROM vocabulary ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomVocabulary(count: Int): List<VocabularyEntity>
    
    @Query("SELECT EXISTS(SELECT 1 FROM vocabulary WHERE word = :word)")
    suspend fun vocabularyExists(word: String): Boolean
}
