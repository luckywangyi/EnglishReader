package com.englishreader.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS (Text-to-Speech) 管理器
 * 用于英语单词和句子的发音
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    init {
        initTts()
    }
    
    private fun initTts() {
        try {
            tts = TextToSpeech(context) { status ->
                try {
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.US)
                        _isInitialized.value = result != TextToSpeech.LANG_MISSING_DATA 
                            && result != TextToSpeech.LANG_NOT_SUPPORTED
                        
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                            }
                            
                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                            }
                            
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                            }
                            
                            override fun onError(utteranceId: String?, errorCode: Int) {
                                _isSpeaking.value = false
                            }
                        })
                    } else {
                        _isInitialized.value = false
                    }
                } catch (e: Exception) {
                    _isInitialized.value = false
                }
            }
        } catch (e: Exception) {
            // TTS 初始化失败，但不应该导致应用崩溃
            _isInitialized.value = false
        }
    }
    
    /**
     * 朗读文本（英语）
     * @param text 要朗读的文本
     * @param speed 语速 (0.5f - 2.0f, 默认 1.0f)
     */
    fun speak(text: String, speed: Float = 1.0f) {
        if (!_isInitialized.value || text.isBlank()) return
        
        tts?.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UUID.randomUUID().toString()
        )
    }
    
    /**
     * 朗读单词（较慢语速，更清晰）
     */
    fun speakWord(word: String) {
        speak(word, 0.8f)
    }
    
    /**
     * 朗读句子（正常语速）
     */
    fun speakSentence(sentence: String) {
        speak(sentence, 1.0f)
    }
    
    /**
     * 停止朗读
     */
    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }
    
    /**
     * 释放资源
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isInitialized.value = false
        _isSpeaking.value = false
    }
    
    /**
     * 设置语言（默认美式英语）
     */
    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA 
            && result != TextToSpeech.LANG_NOT_SUPPORTED
    }
    
    /**
     * 切换到英式英语
     */
    fun useBritishEnglish() {
        setLanguage(Locale.UK)
    }
    
    /**
     * 切换到美式英语
     */
    fun useAmericanEnglish() {
        setLanguage(Locale.US)
    }
}
