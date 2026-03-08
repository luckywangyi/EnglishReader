package com.englishreader.ui.screens.reader

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.englishreader.R
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.englishreader.domain.model.Article
import com.englishreader.ui.components.DifficultyBadge
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
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
    val isFetchingFullContent by viewModel.isFetchingFullContent.collectAsState()
    val fullContentFetchResult by viewModel.fullContentFetchResult.collectAsState()
    
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    
    // 离开页面时保存阅读时长
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveReadingTime()
        }
    }
    
    // 预先拆分段落并缓存，避免每次重组都拆分
    val paragraphs = remember(article?.content) {
        article?.content?.split("\n\n")?.filter { it.isNotBlank() }?.map { it.trim() } ?: emptyList()
    }
    
    // 使用 LazyListState 计算阅读进度
    val readProgress by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) 0f
            else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                (lastVisibleItem + 1).toFloat() / totalItems.toFloat()
            }
        }
    }
    
    // 节流：滚动进度写入数据库 - 防抖 1 秒，只在停止滚动后更新
    LaunchedEffect(Unit) {
        snapshotFlow { readProgress }
            .distinctUntilChanged()
            .debounce(1000L)
            .collect { progress ->
                if (progress > 0.1f) {
                    viewModel.updateReadProgress(progress)
                }
            }
    }
    
    LaunchedEffect(fullContentFetchResult) {
        if (fullContentFetchResult == com.englishreader.data.repository.FullContentFetchResult.UPDATED) {
            viewModel.clearFullContentFetchResult()
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
            is TranslationState.SavedWord,
            is TranslationState.SavedSentence -> {
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
                onSaveWord = { original, translation ->
                    viewModel.saveVocabulary(original, translation)
                },
                onSaveSentence = { original, translation ->
                    viewModel.saveSentence(original, translation)
                },
                onSpeak = { text ->
                    viewModel.speak(text)
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                        
                        // Favorite button
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (article?.isFavorite == true)
                                    Icons.Filled.Favorite
                                else
                                    Icons.Outlined.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (article?.isFavorite == true)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Open in browser
                        article?.originalUrl?.let { url ->
                            IconButton(onClick = { uriHandler.openUri(url) }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "在浏览器中打开")
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
                
                if (isFetchingFullContent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // 文章头部
                    item(key = "header") {
                        Spacer(modifier = Modifier.height(16.dp))
                        ArticleHeader(
                            article = article!!,
                            showSummary = showSummary,
                            onToggleSummary = { viewModel.toggleSummary() }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 提示文字
                        Text(
                            text = "点击单词翻译 · 长按句子翻译",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // 文章段落 - LazyColumn 只渲染可见段落
                    items(
                        items = paragraphs,
                        key = { paragraph -> paragraph.hashCode() }
                    ) { paragraph ->
                        ClickableParagraph(
                            text = paragraph,
                            fontSize = fontSize,
                            onWordClick = { word ->
                                viewModel.translate(word)
                            },
                            onSentenceClick = { sentence ->
                                viewModel.translate(sentence)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // 底部空白
                    item(key = "footer") {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground),
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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

/**
 * 可点击的段落 - 使用 remember 缓存 AnnotatedString
 */
@Composable
private fun ClickableParagraph(
    text: String,
    fontSize: Int,
    onWordClick: (String) -> Unit,
    onSentenceClick: (String) -> Unit = {}
) {
    // 缓存 AnnotatedString 构建结果 - 只在 text 变化时重新构建
    val annotatedString = remember(text) {
        buildParagraphAnnotatedString(text)
    }
    
    // 使用非 Compose 状态的引用来存储 TextLayoutResult
    // 避免 onTextLayout 触发不必要的重组
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.6).sp,
            fontFamily = FontFamily.Serif
        ),
        textAlign = TextAlign.Justify,
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.pointerInput(annotatedString) {
            detectTapGestures(
                onTap = { offset ->
                    // 点击 → 翻译单词
                    textLayoutResult?.let { layoutResult ->
                        val position = layoutResult.getOffsetForPosition(offset)
                        annotatedString.getStringAnnotations(
                            tag = "WORD",
                            start = position,
                            end = position
                        ).firstOrNull()?.let { annotation ->
                            onWordClick(annotation.item)
                        }
                    }
                },
                onLongPress = { offset ->
                    // 长按 → 翻译句子
                    textLayoutResult?.let { layoutResult ->
                        val position = layoutResult.getOffsetForPosition(offset)
                        annotatedString.getStringAnnotations(
                            tag = "SENTENCE",
                            start = position,
                            end = position
                        ).firstOrNull()?.let { annotation ->
                            onSentenceClick(annotation.item)
                        }
                    }
                }
            )
        }
    )
}

/**
 * 纯函数：构建段落的 AnnotatedString（可缓存）
 */
private fun buildParagraphAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        // 将段落分割为句子
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        var globalIndex = 0
        
        sentences.forEachIndexed { sentenceIndex, sentence ->
            // 为整个句子添加注解
            pushStringAnnotation(tag = "SENTENCE", annotation = sentence.trim())
            
            // 分割句子中的单词
            val tokens = sentence.split(Regex("(?<=\\s)|(?=\\s)|(?<=[.,!?;:\"'()\\[\\]{}])|(?=[.,!?;:\"'()\\[\\]{}])"))
            
            tokens.forEach { token ->
                val cleanWord = token.trim()
                if (cleanWord.isNotEmpty() && cleanWord.matches(Regex("[a-zA-Z]+"))) {
                    pushStringAnnotation(tag = "WORD", annotation = cleanWord)
                    append(token)
                    pop()
                } else {
                    append(token)
                }
                globalIndex += token.length
            }
            
            pop() // 结束句子注解
            
            // 句子之间添加空格
            if (sentenceIndex < sentences.size - 1) {
                append(" ")
                globalIndex += 1
            }
        }
    }
}

@Composable
private fun TranslationSheet(
    state: TranslationState,
    onSaveWord: (String, String) -> Unit,
    onSaveSentence: (String, String?) -> Unit,
    onSpeak: (String) -> Unit,
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
                Icon(Icons.Default.Close, contentDescription = "关闭")
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
                val isMultipleWords = state.original.trim().split(Regex("\\s+")).size > 1
                
                Column {
                    // Original text with speak button
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.original,
                            style = if (isMultipleWords) 
                                MaterialTheme.typography.bodyLarge 
                            else 
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 发音按钮
                        IconButton(
                            onClick = { onSpeak(state.original) }
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "朗读",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Phonetic (if available, only for single words)
                    if (!isMultipleWords) {
                        state.phonetic?.let { phonetic ->
                            Text(
                                text = phonetic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    
                    // Save buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isMultipleWords) {
                            // Single word - save to vocabulary
                            TextButton(
                                onClick = { onSaveWord(state.original, state.translation) }
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("加入生词本")
                            }
                        }
                        
                        // Sentence or phrase - save to sentences
                        TextButton(
                            onClick = { onSaveSentence(state.original, state.translation) }
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isMultipleWords) "保存句子" else "保存为句子")
                        }
                    }
                }
            }
            
            is TranslationState.SavedWord -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "已保存到生词本",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            is TranslationState.SavedSentence -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "已保存到句子摘抄",
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
