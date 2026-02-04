package com.englishreader.data.remote.gemini

import com.englishreader.data.repository.SettingsRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private var generativeModel: GenerativeModel? = null
    
    private suspend fun getModel(): GenerativeModel? {
        val apiKey = settingsRepository.geminiApiKey.first()
        if (apiKey.isBlank()) return null
        
        if (generativeModel == null) {
            generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = 2048
                }
            )
        }
        return generativeModel
    }
    
    suspend fun translate(text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val model = getModel() ?: return@withContext Result.failure(
                    Exception("API Key not configured")
                )
                
                val prompt = """
                    Translate the following English text to Chinese. 
                    Only return the translation, no explanations.
                    
                    Text: $text
                """.trimIndent()
                
                val response = model.generateContent(prompt)
                val translation = response.text?.trim() ?: ""
                
                Result.success(translation)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun analyzeArticle(content: String, title: String): Result<ArticleAnalysis> {
        return withContext(Dispatchers.IO) {
            try {
                val model = getModel() ?: return@withContext Result.failure(
                    Exception("API Key not configured")
                )
                
                // Truncate content if too long
                val truncatedContent = if (content.length > 4000) {
                    content.take(4000) + "..."
                } else content
                
                val prompt = """
                    Analyze the following English article and return a JSON response with this exact structure:
                    {
                        "difficulty_level": <1-4 integer>,
                        "difficulty_reason": "<brief reason for difficulty assessment>",
                        "topics": ["<topic1>", "<topic2>"],
                        "summary_en": "<English summary in 50-100 words>",
                        "summary_cn": "<Chinese summary in 100-150 characters>",
                        "key_vocabulary": [
                            {"word": "<important word>", "meaning": "<Chinese meaning>"}
                        ],
                        "questions": [
                            {"q": "<comprehension question>", "a": "<brief answer>"}
                        ]
                    }
                    
                    Difficulty levels:
                    1 = Easy (basic vocabulary, simple sentences)
                    2 = Medium (intermediate vocabulary, compound sentences)
                    3 = Hard (advanced vocabulary, complex structures)
                    4 = Advanced (academic/specialized content)
                    
                    Provide 5-8 key vocabulary items and 2-3 comprehension questions.
                    
                    Title: $title
                    
                    Content:
                    $truncatedContent
                """.trimIndent()
                
                val response = model.generateContent(prompt)
                val responseText = response.text?.trim() ?: ""
                
                // Parse JSON response
                val analysis = parseAnalysisResponse(responseText)
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun parseAnalysisResponse(response: String): ArticleAnalysis {
        try {
            // Extract JSON from response (it might have markdown code blocks)
            val jsonString = response
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val json = JSONObject(jsonString)
            
            val difficultyLevel = json.optInt("difficulty_level", 2)
            val summaryEn = json.optString("summary_en", "")
            val summaryCn = json.optString("summary_cn", "")
            
            val topicsArray = json.optJSONArray("topics") ?: JSONArray()
            val topics = (0 until topicsArray.length()).map { topicsArray.getString(it) }
            
            val vocabArray = json.optJSONArray("key_vocabulary") ?: JSONArray()
            val keyVocabulary = (0 until vocabArray.length()).map { i ->
                val item = vocabArray.getJSONObject(i)
                KeyVocabItem(
                    word = item.optString("word", ""),
                    meaning = item.optString("meaning", "")
                )
            }
            
            val questionsArray = json.optJSONArray("questions") ?: JSONArray()
            val questions = (0 until questionsArray.length()).map { i ->
                val item = questionsArray.getJSONObject(i)
                QuestionItem(
                    question = item.optString("q", ""),
                    answer = item.optString("a", "")
                )
            }
            
            return ArticleAnalysis(
                difficultyLevel = difficultyLevel,
                topics = topics,
                summaryEn = summaryEn,
                summaryCn = summaryCn,
                keyVocabulary = keyVocabulary,
                questions = questions
            )
        } catch (e: Exception) {
            // Return default values on parse error
            return ArticleAnalysis(
                difficultyLevel = 2,
                topics = emptyList(),
                summaryEn = "",
                summaryCn = "",
                keyVocabulary = emptyList(),
                questions = emptyList()
            )
        }
    }
    
    suspend fun explainWord(word: String, context: String? = null): Result<WordExplanation> {
        return withContext(Dispatchers.IO) {
            try {
                val model = getModel() ?: return@withContext Result.failure(
                    Exception("API Key not configured")
                )
                
                val contextPart = if (context != null) {
                    "Context: \"$context\"\n\n"
                } else ""
                
                val prompt = """
                    ${contextPart}Explain the English word or phrase "$word" in Chinese.
                    Return a JSON response:
                    {
                        "word": "$word",
                        "phonetic": "<phonetic transcription>",
                        "meaning": "<Chinese meaning>",
                        "example": "<example sentence in English>",
                        "example_cn": "<example sentence translation>"
                    }
                """.trimIndent()
                
                val response = model.generateContent(prompt)
                val responseText = response.text?.trim() ?: ""
                
                val jsonString = responseText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()
                
                val json = JSONObject(jsonString)
                
                Result.success(
                    WordExplanation(
                        word = json.optString("word", word),
                        phonetic = json.optString("phonetic", ""),
                        meaning = json.optString("meaning", ""),
                        example = json.optString("example", ""),
                        exampleCn = json.optString("example_cn", "")
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    fun resetModel() {
        generativeModel = null
    }
}

data class ArticleAnalysis(
    val difficultyLevel: Int,
    val topics: List<String>,
    val summaryEn: String,
    val summaryCn: String,
    val keyVocabulary: List<KeyVocabItem>,
    val questions: List<QuestionItem>
) {
    val topicsJson: String
        get() = JSONArray(topics).toString()
    
    val keyVocabularyJson: String
        get() {
            val array = JSONArray()
            keyVocabulary.forEach { item ->
                val obj = JSONObject()
                obj.put("word", item.word)
                obj.put("meaning", item.meaning)
                array.put(obj)
            }
            return array.toString()
        }
    
    val questionsJson: String
        get() {
            val array = JSONArray()
            questions.forEach { item ->
                val obj = JSONObject()
                obj.put("q", item.question)
                obj.put("a", item.answer)
                array.put(obj)
            }
            return array.toString()
        }
}

data class KeyVocabItem(
    val word: String,
    val meaning: String
)

data class QuestionItem(
    val question: String,
    val answer: String
)

data class WordExplanation(
    val word: String,
    val phonetic: String,
    val meaning: String,
    val example: String,
    val exampleCn: String
)
