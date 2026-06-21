package com.twitter.downloader.data.repository

import com.twitter.downloader.data.local.dao.DownloadDao
import com.twitter.downloader.data.local.entity.DownloadEntity
import com.twitter.downloader.data.remote.MediaItem
import com.twitter.downloader.data.remote.TwitterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val api: TwitterApi
) {

    suspend fun getDownloadedUrls(userId: Long): Set<String> {
        return downloadDao.getMediaUrlsByUser(userId).toSet()
    }

    suspend fun getDownloadCount(userId: Long): Int {
        return downloadDao.getCountByUser(userId)
    }

    suspend fun downloadMedia(
        item: MediaItem,
        userId: Long,
        saveDir: File,
        onProgress: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = generateFileName(item)
            val file = File(saveDir, fileName)

            val bytes = api.downloadFile(item.mediaUrl) ?: return@withContext false
            file.writeBytes(bytes)

            downloadDao.insertDownload(
                DownloadEntity(
                    userId = userId,
                    mediaUrl = item.mediaUrl,
                    mediaType = item.mediaType,
                    tweetId = item.tweetId,
                    tweetTime = item.tweetTime,
                    tweetContent = item.tweetContent,
                    filePath = file.absolutePath,
                    status = DownloadEntity.STATUS_DOWNLOADED
                )
            )

            onProgress(fileName)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDownloadsForUser(userId: Long) {
        downloadDao.deleteDownloadsByUser(userId)
    }

    private fun generateFileName(item: MediaItem): String {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault())
            .format(java.util.Date(item.tweetTime))
        val hash = item.mediaUrl.hashCode().toString(16).take(4)
        val ext = if (item.mediaType == "video") "mp4" else "jpg"
        return "${dateStr}_${item.userScreenName}_$hash.$ext"
    }
}
