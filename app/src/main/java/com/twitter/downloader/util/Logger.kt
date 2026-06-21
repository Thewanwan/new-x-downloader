package com.twitter.downloader.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val TAG = "XDownloader"
    private var logFile: File? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) logDir.mkdirs()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        logFile = File(logDir, "log_$dateStr.txt")
        isInitialized = true
    }

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun writeLog(level: String, tag: String, message: String) {
        val timestamp = getTimestamp()
        val logEntry = "[$timestamp] $level/$tag: $message\n"

        Log.d(TAG, logEntry)

        try {
            logFile?.appendText(logEntry)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    fun d(tag: String, message: String) {
        writeLog("D", tag, message)
    }

    fun i(tag: String, message: String) {
        writeLog("I", tag, message)
    }

    fun w(tag: String, message: String) {
        writeLog("W", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        writeLog("E", tag, fullMessage)
    }

    fun logDownloadStart(userName: String) {
        i("Download", "开始下载用户: @$userName")
    }

    fun logDownloadProgress(fileName: String, current: Int) {
        d("Download", "下载文件: $fileName (已完成: $current)")
    }

    fun logDownloadComplete(userName: String, count: Int) {
        i("Download", "下载完成: @$userName, 共 $count 个文件")
    }

    fun logDownloadError(userName: String, error: String) {
        e("Download", "下载失败: @$userName - $error")
    }

    fun logApiError(endpoint: String, code: Int, message: String) {
        e("API", "请求失败: $endpoint (HTTP $code) - $message")
    }

    fun logUserAdded(userName: String) {
        i("User", "添加用户成功: @$userName")
    }

    fun logUserDeleted(userName: String) {
        i("User", "删除用户: @$userName")
    }

    fun getLogs(): List<String> {
        return try {
            logFile?.readLines() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearLogs() {
        try {
            logFile?.delete()
            isInitialized = false
            init(logFile?.parentFile?.parentFile ?: return)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }

    fun getLogFile(): File? = logFile
}
