package com.example.wordcards

import android.app.Application
import com.example.wordcards.di.AppContainer
import com.example.wordcards.di.DefaultAppContainer
import com.example.wordcards.worker.ReminderScheduler

/**
 * 进程级入口：装配依赖图。
 *
 * 这里只做「创建容器 + 建通知渠道」两件事，不放任何业务逻辑，
 * 保证冷启动路径足够短。
 */
class WordCardsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        ReminderScheduler.ensureNotificationChannel(this)
    }
}
