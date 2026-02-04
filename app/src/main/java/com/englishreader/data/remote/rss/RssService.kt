package com.englishreader.data.remote.rss

import com.englishreader.data.local.entity.ArticleEntity
import com.englishreader.domain.model.RssSource
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssService @Inject constructor(
    private val rssParser: RssParser
) {
    
    suspend fun fetchArticles(source: RssSource): Result<List<ArticleEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val channel = rssParser.getRssChannel(source.url)
                val articles = parseChannel(channel, source)
                Result.success(articles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun fetchAllArticles(sources: List<RssSource>): List<ArticleEntity> {
        return withContext(Dispatchers.IO) {
            sources.mapNotNull { source ->
                try {
                    val channel = rssParser.getRssChannel(source.url)
                    parseChannel(channel, source)
                } catch (e: Exception) {
                    null
                }
            }.flatten()
        }
    }
    
    private fun parseChannel(channel: RssChannel, source: RssSource): List<ArticleEntity> {
        val now = System.currentTimeMillis()
        
        return channel.items.mapNotNull { item ->
            val link = item.link ?: return@mapNotNull null
            val title = item.title ?: return@mapNotNull null
            
            // Generate stable ID from URL
            val id = generateId(link)
            
            // Parse published date
            val publishedAt = parseDate(item.pubDate) ?: now
            
            // Get description and content
            val description = item.description?.let { cleanHtml(it) }
            val content = item.content ?: description ?: ""
            
            // Calculate word count
            val wordCount = content.split(Regex("\\s+")).size
            
            ArticleEntity(
                id = id,
                sourceId = source.id,
                sourceName = source.name,
                title = title,
                description = description,
                content = content,
                originalUrl = link,
                imageUrl = item.image ?: extractImageFromContent(item.content),
                author = item.author,
                publishedAt = publishedAt,
                fetchedAt = now,
                wordCount = wordCount
            )
        }
    }
    
    private fun generateId(url: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(url.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun parseDate(dateString: String?): Long? {
        if (dateString == null) return null
        
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "dd MMM yyyy HH:mm:ss Z"
        )
        
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                return sdf.parse(dateString)?.time
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }
    
    private fun extractImageFromContent(content: String?): String? {
        if (content == null) return null
        
        val imgRegex = Regex("<img[^>]+src=[\"']([^\"']+)[\"']")
        val match = imgRegex.find(content)
        return match?.groupValues?.getOrNull(1)
    }
}
