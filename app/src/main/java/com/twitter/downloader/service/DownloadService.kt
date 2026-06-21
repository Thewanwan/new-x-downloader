package com.twitter.downloader.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.twitter.downloader.MainActivity
import com.twitter.downloader.TwitterDownloaderApp
import com.twitter.downloader.data.local.entity.UserEntity
import com.twitter.downloader.data.remote.TwitterApi
import com.twitter.downloader.data.remote.MediaItem
import com.twitter.downloader.data.remote.MediaResponse
import com.twitter.downloader.data.repository.DownloadRepository
import com.twitter.downloader.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var api: TwitterApi
    private lateinit var repository: DownloadRepository
    private var currentJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        api = TwitterApi()
        val db = (application as TwitterDownloaderApp).database
        repository = DownloadRepository(db.downloadDao(), api)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val userId = intent.getLongExtra(EXTRA_USER_ID, -1)
                val screenName = intent.getStringExtra(EXTRA_SCREEN_NAME) ?: ""
                val cookie = intent.getStringExtra(EXTRA_COOKIE) ?: ""
                val restId = intent.getStringExtra(EXTRA_REST_ID) ?: ""
                val incremental = intent.getBooleanExtra(EXTRA_INCREMENTAL, true)

                if (userId == -1L || screenName.isEmpty() || cookie.isEmpty() || restId.isEmpty()) {
                    _downloadState.value = DownloadState.Error("参数不完整")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(NOTIFICATION_ID, createNotification("准备下载 @$screenName..."))
                startDownload(userId, screenName, cookie, restId, incremental)
            }
            ACTION_STOP -> {
                currentJob?.cancel()
                _downloadState.value = DownloadState.Error("下载已取消")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_RETRY -> {
                val userId = intent.getLongExtra(EXTRA_USER_ID, -1)
                val screenName = intent.getStringExtra(EXTRA_SCREEN_NAME) ?: ""
                val cookie = intent.getStringExtra(EXTRA_COOKIE) ?: ""
                val restId = intent.getStringExtra(EXTRA_REST_ID) ?: ""
                val incremental = intent.getBooleanExtra(EXTRA_INCREMENTAL, true)

                startForeground(NOTIFICATION_ID, createNotification("重试下载 @$screenName..."))
                startDownload(userId, screenName, cookie, restId, incremental)
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(
        userId: Long,
        screenName: String,
        cookie: String,
        restId: String,
        incremental: Boolean
    ) {
        currentJob?.cancel()
        currentJob = scope.launch {
            Logger.logDownloadStart(screenName)
            _downloadState.value = DownloadState.Loading(
                message = "正在获取推文...",
                currentFile = "",
                downloadedCount = 0,
                totalCount = 0
            )

            try {
                val externalDir = getExternalFilesDir(null)
                if (externalDir == null) {
                    _downloadState.value = DownloadState.Error("无法访问外部存储")
                    return@launch
                }

                // 使用应用默认下载目录
                val downloadDir = File(externalDir, "Downloads")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val saveDir = File(downloadDir, screenName)
                if (!saveDir.exists() && !saveDir.mkdirs()) {
                    _downloadState.value = DownloadState.Error("无法创建保存目录")
                    return@launch
                }
                Logger.i("Download", "保存路径: ${saveDir.absolutePath}")

                val existingUrls = if (incremental) {
                    repository.getDownloadedUrls(userId)
                } else {
                    emptySet()
                }

                var cursor: String? = null
                var totalDownloaded = 0
                var totalFailed = 0
                var hasMore = true
                var retryCount = 0
                val maxRetries = 3
                val failedItems = mutableListOf<MediaItem>()

                while (hasMore) {
                    when (val response = api.getUserMedia(restId, cursor, cookie)) {
                        is MediaResponse.Success -> {
                            retryCount = 0
                            val items = if (incremental) {
                                response.items.filter { it.mediaUrl !in existingUrls }
                            } else {
                                response.items
                            }

                            if (items.isEmpty() && response.nextCursor == null) {
                                hasMore = false
                                continue
                            }

                            if (items.isEmpty()) {
                                cursor = response.nextCursor
                                continue
                            }

                            _downloadState.value = DownloadState.Loading(
                                message = "正在下载... ($totalDownloaded/${items.size})",
                                currentFile = "",
                                downloadedCount = totalDownloaded,
                                totalCount = items.size
                            )
                            updateNotification("正在下载 ($totalDownloaded/${items.size})")

                            for ((index, item) in items.withIndex()) {
                                val fileName = generateFileName(item)
                                _downloadState.value = DownloadState.Loading(
                                    message = "正在下载 ${index + 1}/${items.size}",
                                    currentFile = fileName,
                                    downloadedCount = totalDownloaded,
                                    totalCount = items.size
                                )

                                val success = repository.downloadMedia(
                                    item = item,
                                    userId = userId,
                                    saveDir = saveDir
                                ) { }
                                if (success) {
                                    totalDownloaded++
                                } else {
                                    totalFailed++
                                    failedItems.add(item)
                                }
                            }

                            cursor = response.nextCursor
                            if (cursor == null) hasMore = false
                        }
                        is MediaResponse.Error -> {
                            if (retryCount < maxRetries && response.message.contains("超时")) {
                                retryCount++
                                Logger.w("Download", "超时重试 ($retryCount/$maxRetries)")
                                _downloadState.value = DownloadState.Loading(
                                    message = "超时重试 ($retryCount/$maxRetries)...",
                                    currentFile = "",
                                    downloadedCount = totalDownloaded,
                                    totalCount = 0
                                )
                                delay(2000L * retryCount)
                                continue
                            }
                            Logger.logDownloadError(screenName, response.message)
                            _downloadState.value = DownloadState.Error(
                                message = response.message,
                                canRetry = true,
                                userId = userId,
                                screenName = screenName,
                                cookie = cookie,
                                restId = restId,
                                incremental = incremental
                            )
                            hasMore = false
                        }
                    }
                }

                Logger.logDownloadComplete(screenName, totalDownloaded)

                if (failedItems.isNotEmpty()) {
                    _downloadState.value = DownloadState.Partial(
                        message = "下载完成，$totalFailed 个文件失败",
                        downloadedCount = totalDownloaded,
                        failedCount = totalFailed,
                        canRetry = true,
                        userId = userId,
                        screenName = screenName,
                        cookie = cookie,
                        restId = restId,
                        incremental = incremental
                    )
                } else {
                    _downloadState.value = DownloadState.Success(totalDownloaded)
                }

                updateNotification("下载完成! 共 $totalDownloaded 个文件")
            } catch (e: Exception) {
                Logger.e("Download", "下载出错", e)
                _downloadState.value = DownloadState.Error(
                    message = "下载出错: ${e.message}",
                    canRetry = true,
                    userId = userId,
                    screenName = screenName,
                    cookie = cookie,
                    restId = restId,
                    incremental = incremental
                )
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun generateFileName(item: MediaItem): String {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault())
            .format(java.util.Date(item.tweetTime))
        val hash = item.mediaUrl.hashCode().toString(16).take(4)
        val ext = if (item.mediaType == "video") "mp4" else "jpg"
        val safeName = item.userScreenName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return "${dateStr}_${safeName}_$hash.$ext"
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TwitterDownloaderApp.CHANNEL_ID)
            .setContentTitle("X Downloader")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        currentJob?.cancel()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.twitter.downloader.START"
        const val ACTION_STOP = "com.twitter.downloader.STOP"
        const val ACTION_RETRY = "com.twitter.downloader.RETRY"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_NAME = "screen_name"
        const val EXTRA_COOKIE = "cookie"
        const val EXTRA_REST_ID = "rest_id"
        const val EXTRA_INCREMENTAL = "incremental"
        const val NOTIFICATION_ID = 1001

        private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
        val downloadState: StateFlow<DownloadState> = _downloadState

        fun start(context: Context, user: UserEntity, incremental: Boolean) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, user.id)
                putExtra(EXTRA_SCREEN_NAME, user.screenName)
                putExtra(EXTRA_COOKIE, user.cookie)
                putExtra(EXTRA_REST_ID, user.restId)
                putExtra(EXTRA_INCREMENTAL, incremental)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun retry(context: Context, state: DownloadState.Error) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RETRY
                putExtra(EXTRA_USER_ID, state.userId)
                putExtra(EXTRA_SCREEN_NAME, state.screenName)
                putExtra(EXTRA_COOKIE, state.cookie)
                putExtra(EXTRA_REST_ID, state.restId)
                putExtra(EXTRA_INCREMENTAL, state.incremental)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun retryPartial(context: Context, state: DownloadState.Partial) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RETRY
                putExtra(EXTRA_USER_ID, state.userId)
                putExtra(EXTRA_SCREEN_NAME, state.screenName)
                putExtra(EXTRA_COOKIE, state.cookie)
                putExtra(EXTRA_REST_ID, state.restId)
                putExtra(EXTRA_INCREMENTAL, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}

sealed class DownloadState {
    data object Idle : DownloadState()

    data class Loading(
        val message: String,
        val currentFile: String = "",
        val downloadedCount: Int = 0,
        val totalCount: Int = 0
    ) : DownloadState()

    data class Success(val count: Int) : DownloadState()

    data class Partial(
        val message: String,
        val downloadedCount: Int,
        val failedCount: Int,
        val canRetry: Boolean = false,
        val userId: Long = 0,
        val screenName: String = "",
        val cookie: String = "",
        val restId: String = "",
        val incremental: Boolean = true
    ) : DownloadState()

    data class Error(
        val message: String,
        val canRetry: Boolean = false,
        val userId: Long = 0,
        val screenName: String = "",
        val cookie: String = "",
        val restId: String = "",
        val incremental: Boolean = true
    ) : DownloadState()
}
