package com.englishreader.data.remote.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import java.util.Locale
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
                    .userAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .followRedirects(true)
                    .timeout(15000)
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
            
            val paragraphs = doc.select("p")
                .map { it.text().trim() }
                .filter { it.length > 20 }
            
            val text = if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n")
            } else {
                doc.body()?.text()?.trim() ?: ""
            }
            
            stripReadMore(text)
        } catch (e: Exception) {
            Jsoup.parse(html).text().trim()
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
        doc.select("script, style, nav, header, footer, aside, .ad, .ads, .advertisement, .social-share, .share-buttons, .related-posts, .comments, .sidebar, noscript, iframe").remove()

        val selectors = listOf(
            "[itemprop=articleBody]",
            "article .article-content",
            "article .article-body",
            "article .post-content",
            "article .entry-content",
            "article .story-body",
            ".article-content",
            ".article-body",
            ".article__body",
            ".post-content",
            ".post-body",
            ".entry-content",
            ".story-body",
            ".story-content",
            ".content-body",
            ".blog-post-content",
            ".article-text",
            "article",
            "[role=main]",
            "main",
            "#article-body",
            "#story-body",
            "#content"
        )
        
        for (selector in selectors) {
            val element = doc.selectFirst(selector) ?: continue
            element.select(".ad, .share, .related, .newsletter, .promo, .subscription, figure figcaption").remove()
            
            val paragraphs = element.select("p")
                .map { it.text().trim() }
                .filter { it.length > 30 }
            
            if (paragraphs.size >= 2) {
                return stripReadMore(paragraphs.joinToString("\n\n"))
            }
            
            val text = element.text().trim()
            if (text.length > 200) return stripReadMore(text)
        }
        
        val paragraphs = doc.select("p")
            .map { it.text().trim() }
            .filter { it.length > 40 }
        
        return if (paragraphs.isNotEmpty()) {
            stripReadMore(paragraphs.joinToString("\n\n"))
        } else {
            stripReadMore(doc.body()?.text()?.trim() ?: "")
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
    
    private fun stripReadMore(text: String): String {
        if (text.isBlank()) return text
        
        val blocks = text.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        val filtered = blocks.filterNot { block ->
            val lower = block.lowercase(Locale.getDefault())
            (lower.contains("continue reading") ||
                lower.contains("read more") ||
                lower.contains("continue on medium") ||
                lower.contains("continue reading on medium") ||
                lower.contains("keep reading") ||
                lower.contains("read the full") ||
                lower.contains("full story") ||
                (lower.contains("medium") && block.length < 120))
        }
        
        return filtered.joinToString("\n\n").trim()
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
