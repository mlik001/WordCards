package com.example.wordcards.worker

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wordcards.MainActivity
import com.example.wordcards.R
import com.example.wordcards.WordCardsApplication
import com.example.wordcards.util.DateProvider
import kotlinx.coroutines.flow.first

/**
 * 每日提醒 Worker。
 *
 * 三个务实的取舍：
 * 1. 用 CoroutineWorker 而不是直接 runBlocking 查数据库；
 * 2. 没有待复习/新词时**不发通知**——宁可安静，也不要每天推一条空提醒；
 * 3. API 33+ 没有通知权限时直接返回 success，不重试（重试也弹不出来，只会耗电）。
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? WordCardsApplication ?: return Result.success()
        val today = DateProvider.todayEpochDay(app.container.clock)

        val dueCount = app.container.wordRepository.observeDueCount(today).first()
        val newCount = app.container.wordRepository.observeNewWordCount().first()
        if (dueCount + newCount == 0) return Result.success()

        if (!canPostNotifications()) return Result.success()

        notify(dueCount = dueCount, newCount = newCount)
        return Result.success()
    }

    private fun canPostNotifications(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return granted && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }

    private fun notify(dueCount: Int, newCount: Int) {
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = buildString {
            if (dueCount > 0) append("$dueCount 张卡片到期")
            if (dueCount > 0 && newCount > 0) append("，")
            if (newCount > 0) append("$newCount 个新词待学")
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            ReminderScheduler.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今天的单词还没背")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_NAME = "每日复习提醒"
    }
}
