package com.englishreader.data.repository

import com.englishreader.data.local.dao.SentenceDao
import com.englishreader.data.local.entity.SentenceEntity
import com.englishreader.domain.model.Sentence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentenceRepository @Inject constructor(
    private val sentenceDao: SentenceDao
) {
    fun getAllSentences(): Flow<List<Sentence>> {
        return sentenceDao.getAllSentences().map { list ->
            list.map { Sentence.fromEntity(it) }
        }
    }
    
    fun getFavoriteSentences(): Flow<List<Sentence>> {
        return sentenceDao.getFavoriteSentences().map { list ->
            list.map { Sentence.fromEntity(it) }
        }
    }
    
    fun getSentencesByArticle(articleId: String): Flow<List<Sentence>> {
        return sentenceDao.getSentencesByArticle(articleId).map { list ->
            list.map { Sentence.fromEntity(it) }
        }
    }
    
    fun searchSentences(query: String): Flow<List<Sentence>> {
        return sentenceDao.searchSentences(query).map { list ->
            list.map { Sentence.fromEntity(it) }
        }
    }
    
    suspend fun saveSentence(
        content: String,
        translation: String? = null,
        note: String? = null,
        articleId: String? = null,
        articleTitle: String? = null
    ): Boolean {
        // Check if already exists
        if (sentenceDao.sentenceExists(content)) {
            return false
        }
        
        val entity = SentenceEntity(
            id = UUID.randomUUID().toString(),
            content = content,
            translation = translation,
            note = note,
            articleId = articleId,
            articleTitle = articleTitle,
            savedAt = System.currentTimeMillis()
        )
        sentenceDao.insertSentence(entity)
        return true
    }
    
    suspend fun updateSentence(sentence: Sentence) {
        sentenceDao.updateSentence(sentence.toEntity())
    }
    
    suspend fun deleteSentence(id: String) {
        sentenceDao.deleteSentenceById(id)
    }
    
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean) {
        sentenceDao.updateFavoriteStatus(id, isFavorite)
    }
    
    suspend fun updateNote(id: String, note: String?) {
        sentenceDao.updateNote(id, note)
    }
    
    suspend fun updateTranslation(id: String, translation: String?) {
        sentenceDao.updateTranslation(id, translation)
    }
    
    suspend fun getSentenceCount(): Int {
        return sentenceDao.getSentenceCount()
    }
    
    suspend fun getFavoriteCount(): Int {
        return sentenceDao.getFavoriteCount()
    }
    
    suspend fun sentenceExists(content: String): Boolean {
        return sentenceDao.sentenceExists(content)
    }
}
