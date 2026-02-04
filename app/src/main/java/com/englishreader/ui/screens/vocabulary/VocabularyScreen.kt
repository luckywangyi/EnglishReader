package com.englishreader.ui.screens.vocabulary

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishreader.domain.model.Vocabulary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val vocabulary by viewModel.vocabulary.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val stats by viewModel.stats.collectAsState()
    
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<Vocabulary?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("词汇本") },
                actions = {
                    IconButton(
                        onClick = {
                            val text = viewModel.getExportText()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "English Reader 词汇本")
                            }
                            context.startActivity(Intent.createChooser(intent, "导出词汇本"))
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
            // Stats card
            StatsCard(stats = stats)
            
            // Search and filter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索词汇...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter == VocabularyFilter.ALL,
                        onClick = { viewModel.setFilter(VocabularyFilter.ALL) },
                        label = { Text("全部 (${stats.total})") }
                    )
                    FilterChip(
                        selected = filter == VocabularyFilter.UNMASTERED,
                        onClick = { viewModel.setFilter(VocabularyFilter.UNMASTERED) },
                        label = { Text("学习中 (${stats.unmastered})") }
                    )
                    FilterChip(
                        selected = filter == VocabularyFilter.MASTERED,
                        onClick = { viewModel.setFilter(VocabularyFilter.MASTERED) },
                        label = { Text("已掌握 (${stats.mastered})") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vocabulary list
            if (vocabulary.isEmpty()) {
                EmptyVocabularyState(filter)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vocabulary, key = { it.id }) { vocab ->
                        VocabularyCard(
                            vocabulary = vocab,
                            onToggleMastered = { viewModel.toggleMastered(vocab) },
                            onDelete = { showDeleteDialog = vocab }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { vocab ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除词汇") },
            text = { Text("确定要删除 \"${vocab.word}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVocabulary(vocab)
                        showDeleteDialog = null
                    }
                ) {
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
private fun StatsCard(stats: VocabularyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "学习进度",
                        style = MaterialTheme.typography.titleMedium
                    )
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
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
                onClick = { /* Could show detail */ },
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Word
                Text(
                    text = vocabulary.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Phonetic
                vocabulary.phonetic?.let { phonetic ->
                    Text(
                        text = phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Meaning
                Text(
                    text = vocabulary.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Context
                vocabulary.context?.let { context ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$context\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Source article
                vocabulary.articleTitle?.let { title ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "来自: $title",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Mastered toggle
            IconButton(
                onClick = onToggleMastered,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (vocabulary.isMastered)
                        Icons.Filled.CheckCircle
                    else
                        Icons.Outlined.Circle,
                    contentDescription = if (vocabulary.isMastered) "已掌握" else "标记为已掌握",
                    tint = if (vocabulary.isMastered)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun EmptyVocabularyState(filter: VocabularyFilter) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (filter) {
                    VocabularyFilter.ALL -> "词汇本为空"
                    VocabularyFilter.UNMASTERED -> "没有学习中的词汇"
                    VocabularyFilter.MASTERED -> "还没有已掌握的词汇"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在阅读时长按单词可以添加到词汇本",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
