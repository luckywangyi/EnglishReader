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
    val isFetchingFullContent by viewModel.isFetchingFullContent.collectAsState()
    val fullContentFetchResult by viewModel.fullContentFetchResult.collectAsState()
    
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
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
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
                    
                    // Article content - tap on words, long press on sentences
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
    // Split content into paragraphs
    val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        paragraphs.forEach { paragraph ->
            ClickableParagraph(
                text = paragraph.trim(),
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
    
    // 将段落分割为句子
    val sentences = text.split(Regex("(?<=[.!?])\\s+"))
    
    val annotatedString = buildAnnotatedString {
        var globalIndex = 0
        
        sentences.forEachIndexed { sentenceIndex, sentence ->
            val sentenceStart = globalIndex
            
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
    
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.6).sp,
            fontFamily = FontFamily.Serif
        ),
        textAlign = TextAlign.Justify,
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.pointerInput(Unit) {
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

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
