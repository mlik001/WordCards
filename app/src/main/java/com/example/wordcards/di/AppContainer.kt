package com.example.wordcards.di

import android.content.Context
import com.example.wordcards.data.backup.DefaultWordBackupService
import com.example.wordcards.data.backup.WordBackupService
import com.example.wordcards.data.local.AppDatabase
import com.example.wordcards.data.prefs.DataStoreUserPrefsRepository
import com.example.wordcards.data.prefs.UserPrefsRepository
import com.example.wordcards.data.prefs.userPrefsDataStore
import com.example.wordcards.data.reminder.AndroidReminderController
import com.example.wordcards.data.reminder.ReminderController
import com.example.wordcards.data.repository.DefaultWordRepository
import com.example.wordcards.data.repository.WordRepository
import com.example.wordcards.data.wordbook.DefaultWordBookService
import com.example.wordcards.data.wordbook.WordBookCatalog
import com.example.wordcards.data.wordbook.WordBookService
import com.example.wordcards.domain.review.LeitnerScheduler
import com.example.wordcards.domain.review.ReviewScheduler
import java.time.Clock

/**
 * 全局依赖图。
 *
 * MVP 用手写容器而不是 Hilt：项目只有 5 个 ViewModel，Hilt 带来的注解处理成本
 * 与「一眼能看完整张依赖图」的收益不成正比。所有依赖都从这里出去，
 * 将来换 Hilt 也只需要把这几行改成 @Provides。
 */
interface AppContainer {
    val database: AppDatabase
    val scheduler: ReviewScheduler
    val clock: Clock
    val wordRepository: WordRepository
    val prefsRepository: UserPrefsRepository
    val backupService: WordBackupService
    val reminderController: ReminderController
    val wordBookService: WordBookService
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext: Context = context.applicationContext

    override val database: AppDatabase by lazy { AppDatabase.get(appContext) }

    override val scheduler: ReviewScheduler by lazy { LeitnerScheduler() }

    /** 系统时区时钟；测试时换成 Clock.fixed(...) 即可模拟任意一天。 */
    override val clock: Clock by lazy { Clock.systemDefaultZone() }

    override val wordRepository: WordRepository by lazy {
        DefaultWordRepository(
            database = database,
            wordDao = database.wordDao(),
            reviewLogDao = database.reviewLogDao(),
            scheduler = scheduler
        )
    }

    override val prefsRepository: UserPrefsRepository by lazy {
        DataStoreUserPrefsRepository(appContext.userPrefsDataStore)
    }

    override val backupService: WordBackupService by lazy {
        DefaultWordBackupService(appContext, wordRepository, clock)
    }

    override val reminderController: ReminderController by lazy {
        AndroidReminderController(appContext)
    }

    override val wordBookService: WordBookService by lazy {
        DefaultWordBookService(
            catalog = WordBookCatalog(appContext),
            repository = wordRepository,
            clock = clock
        )
    }
}
