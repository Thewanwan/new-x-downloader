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
import com.twitter.downloader.data.remote.MediaResponse
import com.twitter.downloader.data.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
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
            _downloadState.value = DownloadState.Loading("正在获取推文...")

            try {
                val saveDir = File(getExternalFilesDir(null), screenName)
                if (!saveDir.exists()) saveDir.mkdirs()

                val existingUrls = if (incremental) {
                    repository.getDownloadedUrls(userId)
                } else {
                    emptySet()
                }

                var cursor: String? = null
                var totalDownloaded = 0
                var hasMore = true
                var retryCount = 0
                val maxRetries = 3

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
                                "正在下载... ($totalDownloaded)"
                            )
                            updateNotification("正在下载... ($totalDownloaded)")

                            for (item in items) {
                                val success = repository.downloadMedia(
                                    item = item,
                                    userId = userId,
                                    saveDir = saveDir
                                ) { fileName ->
                                    _downloadState.value = DownloadState.Loading(
                                        "正在下载: $fileName ($totalDownloaded)"
                                    )
                                    updateNotification("正在下载 ($totalDownloaded)")
                                }
                                if (success) totalDownloaded++
                            }

                            cursor = response.nextCursor
                            if (cursor == null) hasMore = false
                        }
                        is MediaResponse.Error -> {
                            if (retryCount < maxRetries && response.message.contains("超时")) {
                                retryCount++
                                kotlinx.coroutines.delay(2000L * retryCount)
                                continue
                            }
                            _downloadState.value = DownloadState.Error(response.message)
                            hasMore = false
                        }
                    }
                }

                _downloadState.value = DownloadState.Success(totalDownloaded)
                updateNotification("下载完成! 共 $totalDownloaded 个文件")
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadState.value = DownloadState.Error("下载出错: ${e.message}")
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = createNotification(text)
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
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
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_NAME = "screen_name"
        const val EXTRA_COOKIE = "cookie"
        const val EXTRA_REST_ID = "rest_id"
        const val EXTRA_INCREMENTAL = "incremental"
        const val NOTIFICATION_ID = 1001

        private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
        val downloadState: StateFlow<DownloadState> = _downloadState

        fun start(
            context: Context,
            user: UserEntity,
            incremental: Boolean
        ) {
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
    data class Loading(val message: String) : DownloadState()
    data class Success(val count: Int) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
