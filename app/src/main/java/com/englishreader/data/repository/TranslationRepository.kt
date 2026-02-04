package com.englishreader.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.englishreader.data.remote.gemini.GeminiService
import com.englishreader.data.remote.gemini.WordExplanation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiService: GeminiService
) {
    private var dictDatabase: SQLiteDatabase? = null
    
    /**
     * Translate text using local dictionary first, then AI fallback
     */
    suspend fun translate(text: String, articleContext: String? = null): TranslationResult {
        val cleanText = text.trim().lowercase()
        
        // Single word: try local dictionary first
        if (cleanText.split(Regex("\\s+")).size == 1) {
            val localResult = lookupLocalDictionary(cleanText)
            if (localResult != null) {
                return TranslationResult.LocalDict(
                    word = text,
                    phonetic = localResult.phonetic,
                    translation = localResult.translation,
                    isFromDict = true
                )
            }
        }
        
        // Use AI translation
        val aiResult = geminiService.translate(text)
        return aiResult.fold(
            onSuccess = { translation ->
                TranslationResult.AITranslation(
                    original = text,
                    translation = translation,
                    isFromDict = false
                )
            },
            onFailure = { error ->
                TranslationResult.Error(error.message ?: "Translation failed")
            }
        )
    }
    
    /**
     * Get detailed word explanation using AI
     */
    suspend fun explainWord(word: String, context: String? = null): Result<WordExplanation> {
        // First try local dictionary
        val localResult = lookupLocalDictionary(word.trim().lowercase())
        if (localResult != null) {
            return Result.success(
                WordExplanation(
                    word = word,
                    phonetic = localResult.phonetic ?: "",
                    meaning = localResult.translation ?: "",
                    example = "",
                    exampleCn = ""
                )
            )
        }
        
        // Fall back to AI
        return geminiService.explainWord(word, context)
    }
    
    private suspend fun lookupLocalDictionary(word: String): DictEntry? {
        return withContext(Dispatchers.IO) {
            try {
                val db = getDictDatabase() ?: return@withContext null
                
                val cursor = db.rawQuery(
                    "SELECT word, phonetic, translation FROM stardict WHERE word = ? LIMIT 1",
                    arrayOf(word)
                )
                
                cursor.use {
                    if (it.moveToFirst()) {
                        DictEntry(
                            word = it.getString(0),
                            phonetic = it.getString(1),
                            translation = it.getString(2)
                        )
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun getDictDatabase(): SQLiteDatabase? {
        if (dictDatabase != null) return dictDatabase
        
        try {
            // Check if dictionary exists in assets
            val dbFile = File(context.filesDir, "ecdict.db")
            
            if (!dbFile.exists()) {
                // Copy from assets if available
                try {
                    context.assets.open("ecdict.db").use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    // Dictionary not bundled, AI-only mode
                    return null
                }
            }
            
            dictDatabase = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            
            return dictDatabase
        } catch (e: Exception) {
            return null
        }
    }
    
    fun closeDictionary() {
        dictDatabase?.close()
        dictDatabase = null
    }
}

sealed class TranslationResult {
    abstract val isFromDict: Boolean
    
    data class LocalDict(
        val word: String,
        val phonetic: String?,
        val translation: String?,
        override val isFromDict: Boolean = true
    ) : TranslationResult() {
        val displayText: String get() = translation ?: ""
    }
    
    data class AITranslation(
        val original: String,
        val translation: String,
        override val isFromDict: Boolean = false
    ) : TranslationResult() {
        val displayText: String get() = translation
    }
    
    data class Error(
        val message: String,
        override val isFromDict: Boolean = false
    ) : TranslationResult()
}

private data class DictEntry(
    val word: String,
    val phonetic: String?,
    val translation: String?
)
