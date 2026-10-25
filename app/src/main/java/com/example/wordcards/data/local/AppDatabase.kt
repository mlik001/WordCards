package com.example.wordcards.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.wordcards.data.local.dao.ReviewLogDao
import com.example.wordcards.data.local.dao.WordDao
import com.example.wordcards.data.local.entity.ReviewLog
import com.example.wordcards.data.local.entity.Word

/**
 * 应用唯一的数据库。
 *
 * `exportSchema = true` 从 version 1 就开启：Room 会把建表 SQL 导出到 app/schemas，
 * 将来加字段时能据此写出可验证的 Migration，而不是一句 fallbackToDestructiveMigration 清库了事。
 */
@Database(
    entities = [Word::class, ReviewLog::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        private const val DB_NAME = "wordcards.db"

        @Volatile
        private var instance: AppDatabase? = null

        /** 进程内单例；Room 实例本身是线程安全的。 */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
