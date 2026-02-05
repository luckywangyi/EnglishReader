package com.englishreader.data.remote.ai

import com.englishreader.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Service using DashScope API (通义千问)
 * 
 * 免费额度：
 * - qwen-turbo: 100万 tokens/月 免费
 * - qwen-plus: 100万 tokens 免费（一次性）
 * 
 * 获取 API Key: https://dashscope.console.aliyun.com/apiKey
 */
@Singleton
class AiService @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private val model = "qwen-turbo" // 免费模型
    
    private suspend fun getApiKey(): String {
        return settingsRepository.geminiApiKey.first()
    }
    
    suspend fun translate(text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key 未配置"))
                }
                
                val prompt = """
                    将以下英文翻译成中文，只返回翻译结果，不要解释：
                    
                    $text
                """.trimIndent()
                
                val response = callApi(apiKey, prompt)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun analyzeArticle(content: String, title: String): Result<ArticleAnalysis> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key 未配置"))
                }
                
                val truncatedContent = if (content.length > 4000) {
                    content.take(4000) + "..."
                } else content
                
                val prompt = """
                    分析以下英文文章，返回 JSON 格式：
                    {
                        "difficulty_level": <1-4的整数>,
                        "topics": ["主题1", "主题2"],
                        "summary_en": "<50-100词的英文摘要>",
                        "summary_cn": "<100-150字的中文摘要>",
                        "key_vocabulary": [
                            {"word": "重要单词", "meaning": "中文含义"}
                        ],
                        "questions": [
                            {"q": "理解问题", "a": "简短答案"}
                        ]
                    }
                    
                    难度级别：1=简单 2=中等 3=困难 4=高级
                    提供5-8个关键词汇和2-3个理解问题。
                    
                    标题: $title
                    
                    内容:
                    $truncatedContent
                """.trimIndent()
                
                val response = callApi(apiKey, prompt)
                val analysis = parseAnalysisResponse(response)
                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun explainWord(word: String, context: String? = null): Result<WordExplanation> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key 未配置"))
                }
                
                val contextPart = if (context != null) "上下文: \"$context\"\n\n" else ""
                
                val prompt = """
                    ${contextPart}解释英文单词或短语 "$word"。
                    返回 JSON 格式：
                    {
                        "word": "$word",
                        "phonetic": "<音标>",
                        "meaning": "<中文含义>",
                        "example": "<英文例句>",
                        "example_cn": "<例句翻译>"
                    }
                """.trimIndent()
                
                val response = callApi(apiKey, prompt)
                val jsonString = response
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
    
    suspend fun testConnection(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key 未配置"))
                }
                
                val response = callApi(apiKey, "你好，请回复\"连接成功\"")
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 使用指定的 API Key 测试连接（不保存）
     */
    suspend fun testConnectionWithKey(apiKey: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key 未配置"))
                }
                
                val response = callApi(apiKey, "你好，请回复\"连接成功\"")
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun callApi(apiKey: String, prompt: String): String {
        val requestBody = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 2048)
        }
        
        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        
        if (!response.isSuccessful) {
            val errorJson = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                null
            }
            val errorMessage = errorJson?.optJSONObject("error")?.optString("message")
                ?: "请求失败: ${response.code}"
            throw Exception(errorMessage)
        }
        
        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw Exception("API 返回为空")
        }
        
        val message = choices.getJSONObject(0).optJSONObject("message")
        return message?.optString("content", "") ?: ""
    }
    
    private fun parseAnalysisResponse(response: String): ArticleAnalysis {
        try {
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
    
    fun resetModel() {
        // No cached model to reset for HTTP API
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
