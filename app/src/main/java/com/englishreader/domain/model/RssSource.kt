package com.englishreader.domain.model

import androidx.annotation.DrawableRes

data class RssSource(
    val id: String,
    val name: String,
    val url: String,
    val category: Category,
    @DrawableRes val icon: Int? = null,
    val iconUrl: String? = null,  // 用于自定义 RSS 源的图标 URL
    val isEnabled: Boolean = true
)

enum class Category(val displayName: String, val displayNameCn: String) {
    NEWS("News", "新闻"),
    TECH("Technology", "科技"),
    BUSINESS("Business", "商业"),
    SCIENCE("Science", "科学"),
    SPEECH("Speech/TED", "演讲"),
    CULTURE("Culture", "文化"),
    EDUCATION("Education", "教育"),
    CUSTOM("Custom", "自定义")
}
