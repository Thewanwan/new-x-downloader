package com.twitter.downloader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twitter.downloader.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDownloadsByUser(userId: Long): Flow<List<DownloadEntity>>

    @Query("SELECT mediaUrl FROM downloads WHERE userId = :userId AND status = :status")
    suspend fun getMediaUrlsByUser(userId: Long, status: String = DownloadEntity.STATUS_DOWNLOADED): List<String>

    @Query("SELECT COUNT(*) FROM downloads WHERE userId = :userId AND status = :status")
    suspend fun getCountByUser(userId: Long, status: String = DownloadEntity.STATUS_DOWNLOADED): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDownloads(downloads: List<DownloadEntity>): List<Long>

    @Query("UPDATE downloads SET status = :status, filePath = :filePath WHERE mediaUrl = :mediaUrl")
    suspend fun updateStatus(mediaUrl: String, status: String, filePath: String)

    @Query("DELETE FROM downloads WHERE userId = :userId")
    suspend fun deleteDownloadsByUser(userId: Long)

    @Query("SELECT SUM(1) FROM downloads WHERE status = 'downloaded'")
    fun getTotalDownloadedCount(): Flow<Int?>
}
