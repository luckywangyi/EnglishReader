package com.englishreader.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.englishreader.domain.model.Article
import com.englishreader.ui.components.DifficultyBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val translationState by viewModel.translationState.collectAsState()
    val showSummary by viewModel.showSummary.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    
    // Calculate reading progress
    val readProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else 0f
        }
    }
    
    // Update progress when scrolling
    LaunchedEffect(readProgress) {
        if (readProgress > 0.1f) {
            viewModel.updateReadProgress(readProgress)
        }
    }
    
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    
    // Show bottom sheet when translation is available
    LaunchedEffect(translationState) {
        when (translationState) {
            is TranslationState.Loading,
            is TranslationState.Success -> {
                scope.launch { bottomSheetState.expand() }
            }
            is TranslationState.Saved -> {
                scope.launch { 
                    kotlinx.coroutines.delay(1000)
                    bottomSheetState.hide()
                    viewModel.clearTranslation()
                }
            }
            else -> {}
        }
    }
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            TranslationSheet(
                state = translationState,
                onSave = { original, translation ->
                    viewModel.saveVocabulary(original, translation)
                },
                onDismiss = {
                    scope.launch { bottomSheetState.hide() }
                    viewModel.clearTranslation()
                }
            )
        },
        sheetPeekHeight = 0.dp,
        topBar = {
            Column {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        // Font size controls
                        IconButton(onClick = { viewModel.decreaseFontSize() }) {
                            Text("A-", fontSize = 16.sp)
                        }
                        IconButton(onClick = { viewModel.increaseFontSize() }) {
                            Text("A+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Analyze button
                        IconButton(
                            onClick = { viewModel.analyzeArticle() },
                            enabled = !isAnalyzing && article?.isAnalyzed != true
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Analytics, "Analyze")
                            }
                        }
                        
                        // Favorite button
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (article?.isFavorite == true)
                                    Icons.Filled.Favorite
                                else
                                    Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (article?.isFavorite == true)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Open in browser
                        article?.originalUrl?.let { url ->
                            IconButton(onClick = { uriHandler.openUri(url) }) {
                                Icon(Icons.Default.OpenInBrowser, "Open in browser")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // Reading progress indicator
                LinearProgressIndicator(
                    progress = { readProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (article == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        ArticleHeader(
                            article = article!!,
                            showSummary = showSummary,
                            onToggleSummary = { viewModel.toggleSummary() }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Article content with selectable text
                        ArticleContent(
                            content = article!!.content,
                            fontSize = fontSize,
                            onTextSelected = { text ->
                                viewModel.translate(text)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleHeader(
    article: Article,
    showSummary: Boolean,
    onToggleSummary: () -> Unit
) {
    Column {
        // Image
        article.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Title
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Meta info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.sourceName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                article.difficultyLevel?.let { level ->
                    DifficultyBadge(level = level)
                }
            }
            
            Text(
                text = "${article.wordCount} 词",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Author and date
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            article.author?.let { author ->
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " · ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatDate(article.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Summary section (if analyzed)
        if (article.isAnalyzed && (article.summaryCn != null || article.summaryEn != null)) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                onClick = onToggleSummary,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI 摘要",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (showSummary) "收起" else "展开",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (showSummary) {
                        Spacer(modifier = Modifier.height(8.dp))
                        article.summaryCn?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleContent(
    content: String,
    fontSize: Int,
    onTextSelected: (String) -> Unit
) {
    // Split content into paragraphs
    val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        paragraphs.forEach { paragraph ->
            Text(
                text = paragraph.trim(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.6).sp,
                    fontFamily = FontFamily.Serif
                ),
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Composable
private fun TranslationSheet(
    state: TranslationState,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "翻译",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        when (state) {
            is TranslationState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is TranslationState.Success -> {
                Column {
                    // Original text
                    Text(
                        text = state.original,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Phonetic (if available)
                    state.phonetic?.let { phonetic ->
                        Text(
                            text = phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Translation
                    Text(
                        text = state.translation,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Source indicator
                    Text(
                        text = if (state.isFromDict) "来源：本地词典" else "来源：AI 翻译",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Save button
                    TextButton(
                        onClick = { onSave(state.original, state.translation) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("加入词汇本")
                    }
                }
            }
            
            is TranslationState.Saved -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "已保存到词汇本",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            is TranslationState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
