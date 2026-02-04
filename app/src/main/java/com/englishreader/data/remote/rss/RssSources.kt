package com.englishreader.data.remote.rss

import com.englishreader.domain.model.Category
import com.englishreader.domain.model.RssSource

object RssSources {
    val sources = listOf(
        // News Sources
        RssSource(
            id = "bbc_news",
            name = "BBC News",
            url = "https://feeds.bbci.co.uk/news/rss.xml",
            category = Category.NEWS
        ),
        RssSource(
            id = "bbc_world",
            name = "BBC World",
            url = "https://feeds.bbci.co.uk/news/world/rss.xml",
            category = Category.NEWS
        ),
        RssSource(
            id = "guardian_world",
            name = "The Guardian",
            url = "https://www.theguardian.com/world/rss",
            category = Category.NEWS
        ),
        RssSource(
            id = "npr",
            name = "NPR News",
            url = "https://feeds.npr.org/1001/rss.xml",
            category = Category.NEWS
        ),
        RssSource(
            id = "reuters_world",
            name = "Reuters",
            url = "https://www.reutersagency.com/feed/?best-topics=world",
            category = Category.NEWS
        ),
        
        // Tech Sources
        RssSource(
            id = "wired",
            name = "Wired",
            url = "https://www.wired.com/feed/rss",
            category = Category.TECH
        ),
        RssSource(
            id = "ars_technica",
            name = "Ars Technica",
            url = "https://feeds.arstechnica.com/arstechnica/index",
            category = Category.TECH
        ),
        RssSource(
            id = "the_verge",
            name = "The Verge",
            url = "https://www.theverge.com/rss/index.xml",
            category = Category.TECH
        ),
        RssSource(
            id = "techcrunch",
            name = "TechCrunch",
            url = "https://techcrunch.com/feed/",
            category = Category.TECH
        ),
        
        // Science Sources
        RssSource(
            id = "science_daily",
            name = "Science Daily",
            url = "https://www.sciencedaily.com/rss/all.xml",
            category = Category.SCIENCE
        ),
        RssSource(
            id = "phys_org",
            name = "Phys.org",
            url = "https://phys.org/rss-feed/",
            category = Category.SCIENCE
        ),
        RssSource(
            id = "nat_geo",
            name = "National Geographic",
            url = "https://www.nationalgeographic.com/feed",
            category = Category.SCIENCE
        ),
        
        // Business Sources
        RssSource(
            id = "bbc_business",
            name = "BBC Business",
            url = "https://feeds.bbci.co.uk/news/business/rss.xml",
            category = Category.BUSINESS
        ),
        RssSource(
            id = "forbes",
            name = "Forbes",
            url = "https://www.forbes.com/innovation/feed/",
            category = Category.BUSINESS
        ),
        
        // Speech/TED
        RssSource(
            id = "ted_talks",
            name = "TED Talks",
            url = "https://feeds.feedburner.com/TedtalksHD",
            category = Category.SPEECH
        ),
        
        // Culture/Education
        RssSource(
            id = "aeon",
            name = "Aeon Essays",
            url = "https://aeon.co/feed.rss",
            category = Category.CULTURE
        ),
        RssSource(
            id = "medium_tech",
            name = "Medium - Technology",
            url = "https://medium.com/feed/tag/technology",
            category = Category.TECH
        )
    )
    
    fun getSourceById(id: String): RssSource? {
        return sources.find { it.id == id }
    }
    
    fun getSourcesByCategory(category: Category): List<RssSource> {
        return sources.filter { it.category == category }
    }
    
    fun getEnabledSources(): List<RssSource> {
        return sources.filter { it.isEnabled }
    }
}
