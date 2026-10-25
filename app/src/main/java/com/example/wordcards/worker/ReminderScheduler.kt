package com.example.wordcards.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 提醒任务的调度入口。
 *
 * 关于「准不准」：WorkManager 的周期任务在 Doze 下会被推迟，只能保证「大致在这个时段」。
 * 产品文案因此写成「每天提醒」而不是「8:00 准点提醒」。要精确必须换 AlarmManager +
 * SCHEDULE_EXACT_ALARM 权限，对一个背单词 App 来说不值得。
 */
object ReminderScheduler {

    const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "daily_reminder_work"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "每日复习提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "到点提醒今天该复习的单词数量"
            }
        )
    }

    /** 从下一个 hour:minute 开始，每 24 小时触发一次。重复调用会覆盖旧任务而不是叠加。 */
    fun schedule(context: Context, hour: Int, minute: Int) {
        ensureNotificationChannel(context)

        val now = LocalDateTime.now()
        var firstRun = now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val initialDelay = Duration.between(now, firstRun).toMillis()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
