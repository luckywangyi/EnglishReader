package com.englishreader.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.englishreader.MainActivity
import com.englishreader.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "reading_reminder"
        const val CHANNEL_NAME = "阅读提醒"
        const val CHANNEL_DESCRIPTION = "每日阅读提醒通知"
        
        const val NOTIFICATION_ID_MORNING = 1001
        const val NOTIFICATION_ID_EVENING = 1002
        const val NOTIFICATION_ID_WEEKLY = 1003
        
        const val EXTRA_ARTICLE_ID = "article_id"
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * 发送早晨推荐阅读通知
     */
    fun sendMorningReminder(articleId: String, articleTitle: String) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ARTICLE_ID, articleId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MORNING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("今日推荐阅读")
            .setContentText(articleTitle)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(articleTitle)
                .setSummaryText("点击开始今天的英语阅读之旅"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING, notification)
        } catch (e: SecurityException) {
            // 权限被拒绝
        }
    }
    
    /**
     * 发送晚间提醒通知
     */
    fun sendEveningReminder() {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_EVENING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("别忘了今天的阅读")
            .setContentText("坚持阅读，保持学习习惯")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EVENING, notification)
        } catch (e: SecurityException) {
            // 权限被拒绝
        }
    }
    
    /**
     * 发送每周学习摘要通知
     */
    fun sendWeeklySummary(
        articlesRead: Int,
        wordsLearned: Int,
        lookupRateChange: String? = null
    ) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "stats")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_WEEKLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val summaryParts = mutableListOf<String>()
        summaryParts.add("本周阅读 $articlesRead 篇")
        if (wordsLearned > 0) {
            summaryParts.add("掌握 $wordsLearned 个新词")
        }
        if (lookupRateChange != null) {
            summaryParts.add(lookupRateChange)
        }
        
        val summaryText = summaryParts.joinToString("，") + "。"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("本周学习摘要")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(summaryText)
                .setSummaryText("点击查看详细统计"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WEEKLY, notification)
        } catch (e: SecurityException) {
            // 权限被拒绝
        }
    }
    
    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
