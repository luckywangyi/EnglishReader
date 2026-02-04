package com.englishreader.data.remote.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlParser @Inject constructor() {
    
    /**
     * Fetches and extracts the main article content from a URL
     */
    suspend fun fetchArticleContent(url: String): Result<ArticleContent> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Android; Mobile)")
                    .timeout(30000)
                    .get()
                
                val content = extractMainContent(doc)
                val title = extractTitle(doc)
                val imageUrl = extractMainImage(doc)
                val author = extractAuthor(doc)
                
                Result.success(
                    ArticleContent(
                        title = title,
                        content = content,
                        imageUrl = imageUrl,
                        author = author
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cleans HTML content and extracts readable text
     */
    fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        
        return try {
            // Parse HTML
            val doc = Jsoup.parse(html)
            
            // Remove unwanted elements
            doc.select("script, style, nav, header, footer, aside, .ad, .advertisement, .social-share").remove()
            
            // Get text content
            val text = doc.body()?.text() ?: ""
            
            // Clean up whitespace
            text.replace(Regex("\\s+"), " ").trim()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "").trim()
        }
    }
    
    /**
     * Extracts paragraphs from HTML content
     */
    fun extractParagraphs(html: String): List<String> {
        if (html.isBlank()) return emptyList()
        
        return try {
            val doc = Jsoup.parse(html)
            
            // Remove unwanted elements
            doc.select("script, style, nav, header, footer, aside").remove()
            
            // Extract paragraphs
            doc.select("p").mapNotNull { element ->
                val text = element.text().trim()
                if (text.length > 20) text else null  // Filter out short fragments
            }
        } catch (e: Exception) {
            listOf(cleanHtml(html))
        }
    }
    
    private fun extractMainContent(doc: Document): String {
        // Try common article selectors
        val selectors = listOf(
            "article",
            "[role=main]",
            ".article-content",
            ".article-body",
            ".post-content",
            ".entry-content",
            ".story-body",
            ".content-body",
            "main",
            "#content"
        )
        
        for (selector in selectors) {
            val element = doc.selectFirst(selector)
            if (element != null) {
                // Remove unwanted nested elements
                element.select("script, style, nav, .ad, .share, .related").remove()
                
                // Extract paragraphs
                val paragraphs = element.select("p")
                if (paragraphs.isNotEmpty()) {
                    return paragraphs.joinToString("\n\n") { it.text().trim() }
                }
                
                // Fallback to element text
                val text = element.text().trim()
                if (text.length > 200) return text
            }
        }
        
        // Fallback: get all paragraphs
        val paragraphs = doc.select("p")
            .map { it.text().trim() }
            .filter { it.length > 50 }
        
        return if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            doc.body()?.text()?.trim() ?: ""
        }
    }
    
    private fun extractTitle(doc: Document): String {
        // Try meta tags first
        doc.selectFirst("meta[property=og:title]")?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        // Try common title selectors
        val selectors = listOf(
            "h1.article-title",
            "h1.headline",
            "h1.post-title",
            "article h1",
            "h1"
        )
        
        for (selector in selectors) {
            doc.selectFirst(selector)?.text()?.trim()?.let {
                if (it.isNotBlank()) return it
            }
        }
        
        // Fallback to title tag
        return doc.title()
    }
    
    private fun extractMainImage(doc: Document): String? {
        // Try Open Graph image
        doc.selectFirst("meta[property=og:image]")?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        // Try Twitter card image
        doc.selectFirst("meta[name=twitter:image]")?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        // Try article image
        doc.selectFirst("article img, .article-image img, .featured-image img")?.attr("src")?.let {
            if (it.isNotBlank()) return it
        }
        
        return null
    }
    
    private fun extractAuthor(doc: Document): String? {
        // Try meta tags
        doc.selectFirst("meta[name=author]")?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        // Try common author selectors
        val selectors = listOf(
            ".author-name",
            ".byline",
            "[rel=author]",
            ".article-author"
        )
        
        for (selector in selectors) {
            doc.selectFirst(selector)?.text()?.trim()?.let {
                if (it.isNotBlank()) return it
            }
        }
        
        return null
    }
    
    /**
     * Sanitizes HTML for safe display
     */
    fun sanitizeHtml(html: String): String {
        return Jsoup.clean(html, Safelist.basic())
    }
}

data class ArticleContent(
    val title: String,
    val content: String,
    val imageUrl: String?,
    val author: String?
) {
    val wordCount: Int get() = content.split(Regex("\\s+")).size
}
