package com.englishreader.data.repository

import com.englishreader.data.local.dao.VocabularyDao
import com.englishreader.data.local.entity.VocabularyEntity
import com.englishreader.domain.model.Vocabulary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao
) {
    fun getAllVocabulary(): Flow<List<Vocabulary>> {
        return vocabularyDao.getAllVocabulary().map { list ->
            list.map { Vocabulary.fromEntity(it) }
        }
    }
    
    fun getUnmasteredVocabulary(): Flow<List<Vocabulary>> {
        return vocabularyDao.getUnmasteredVocabulary().map { list ->
            list.map { Vocabulary.fromEntity(it) }
        }
    }
    
    fun getMasteredVocabulary(): Flow<List<Vocabulary>> {
        return vocabularyDao.getMasteredVocabulary().map { list ->
            list.map { Vocabulary.fromEntity(it) }
        }
    }
    
    fun getVocabularyByArticle(articleId: String): Flow<List<Vocabulary>> {
        return vocabularyDao.getVocabularyByArticle(articleId).map { list ->
            list.map { Vocabulary.fromEntity(it) }
        }
    }
    
    suspend fun saveVocabulary(
        word: String,
        meaning: String,
        phonetic: String? = null,
        context: String? = null,
        articleId: String? = null,
        articleTitle: String? = null
    ): Boolean {
        // Check if already exists
        if (vocabularyDao.vocabularyExists(word.lowercase())) {
            return false
        }
        
        val entity = VocabularyEntity(
            id = UUID.randomUUID().toString(),
            word = word.lowercase(),
            meaning = meaning,
            phonetic = phonetic,
            context = context,
            articleId = articleId,
            articleTitle = articleTitle,
            savedAt = System.currentTimeMillis()
        )
        vocabularyDao.insertVocabulary(entity)
        return true
    }
    
    suspend fun deleteVocabulary(id: String) {
        vocabularyDao.deleteVocabularyById(id)
    }
    
    suspend fun updateMasteredStatus(id: String, isMastered: Boolean) {
        vocabularyDao.updateMasteredStatus(id, isMastered)
    }
    
    suspend fun updateReviewStatus(id: String) {
        vocabularyDao.updateReviewStatus(id, System.currentTimeMillis())
    }
    
    suspend fun getVocabularyCount(): Int {
        return vocabularyDao.getVocabularyCount()
    }
    
    suspend fun getMasteredCount(): Int {
        return vocabularyDao.getMasteredCount()
    }
    
    suspend fun getRandomVocabulary(count: Int): List<Vocabulary> {
        return vocabularyDao.getRandomVocabulary(count).map { Vocabulary.fromEntity(it) }
    }
    
    suspend fun vocabularyExists(word: String): Boolean {
        return vocabularyDao.vocabularyExists(word.lowercase())
    }
}
