package com.twitter.downloader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.twitter.downloader.data.local.dao.DownloadDao
import com.twitter.downloader.data.local.dao.UserDao
import com.twitter.downloader.data.local.entity.DownloadEntity
import com.twitter.downloader.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "twitter_downloader.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
