package com.twitter.downloader.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val REPO_OWNER = "Thewanwan"
    private const val REPO_NAME = "new-x-downloader"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            Logger.i(TAG, "当前版本: $currentVersion")

            val url = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
            Logger.i(TAG, "请求: $url")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "XDownloader-Android")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Logger.i(TAG, "响应码: $responseCode")

            if (responseCode != 200) {
                Logger.e(TAG, "请求失败: HTTP $responseCode")
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().readText()
            Logger.d(TAG, "响应: ${response.take(200)}")

            val json = JSONObject(response)
            val latestVersion = json.getString("tag_name").removePrefix("v")
            val releaseNotes = json.optString("body", "")
            val assets = json.getJSONArray("assets")
            var downloadUrl = ""

            if (assets.length() > 0) {
                downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
            }

            val hasUpdate = compareVersions(latestVersion, currentVersion) > 0
            Logger.i(TAG, "最新版本: $latestVersion, 有更新: $hasUpdate")

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )
        } catch (e: Exception) {
            Logger.e(TAG, "检查更新失败", e)
            null
        }
    }

    fun downloadAndInstall(context: Context, downloadUrl: String) {
        Logger.i(TAG, "开始下载更新: $downloadUrl")

        val fileName = "XDownloader.apk"
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("下载更新")
            .setDescription("正在下载新版本...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        Logger.i(TAG, "下载任务已启动, ID: $downloadId")

        // 启动一个线程监听下载完成
        Thread {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Logger.i(TAG, "下载完成")
                            downloading = false
                            installApk(context, downloadId)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            Logger.e(TAG, "下载失败")
                            downloading = false
                        }
                    }
                }
                cursor?.close()
                if (downloading) Thread.sleep(1000)
            }
        }.start()
    }

    private fun installApk(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Logger.e(TAG, "安装失败", e)
            // 尝试备用方式
            val installIntent2 = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent2)
        }
    }

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val v1 = parts1.getOrElse(i) { 0 }
            val v2 = parts2.getOrElse(i) { 0 }
            if (v1 != v2) return v1 - v2
        }
        return 0
    }
}
