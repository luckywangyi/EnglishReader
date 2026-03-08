package com.englishreader.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * 启动每日提醒任务
     */
    fun scheduleReminders(morningHour: Int = 10, eveningHour: Int = 21) {
        scheduleMorningReminder(morningHour)
        scheduleEveningReminder(eveningHour)
        scheduleWeeklySummary()
    }
    
    /**
     * 调度早晨提醒
     * 每天指定时间执行
     */
    private fun scheduleMorningReminder(hour: Int) {
        val initialDelay = calculateInitialDelay(hour, 0)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<MorningReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            MorningReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * 调度晚间提醒
     * 每天指定时间执行
     */
    private fun scheduleEveningReminder(hour: Int) {
        val initialDelay = calculateInitialDelay(hour, 0)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<EveningReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            EveningReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * 调度每周学习摘要
     * 每周日 20:00 推送
     */
    private fun scheduleWeeklySummary() {
        val initialDelay = calculateDelayToNextSunday(20)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(
            7, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WeeklySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * 计算到下一个周日指定时间的延迟（毫秒）
     */
    private fun calculateDelayToNextSunday(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        if (target.before(now) || target == now) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }
        
        return target.timeInMillis - now.timeInMillis
    }
    
    /**
     * 计算到指定时间的初始延迟（毫秒）
     */
    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 如果目标时间已过，设置为明天
        if (target.before(now) || target == now) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return target.timeInMillis - now.timeInMillis
    }
    
    /**
     * 取消所有提醒任务
     */
    fun cancelAllReminders() {
        workManager.cancelUniqueWork(MorningReminderWorker.WORK_NAME)
        workManager.cancelUniqueWork(EveningReminderWorker.WORK_NAME)
        workManager.cancelUniqueWork(WeeklySummaryWorker.WORK_NAME)
    }
    
    /**
     * 检查提醒是否已调度
     */
    fun isReminderScheduled(): Boolean {
        val morningInfo = workManager.getWorkInfosForUniqueWork(MorningReminderWorker.WORK_NAME)
        val eveningInfo = workManager.getWorkInfosForUniqueWork(EveningReminderWorker.WORK_NAME)
        
        return try {
            val morningWorks = morningInfo.get()
            val eveningWorks = eveningInfo.get()
            morningWorks.isNotEmpty() && eveningWorks.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
