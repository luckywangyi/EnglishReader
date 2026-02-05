package com.englishreader.ui.screens.rss

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.local.entity.CustomRssSourceEntity
import com.englishreader.data.remote.rss.RssSources
import com.englishreader.data.repository.CustomRssSourceRepository
import com.englishreader.domain.model.RssSource
import com.prof18.rssparser.RssParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RssManageViewModel @Inject constructor(
    private val customRssSourceRepository: CustomRssSourceRepository,
    private val rssParser: RssParser
) : ViewModel() {
    
    // 内置 RSS 源
    val builtInSources: List<RssSource> = RssSources.sources
    
    // 自定义 RSS 源
    val customSources: StateFlow<List<CustomRssSourceEntity>> = customRssSourceRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 添加对话框状态
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()
    
    // 添加状态
    private val _addState = MutableStateFlow<AddState>(AddState.Idle)
    val addState: StateFlow<AddState> = _addState.asStateFlow()
    
    // 输入的 URL
    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()
    
    // 解析出的名称
    private val _parsedName = MutableStateFlow("")
    val parsedName: StateFlow<String> = _parsedName.asStateFlow()
    
    fun showAddDialog() {
        _showAddDialog.value = true
        _inputUrl.value = ""
        _parsedName.value = ""
        _addState.value = AddState.Idle
    }
    
    fun hideAddDialog() {
        _showAddDialog.value = false
        _addState.value = AddState.Idle
    }
    
    fun updateInputUrl(url: String) {
        _inputUrl.value = url
    }
    
    fun updateParsedName(name: String) {
        _parsedName.value = name
    }
    
    /**
     * 验证并解析 RSS 源
     */
    fun validateRssUrl() {
        val url = _inputUrl.value.trim()
        if (url.isBlank()) return
        
        viewModelScope.launch {
            _addState.value = AddState.Validating
            
            try {
                val channel = withContext(Dispatchers.IO) {
                    rssParser.getRssChannel(url)
                }
                
                _parsedName.value = channel.title ?: "未命名订阅源"
                _addState.value = AddState.Validated(
                    name = _parsedName.value,
                    articleCount = channel.items.size
                )
            } catch (e: Exception) {
                _addState.value = AddState.Error("无法解析该 RSS 源：${e.message}")
            }
        }
    }
    
    /**
     * 添加 RSS 源
     */
    fun addSource() {
        val url = _inputUrl.value.trim()
        val name = _parsedName.value.trim()
        
        if (url.isBlank() || name.isBlank()) return
        
        viewModelScope.launch {
            _addState.value = AddState.Adding
            
            val success = customRssSourceRepository.addSource(
                name = name,
                url = url
            )
            
            if (success) {
                _addState.value = AddState.Added
                hideAddDialog()
            } else {
                _addState.value = AddState.Error("该 RSS 源已存在")
            }
        }
    }
    
    /**
     * 切换自定义源启用状态
     */
    fun toggleCustomSourceEnabled(source: CustomRssSourceEntity) {
        viewModelScope.launch {
            customRssSourceRepository.toggleEnabled(source.id, !source.isEnabled)
        }
    }
    
    /**
     * 删除自定义源
     */
    fun deleteCustomSource(source: CustomRssSourceEntity) {
        viewModelScope.launch {
            customRssSourceRepository.deleteSource(source.id)
        }
    }
}

sealed class AddState {
    data object Idle : AddState()
    data object Validating : AddState()
    data class Validated(val name: String, val articleCount: Int) : AddState()
    data object Adding : AddState()
    data object Added : AddState()
    data class Error(val message: String) : AddState()
}
