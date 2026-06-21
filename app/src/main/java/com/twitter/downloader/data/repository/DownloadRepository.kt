package com.twitter.downloader.data.repository

import com.twitter.downloader.data.local.dao.DownloadDao
import com.twitter.downloader.data.local.entity.DownloadEntity
import com.twitter.downloader.data.remote.MediaItem
import com.twitter.downloader.data.remote.TwitterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val api: TwitterApi
) {

    suspend fun getDownloadedUrls(userId: Long): Set<String> {
        return try {
            downloadDao.getMediaUrlsByUser(userId).toSet()
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    suspend fun getDownloadCount(userId: Long): Int {
        return try {
            downloadDao.getCountByUser(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
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

            if (file.exists() && file.length() > 0) {
                onProgress(fileName)
                return@withContext true
            }

            val tempFile = File(saveDir, "$fileName.tmp")
            try {
                tempFile.delete()
                val success = api.downloadFileTo(item.mediaUrl, tempFile)
                if (!success || !tempFile.exists() || tempFile.length() == 0L) {
                    tempFile.delete()
                    return@withContext false
                }

                if (file.exists()) file.delete()
                val renamed = tempFile.renameTo(file)
                if (!renamed) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }

                if (!file.exists() || file.length() == 0L) {
                    return@withContext false
                }
            } catch (e: IOException) {
                tempFile.delete()
                throw e
            }

            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onProgress(fileName)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDownloadsForUser(userId: Long) {
        try {
            downloadDao.deleteDownloadsByUser(userId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateFileName(item: MediaItem): String {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault())
            .format(java.util.Date(item.tweetTime))
        val hash = item.mediaUrl.hashCode().toString(16).take(4)
        val ext = if (item.mediaType == "video") "mp4" else "jpg"
        val safeScreenName = item.userScreenName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return "${dateStr}_${safeScreenName}_$hash.$ext"
    }
}
