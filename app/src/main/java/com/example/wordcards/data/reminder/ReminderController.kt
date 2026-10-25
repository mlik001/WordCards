package com.example.wordcards.data.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.wordcards.worker.ReminderScheduler

/**
 * 提醒能力的门面。
 *
 * ViewModel 需要一个「能开关提醒」的对象，但它不能持有 Context。
 * 这里只暴露两个方法，把 WorkManager 和权限判断留在实现里。
 */
interface ReminderController {
    fun apply(enabled: Boolean, hour: Int, minute: Int)

    /** false 表示系统还没给通知权限，UI 需要先发起运行时申请。 */
    fun hasNotificationPermission(): Boolean
}

class AndroidReminderController(private val context: Context) : ReminderController {

    override fun apply(enabled: Boolean, hour: Int, minute: Int) {
        if (enabled) {
            ReminderScheduler.schedule(context, hour, minute)
        } else {
            ReminderScheduler.cancel(context)
        }
    }

    override fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
