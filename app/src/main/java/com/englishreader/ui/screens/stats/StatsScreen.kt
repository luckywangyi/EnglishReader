package com.englishreader.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val overallStats by viewModel.overallStats.collectAsState()
    val weeklyData by viewModel.weeklyData.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习统计") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall stats cards
            item {
                Text(
                    text = "总览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MenuBook,
                        title = "已读文章",
                        value = overallStats.totalArticlesRead.toString(),
                        subtitle = "篇"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoStories,
                        title = "阅读单词",
                        value = formatNumber(overallStats.totalWordsRead),
                        subtitle = "词"
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Spellcheck,
                        title = "收藏词汇",
                        value = overallStats.totalVocabulary.toString(),
                        subtitle = "已掌握 ${overallStats.masteredVocabulary}"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        title = "连续阅读",
                        value = overallStats.currentStreak.toString(),
                        subtitle = "天",
                        isHighlighted = overallStats.currentStreak > 0
                    )
                }
            }
            
            // Weekly chart
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "本周阅读",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                WeeklyChart(weeklyData = weeklyData)
            }
            
            // Tips
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "学习建议",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = buildLearningTip(overallStats),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlighted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WeeklyChart(weeklyData: List<DailyStats>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val maxArticles = weeklyData.maxOfOrNull { it.articlesRead } ?: 1
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { day ->
                    DayBar(
                        dayName = day.dayName,
                        articlesRead = day.articlesRead,
                        maxArticles = maxArticles.coerceAtLeast(1)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBar(
    dayName: String,
    articlesRead: Int,
    maxArticles: Int
) {
    val barHeight = if (maxArticles > 0) {
        (articlesRead.toFloat() / maxArticles * 80).dp
    } else 0.dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.height(120.dp)
    ) {
        if (articlesRead > 0) {
            Text(
                text = articlesRead.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(barHeight.coerceAtLeast(4.dp))
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(
                    if (articlesRead > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNumber(number: Int): String {
    return when {
        number >= 10000 -> String.format("%.1fw", number / 10000f)
        number >= 1000 -> String.format("%.1fk", number / 1000f)
        else -> number.toString()
    }
}

private fun buildLearningTip(stats: OverallStatsUi): String {
    return when {
        stats.currentStreak == 0 -> "今天还没有阅读哦，开始阅读一篇文章吧！"
        stats.currentStreak < 7 -> "继续保持！坚持阅读 7 天可以形成良好的学习习惯。"
        stats.currentStreak < 30 -> "太棒了！你已经连续阅读 ${stats.currentStreak} 天，继续加油！"
        else -> "你真是学习达人！连续阅读 ${stats.currentStreak} 天，英语水平一定提升很多！"
    }
}
