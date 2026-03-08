package com.englishreader.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.englishreader.data.local.dao.ReadingStatsDao
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.domain.service.RecommendationService
import com.englishreader.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 早晨阅读提醒 Worker
 * 每天早上推送今日推荐文章
 */
@HiltWorker
class MorningReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val recommendationService: RecommendationService,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // 检查提醒是否启用
        if (!settingsRepository.isReminderEnabled()) {
            return Result.success()
        }
        
        // 获取今日推荐
        val recommendation = recommendationService.getRecommendationForNotification()
        
        if (recommendation != null) {
            val (articleId, articleTitle) = recommendation
            notificationHelper.sendMorningReminder(articleId, articleTitle)
        }
        
        return Result.success()
    }
    
    companion object {
        const val WORK_NAME = "morning_reminder_work"
    }
}

/**
 * 每周学习摘要 Worker
 * 每周日晚推送一条学习摘要通知
 */
@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val readingStatsDao: ReadingStatsDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override suspend fun doWork(): Result {
        if (!settingsRepository.isReminderEnabled()) {
            return Result.success()
        }
        
        // 计算过去 7 天的统计
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
        val startDate = dateFormat.format(calendar.time)
        
        val weekStats = readingStatsDao.getStatsFromDateSync(startDate)
        
        val totalArticles = weekStats.sumOf { it.articlesRead }
        val totalVocabulary = weekStats.sumOf { it.vocabularySaved }
        
        // 只有本周有活动才推送
        if (totalArticles > 0 || totalVocabulary > 0) {
            notificationHelper.sendWeeklySummary(
                articlesRead = totalArticles,
                wordsLearned = totalVocabulary
            )
        }
        
        return Result.success()
    }
    
    companion object {
        const val WORK_NAME = "weekly_summary_work"
    }
}

/**
 * 晚间阅读提醒 Worker
 * 检查今天是否有阅读，没有则发送提醒
 */
@HiltWorker
class EveningReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val readingStatsDao: ReadingStatsDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override suspend fun doWork(): Result {
        // 检查提醒是否启用
        if (!settingsRepository.isReminderEnabled()) {
            return Result.success()
        }
        
        // 检查今天是否有阅读
        val today = dateFormat.format(Date())
        val todayStats = readingStatsDao.getStatsForDate(today)
        
        // 如果今天没有阅读任何文章，发送提醒
        if (todayStats == null || todayStats.articlesRead == 0) {
            notificationHelper.sendEveningReminder()
        }
        
        return Result.success()
    }
    
    companion object {
        const val WORK_NAME = "evening_reminder_work"
    }
}
