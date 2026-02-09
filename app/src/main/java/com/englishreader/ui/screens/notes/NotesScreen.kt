package com.englishreader.ui.screens.notes

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishreader.domain.model.Sentence
import com.englishreader.domain.model.Vocabulary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onNavigateToFlashcard: () -> Unit = {},
    viewModel: NotesViewModel = hiltViewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val dueReviewCount by viewModel.dueReviewCount.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("笔记本") },
                actions = {
                    // 开始复习按钮
                    IconButton(
                        onClick = onNavigateToFlashcard
                    ) {
                        androidx.compose.foundation.layout.Box {
                            Icon(Icons.Default.PlayArrow, contentDescription = "开始复习")
                            if (dueReviewCount > 0) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (dueReviewCount > 99) "99+" else dueReviewCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            val text = viewModel.getExportText()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "English Reader 笔记")
                            }
                            context.startActivity(Intent.createChooser(intent, "导出笔记"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = currentTab.ordinal
            ) {
                Tab(
                    selected = currentTab == NotesTab.VOCABULARY,
                    onClick = { viewModel.setCurrentTab(NotesTab.VOCABULARY) },
                    text = { Text("生词本") }
                )
                Tab(
                    selected = currentTab == NotesTab.SENTENCES,
                    onClick = { viewModel.setCurrentTab(NotesTab.SENTENCES) },
                    text = { Text("句子摘抄") }
                )
            }
            
            // Content based on tab
            when (currentTab) {
                NotesTab.VOCABULARY -> VocabularyTabContent(viewModel)
                NotesTab.SENTENCES -> SentenceTabContent(viewModel)
            }
        }
    }
}

@Composable
private fun VocabularyTabContent(viewModel: NotesViewModel) {
    val vocabulary by viewModel.vocabulary.collectAsState()
    val filter by viewModel.vocabularyFilter.collectAsState()
    val searchQuery by viewModel.vocabularySearchQuery.collectAsState()
    val stats by viewModel.vocabularyStats.collectAsState()
    val viewMode by viewModel.vocabularyViewMode.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<Vocabulary?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats card
        VocabularyStatsCard(stats = stats)
        
        // Search and filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setVocabularySearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索词汇...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == VocabularyFilter.ALL,
                        onClick = { viewModel.setVocabularyFilter(VocabularyFilter.ALL) },
                        label = { Text("全部 (${stats.total})") }
                    )
                    FilterChip(
                        selected = filter == VocabularyFilter.UNMASTERED,
                        onClick = { viewModel.setVocabularyFilter(VocabularyFilter.UNMASTERED) },
                        label = { Text("学习中 (${stats.unmastered})") }
                    )
                    FilterChip(
                        selected = filter == VocabularyFilter.MASTERED,
                        onClick = { viewModel.setVocabularyFilter(VocabularyFilter.MASTERED) },
                        label = { Text("已掌握 (${stats.mastered})") }
                    )
                }
                
                // 视图切换按钮
                IconButton(
                    onClick = {
                        viewModel.setVocabularyViewMode(
                            if (viewMode == VocabularyViewMode.FLAT) 
                                VocabularyViewMode.GROUPED_BY_ARTICLE 
                            else 
                                VocabularyViewMode.FLAT
                        )
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (viewMode == VocabularyViewMode.FLAT)
                            Icons.Default.Folder
                        else
                            Icons.Default.ViewList,
                        contentDescription = if (viewMode == VocabularyViewMode.FLAT) "按文章分组" else "列表视图",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (vocabulary.isEmpty()) {
            EmptyVocabularyState()
        } else if (viewMode == VocabularyViewMode.GROUPED_BY_ARTICLE) {
            // 按文章分组视图
            GroupedVocabularyList(
                vocabulary = vocabulary,
                onToggleMastered = { viewModel.toggleVocabularyMastered(it) },
                onDelete = { showDeleteDialog = it }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vocabulary, key = { it.id }) { vocab ->
                    VocabularyCard(
                        vocabulary = vocab,
                        onToggleMastered = { viewModel.toggleVocabularyMastered(vocab) },
                        onDelete = { showDeleteDialog = vocab }
                    )
                }
            }
        }
    }
    
    showDeleteDialog?.let { vocab ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除词汇") },
            text = { Text("确定要删除 \"${vocab.word}\" 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVocabulary(vocab)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SentenceTabContent(viewModel: NotesViewModel) {
    val sentences by viewModel.sentences.collectAsState()
    val filter by viewModel.sentenceFilter.collectAsState()
    val searchQuery by viewModel.sentenceSearchQuery.collectAsState()
    val stats by viewModel.sentenceStats.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<Sentence?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats card
        SentenceStatsCard(stats = stats)
        
        // Search and filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSentenceSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索句子...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == SentenceFilter.ALL,
                    onClick = { viewModel.setSentenceFilter(SentenceFilter.ALL) },
                    label = { Text("全部 (${stats.total})") }
                )
                FilterChip(
                    selected = filter == SentenceFilter.FAVORITES,
                    onClick = { viewModel.setSentenceFilter(SentenceFilter.FAVORITES) },
                    label = { Text("收藏 (${stats.favorites})") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (sentences.isEmpty()) {
            EmptySentenceState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sentences, key = { it.id }) { sentence ->
                    SentenceCard(
                        sentence = sentence,
                        onToggleFavorite = { viewModel.toggleSentenceFavorite(sentence) },
                        onDelete = { showDeleteDialog = sentence }
                    )
                }
            }
        }
    }
    
    showDeleteDialog?.let { sentence ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除句子") },
            text = { Text("确定要删除这个句子吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSentence(sentence)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun VocabularyStatsCard(stats: VocabularyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "学习进度", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "已掌握 ${stats.mastered}/${stats.total} 个词汇",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(stats.masteredPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { stats.masteredPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun SentenceStatsCard(stats: SentenceStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stats.total.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "摘抄句子",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stats.favorites.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "收藏句子",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VocabularyCard(
    vocabulary: Vocabulary,
    onToggleMastered: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onDelete
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (vocabulary.isMastered)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vocabulary.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                vocabulary.phonetic?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vocabulary.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                vocabulary.context?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$it\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                vocabulary.articleTitle?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "来自: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onToggleMastered, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (vocabulary.isMastered) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (vocabulary.isMastered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SentenceCard(
    sentence: Sentence,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onDelete
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // English sentence
                Text(
                    text = sentence.content,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                // Translation
                sentence.translation?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Note
                sentence.note?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "笔记: $it",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                // Source article
                sentence.articleTitle?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "来自: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (sentence.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (sentence.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun GroupedVocabularyList(
    vocabulary: List<Vocabulary>,
    onToggleMastered: (Vocabulary) -> Unit,
    onDelete: (Vocabulary) -> Unit
) {
    // 按文章分组
    val grouped = vocabulary.groupBy { it.articleTitle ?: "未分类" }
    val expandedGroups = remember { mutableStateOf(setOf<String>()) }
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (articleTitle, words) ->
            val isExpanded = expandedGroups.value.contains(articleTitle)
            
            item(key = "group_$articleTitle") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedGroups.value = if (isExpanded) {
                                expandedGroups.value - articleTitle
                            } else {
                                expandedGroups.value + articleTitle
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = articleTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${words.size} 个词汇",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (isExpanded) {
                items(words, key = { it.id }) { vocab ->
                    VocabularyCard(
                        vocabulary = vocab,
                        onToggleMastered = { onToggleMastered(vocab) },
                        onDelete = { onDelete(vocab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVocabularyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "生词本为空",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在阅读时选择单词可以添加到生词本",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySentenceState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "句子摘抄为空",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在阅读时选择句子可以添加到摘抄",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
