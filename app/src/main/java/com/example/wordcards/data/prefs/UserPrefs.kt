package com.example.wordcards.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** 用户设置项。全部给默认值，首次启动无需任何初始化逻辑。 */
data class UserPrefs(
    /** 一轮复习最多几张卡 */
    val sessionSize: Int = 20,
    /** 每天最多引入几个新词，0 表示只复习不学新词 */
    val dailyNewWordLimit: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0
)

interface UserPrefsRepository {
    val prefs: Flow<UserPrefs>

    suspend fun setSessionSize(size: Int)

    suspend fun setDailyNewWordLimit(limit: Int)

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int)
}

/** 用扩展属性保证整个进程只有一个 DataStore 实例（重复创建会抛异常）。 */
val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * DataStore 实现。
 *
 * 这里刻意不用 SharedPreferences：它没有事务、没有 Flow、`commit()` 还可能阻塞主线程。
 * 设置项虽少，但「读到旧值 → UI 闪一下」这种问题一旦出现就很难查。
 */
class DataStoreUserPrefsRepository(
    private val dataStore: DataStore<Preferences>
) : UserPrefsRepository {

    override val prefs: Flow<UserPrefs> = dataStore.data
        .catch { error ->
            // 磁盘读失败时给默认值，不要让整个页面崩掉
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            val defaults = UserPrefs()
            UserPrefs(
                sessionSize = preferences[Keys.SESSION_SIZE] ?: defaults.sessionSize,
                dailyNewWordLimit = preferences[Keys.DAILY_NEW_LIMIT] ?: defaults.dailyNewWordLimit,
                reminderEnabled = preferences[Keys.REMINDER_ENABLED] ?: defaults.reminderEnabled,
                reminderHour = preferences[Keys.REMINDER_HOUR] ?: defaults.reminderHour,
                reminderMinute = preferences[Keys.REMINDER_MINUTE] ?: defaults.reminderMinute
            )
        }

    override suspend fun setSessionSize(size: Int) {
        dataStore.edit { it[Keys.SESSION_SIZE] = size.coerceIn(MIN_SESSION_SIZE, MAX_SESSION_SIZE) }
    }

    override suspend fun setDailyNewWordLimit(limit: Int) {
        dataStore.edit { it[Keys.DAILY_NEW_LIMIT] = limit.coerceIn(0, MAX_DAILY_NEW_LIMIT) }
    }

    override suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    private object Keys {
        val SESSION_SIZE = intPreferencesKey("session_size")
        val DAILY_NEW_LIMIT = intPreferencesKey("daily_new_limit")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    companion object {
        const val MIN_SESSION_SIZE = 5
        const val MAX_SESSION_SIZE = 50
        const val MAX_DAILY_NEW_LIMIT = 50
    }
}
