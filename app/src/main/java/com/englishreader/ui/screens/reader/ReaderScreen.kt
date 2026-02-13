package com.englishreader.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.englishreader.R
import android.widget.Toast
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.englishreader.domain.model.Article
import com.englishreader.domain.model.ComprehensionQuestion
import com.englishreader.ui.components.DifficultyBadge
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// 预编译正则，避免每次重组都创建新实例
private val SENTENCE_SPLIT_REGEX = Regex("(?<=[.!?])\\s+")

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
    val isFetchingFullContent by viewModel.isFetchingFullContent.collectAsState()
    val fullContentFetchResult by viewModel.fullContentFetchResult.collectAsState()
    val quizState by viewModel.quizState.collectAsState()
    
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    
    // 离开页面时保存阅读时长
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.saveReadingTime()
        }
    }
    
    // Calculate reading progress — 只用于进度条显示，轻量
    val readProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else 0f
        }
    }
    
    // 派生布尔值：避免 Column 内部读取浮点 readProgress 导致逐像素重组
    val isReadingAlmostDone by remember {
        derivedStateOf { readProgress > 0.9f }
    }
    
    // 节流：用 snapshotFlow + debounce 来避免每帧都写数据库
    var hasTriggeredQuiz by remember { mutableStateOf(false) }
    @OptIn(FlowPreview::class)
    LaunchedEffect(scrollState) {
        snapshotFlow { 
            if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue.toFloat() else 0f 
        }
            .debounce(500L) // 500ms 内只取最后一个值
            .collect { progress ->
                if (progress > 0.1f) {
                    viewModel.updateReadProgress(progress)
                }
                if (progress > 0.9f && !hasTriggeredQuiz) {
                    hasTriggeredQuiz = true
                    viewModel.onReadingProgressHigh()
                }
            }
    }
    
    LaunchedEffect(fullContentFetchResult) {
        if (fullContentFetchResult == com.englishreader.data.repository.FullContentFetchResult.TOO_SHORT) {
            Toast.makeText(context, "该内容过短，已过滤", Toast.LENGTH_SHORT).show()
            viewModel.clearFullContentFetchResult()
            onBack()
        }
    }
    
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    
    // 判断翻译结果是单词还是句子
    val isSingleWordResult = when (val state = translationState) {
        is TranslationState.Success -> state.original.trim().split(Regex("\\s+")).size <= 1
        else -> false
    }
    
    // 单词翻译用 Popup，句子翻译用 BottomSheet
    var showWordPopup by remember { mutableStateOf(false) }
    
    LaunchedEffect(translationState) {
        when (translationState) {
            is TranslationState.Loading -> {
                // 不在 Loading 时展开 sheet，避免单词翻译出现"闪一下蓝框"
                // sheet 只在确认是句子结果后再展开
            }
            is TranslationState.Success -> {
                val isWord = (translationState as TranslationState.Success).original.trim().split(Regex("\\s+")).size <= 1
                if (isWord) {
                    showWordPopup = true
                    // 确保 sheet 是隐藏的（可能之前因句子翻译展开过）
                    if (bottomSheetState.currentValue != SheetValue.Hidden) {
                        scope.launch { bottomSheetState.hide() }
                    }
                } else {
                    showWordPopup = false
                    scope.launch { bottomSheetState.expand() }
                }
            }
            is TranslationState.SavedWord -> {
                scope.launch {
                    kotlinx.coroutines.delay(800)
                    showWordPopup = false
                    bottomSheetState.hide()
                    viewModel.clearTranslation()
                }
            }
            is TranslationState.SavedSentence -> {
                scope.launch { 
                    kotlinx.coroutines.delay(1000)
                    bottomSheetState.hide()
                    viewModel.clearTranslation()
                }
            }
            else -> {
                showWordPopup = false
            }
        }
    }
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            TranslationSheet(
                state = translationState,
                onSaveWord = { original, translation ->
                    // 自动从文章内容中提取上下文句子
                    val content = article?.content ?: ""
                    val context = extractContextSentence(content, original)
                    viewModel.saveVocabulary(original, translation, context)
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
            // 单词翻译浮窗
            if (showWordPopup && translationState is TranslationState.Success && isSingleWordResult) {
                WordTranslationPopup(
                    state = translationState as TranslationState.Success,
                    onSaveWord = { original, translation ->
                        val content = article?.content ?: ""
                        val ctx = extractContextSentence(content, original)
                        viewModel.saveVocabulary(original, translation, ctx)
                    },
                    onSpeak = { viewModel.speak(it) },
                    onShowMore = {
                        showWordPopup = false
                        scope.launch { bottomSheetState.expand() }
                    },
                    onDismiss = {
                        showWordPopup = false
                        viewModel.clearTranslation()
                    }
                )
            }
            
            // 保存成功浮窗提示
            if (showWordPopup && translationState is TranslationState.SavedWord) {
                Popup(
                    alignment = Alignment.BottomCenter,
                    onDismissRequest = { showWordPopup = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "已保存到生词本",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            if (article == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    ArticleHeader(
                        article = article!!,
                        showSummary = showSummary,
                        onToggleSummary = { viewModel.toggleSummary() }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 提示文字
                    Text(
                        text = "长按选词翻译 · 拖动选句翻译",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Article content
                    ArticleContent(
                        content = article!!.content,
                        fontSize = fontSize,
                        onWordClick = { word ->
                            viewModel.translate(word)
                        },
                        onSentenceClick = { sentence ->
                            viewModel.translate(sentence)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // 阅读完成摘要卡片
                    if (isReadingAlmostDone) {
                        ReadingCompletionCard(
                            summary = viewModel.getReadingSummary()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // 读后三问
                    if (quizState is QuizState.Ready) {
                        QuizSection(
                            state = quizState as QuizState.Ready,
                            onSubmitAnswer = { index, answer ->
                                viewModel.submitQuizAnswer(index, answer)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
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
    onWordClick: (String) -> Unit,
    onSentenceClick: (String) -> Unit
) {
    // 缓存段落分割结果，只在 content 变化时重新分割
    val paragraphs = remember(content) {
        content.split("\n\n").filter { it.isNotBlank() }.map { it.trim() }
    }
    
    // 段距与字体大小成比例: fontSize * 1.2
    val paragraphSpacing = (fontSize * 1.2f).dp
    Column(verticalArrangement = Arrangement.spacedBy(paragraphSpacing)) {
        paragraphs.forEach { paragraph ->
            ClickableParagraph(
                text = paragraph,
                fontSize = fontSize,
                onWordClick = onWordClick,
                onSentenceClick = onSentenceClick
            )
        }
    }
}

@Composable
private fun ClickableParagraph(
    text: String,
    fontSize: Int,
    onWordClick: (String) -> Unit,
    onSentenceClick: (String) -> Unit = {}
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var selectionStart by remember { mutableStateOf<Int?>(null) }
    var selectionEnd by remember { mutableStateOf<Int?>(null) }
    val highlightScope = rememberCoroutineScope()
    
    // 高亮仅在选区变化时才重建，正常滑动时 selectionStart/End 都是 null
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val displayString = remember(text, selectionStart, selectionEnd) {
        val s = selectionStart
        val e = selectionEnd
        if (s != null && e != null) {
            val lo = min(s, e).coerceIn(0, text.length)
            val hi = max(s, e).coerceIn(0, text.length)
            if (lo < hi) {
                buildAnnotatedString {
                    append(text)
                    addStyle(SpanStyle(background = highlightColor), lo, hi)
                }
            } else {
                buildAnnotatedString { append(text) }
            }
        } else {
            buildAnnotatedString { append(text) }
        }
    }
    
    Text(
        text = displayString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.75).sp,
            fontFamily = FontFamily.Serif
        ),
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    textLayoutResult?.let { layoutResult ->
                        val position = layoutResult.getOffsetForPosition(offset)
                        // 直接用字符级扫描找到当前词的边界，比注解更准确
                        val (wordStart, wordEnd) = findWordBoundary(text, position)
                        selectionStart = wordStart
                        selectionEnd = wordEnd
                    }
                },
                onDrag = { change, _ ->
                    textLayoutResult?.let { layoutResult ->
                        val position = layoutResult.getOffsetForPosition(change.position)
                        // 拖动时扩展到整个词的边界，而不是精确到字符
                        val (_, wordEnd) = findWordBoundary(text, position)
                        selectionEnd = wordEnd
                    }
                },
                onDragCancel = {
                    selectionStart = null
                    selectionEnd = null
                },
                onDragEnd = {
                    val start = selectionStart
                    val end = selectionEnd
                    if (start != null && end != null) {
                        val lo = min(start, end).coerceIn(0, text.length)
                        val hi = max(start, end).coerceIn(0, text.length)
                        
                        if (lo < hi) {
                            val selected = text.substring(lo, hi).trim()
                            if (selected.isNotEmpty()) {
                                // 判断选中的内容是单词还是短语/句子
                                val cleaned = selected.trim { !it.isLetter() && it != '\'' && it != '-' }
                                if (cleaned.isNotEmpty() && cleaned.all { it.isLetter() || it == '\'' || it == '-' }) {
                                    onWordClick(cleaned)
                                } else {
                                    onSentenceClick(selected)
                                }
                            }
                        }
                    }
                    // 高亮保持 300ms 后消失
                    highlightScope.launch {
                        kotlinx.coroutines.delay(300)
                        selectionStart = null
                        selectionEnd = null
                    }
                }
            )
        }
    )
}

/**
 * 根据字符位置找到所在单词的边界 [start, end)
 * 比基于正则注解的方式更准确，直接在原始文本上操作
 */
private fun findWordBoundary(text: String, offset: Int): Pair<Int, Int> {
    if (text.isEmpty()) return Pair(0, 0)
    val pos = offset.coerceIn(0, text.length - 1)
    
    // 如果当前位置不是字母，尝试往左找一个字母（用户可能点在了词的右侧空白处）
    var anchor = pos
    if (anchor < text.length && !text[anchor].isLetter()) {
        if (anchor > 0 && text[anchor - 1].isLetter()) {
            anchor = anchor - 1
        }
    }
    
    // 如果仍然不是字母，返回原始位置
    if (anchor >= text.length || !text[anchor].isLetter()) {
        return Pair(offset.coerceIn(0, text.length), offset.coerceIn(0, text.length))
    }
    
    // 向左扫描到词的起始位置
    var start = anchor
    while (start > 0 && (text[start - 1].isLetter() || text[start - 1] == '\'' || text[start - 1] == '-')) {
        start--
    }
    
    // 向右扫描到词的结束位置
    var end = anchor + 1
    while (end < text.length && (text[end].isLetter() || text[end] == '\'' || text[end] == '-')) {
        end++
    }
    
    return Pair(start, end)
}

// ==================== 单词轻量浮窗 ====================

@Composable
private fun WordTranslationPopup(
    state: TranslationState.Success,
    onSaveWord: (String, String) -> Unit,
    onSpeak: (String) -> Unit,
    onShowMore: () -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.BottomCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 单词 + 音标 + 发音
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.original,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onSpeak(state.original) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "朗读",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                state.phonetic?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 释义
                Text(
                    text = state.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
                
                // 来源
                Text(
                    text = if (state.isFromDict) "本地词典" else "AI 翻译",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 操作按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onSaveWord(state.original, state.translation) }
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("生词本", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    TextButton(onClick = onShowMore) {
                        Text("更多", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            Icons.Default.ExpandMore,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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

// ==================== 阅读完成摘要 ====================

@Composable
private fun ReadingCompletionCard(summary: ReadingSummary) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "阅读完成",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.readingTimeMinutes}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "分钟",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.newWordsCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "查词",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.wpm}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "WPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==================== 读后三问 ====================

@Composable
private fun QuizSection(
    state: QuizState.Ready,
    onSubmitAnswer: (Int, String) -> Unit
) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                Icons.Default.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "读后检测",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "检验你的理解",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        state.questions.forEachIndexed { index, question ->
            QuizQuestionCard(
                index = index,
                question = question,
                answer = state.userAnswers[index],
                onSubmit = { answer -> onSubmitAnswer(index, answer) }
            )
            if (index < state.questions.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(
    index: Int,
    question: ComprehensionQuestion,
    answer: QuizAnswer?,
    onSubmit: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${index + 1}. ${question.question}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (answer != null) {
                // 已回答 — 显示反馈
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "你的回答：${answer.userAnswer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = answer.feedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // 未回答 — 显示输入框
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("用英文或中文简短作答…") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSubmit(inputText)
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("提交", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * 从文章内容中提取包含目标单词的句子作为上下文
 */
private fun extractContextSentence(content: String, word: String): String? {
    if (content.isBlank() || word.isBlank()) return null
    val sentences = content.split(SENTENCE_SPLIT_REGEX)
    val target = word.lowercase()
    val wordBoundaryRegex = Regex("\\b${Regex.escape(target)}\\b")
    return sentences.firstOrNull { sentence ->
        sentence.lowercase().contains(wordBoundaryRegex)
    }?.trim()
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
