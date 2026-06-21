package com.twitter.downloader.ui.screens.settings

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val Application.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    // Cookie
    private val _authToken = MutableStateFlow("")
    val authToken: StateFlow<String> = _authToken

    private val _ct0 = MutableStateFlow("")
    val ct0: StateFlow<String> = _ct0

    // Save path
    private val _savePath = MutableStateFlow("")
    val savePath: StateFlow<String> = _savePath

    // Download options
    private val _hasRetweet = MutableStateFlow(false)
    val hasRetweet: StateFlow<Boolean> = _hasRetweet

    private val _highLights = MutableStateFlow(false)
    val highLights: StateFlow<Boolean> = _highLights

    private val _likes = MutableStateFlow(false)
    val likes: StateFlow<Boolean> = _likes

    private val _timeRange = MutableStateFlow("1990-01-01:2030-01-01")
    val timeRange: StateFlow<String> = _timeRange

    private val _downLog = MutableStateFlow(false)
    val downLog: StateFlow<Boolean> = _downLog

    private val _autoSync = MutableStateFlow(false)
    val autoSync: StateFlow<Boolean> = _autoSync

    private val _imageFormat = MutableStateFlow("orig")
    val imageFormat: StateFlow<String> = _imageFormat

    private val _hasVideo = MutableStateFlow(true)
    val hasVideo: StateFlow<Boolean> = _hasVideo

    private val _logOutput = MutableStateFlow(false)
    val logOutput: StateFlow<Boolean> = _logOutput

    private val _maxConcurrentRequests = MutableStateFlow(8)
    val maxConcurrentRequests: StateFlow<Int> = _maxConcurrentRequests

    private val _proxy = MutableStateFlow("")
    val proxy: StateFlow<String> = _proxy

    private val _mdOutput = MutableStateFlow(false)
    val mdOutput: StateFlow<Boolean> = _mdOutput

    private val _mediaCountLimit = MutableStateFlow(350)
    val mediaCountLimit: StateFlow<Int> = _mediaCountLimit

    init {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _authToken.value = preferences[AUTH_TOKEN_KEY] ?: ""
                _ct0.value = preferences[CT0_KEY] ?: ""
                _savePath.value = preferences[SAVE_PATH_KEY] ?: ""
                _hasRetweet.value = preferences[HAS_RETWEET_KEY] ?: false
                _highLights.value = preferences[HIGHLIGHTS_KEY] ?: false
                _likes.value = preferences[LIKES_KEY] ?: false
                _timeRange.value = preferences[TIME_RANGE_KEY] ?: "1990-01-01:2030-01-01"
                _downLog.value = preferences[DOWN_LOG_KEY] ?: false
                _autoSync.value = preferences[AUTO_SYNC_KEY] ?: false
                _imageFormat.value = preferences[IMAGE_FORMAT_KEY] ?: "orig"
                _hasVideo.value = preferences[HAS_VIDEO_KEY] ?: true
                _logOutput.value = preferences[LOG_OUTPUT_KEY] ?: false
                _maxConcurrentRequests.value = preferences[MAX_CONCURRENT_KEY] ?: 8
                _proxy.value = preferences[PROXY_KEY] ?: ""
                _mdOutput.value = preferences[MD_OUTPUT_KEY] ?: false
                _mediaCountLimit.value = preferences[MEDIA_COUNT_LIMIT_KEY] ?: 350
            }
        }
    }

    fun getCookieString(): String = "auth_token=${_authToken.value}; ct0=${_ct0.value};"

    fun updateAuthToken(value: String) {
        _authToken.value = value
        viewModelScope.launch { dataStore.edit { it[AUTH_TOKEN_KEY] = value } }
    }

    fun updateCt0(value: String) {
        _ct0.value = value
        viewModelScope.launch { dataStore.edit { it[CT0_KEY] = value } }
    }

    fun updateSavePath(value: String) {
        _savePath.value = value
        viewModelScope.launch { dataStore.edit { it[SAVE_PATH_KEY] = value } }
    }

    fun updateHasRetweet(value: Boolean) {
        _hasRetweet.value = value
        viewModelScope.launch { dataStore.edit { it[HAS_RETWEET_KEY] = value } }
    }

    fun updateHighLights(value: Boolean) {
        _highLights.value = value
        viewModelScope.launch { dataStore.edit { it[HIGHLIGHTS_KEY] = value } }
    }

    fun updateLikes(value: Boolean) {
        _likes.value = value
        viewModelScope.launch { dataStore.edit { it[LIKES_KEY] = value } }
    }

    fun updateTimeRange(value: String) {
        _timeRange.value = value
        viewModelScope.launch { dataStore.edit { it[TIME_RANGE_KEY] = value } }
    }

    fun updateDownLog(value: Boolean) {
        _downLog.value = value
        viewModelScope.launch { dataStore.edit { it[DOWN_LOG_KEY] = value } }
    }

    fun updateAutoSync(value: Boolean) {
        _autoSync.value = value
        viewModelScope.launch { dataStore.edit { it[AUTO_SYNC_KEY] = value } }
    }

    fun updateImageFormat(value: String) {
        _imageFormat.value = value
        viewModelScope.launch { dataStore.edit { it[IMAGE_FORMAT_KEY] = value } }
    }

    fun updateHasVideo(value: Boolean) {
        _hasVideo.value = value
        viewModelScope.launch { dataStore.edit { it[HAS_VIDEO_KEY] = value } }
    }

    fun updateLogOutput(value: Boolean) {
        _logOutput.value = value
        viewModelScope.launch { dataStore.edit { it[LOG_OUTPUT_KEY] = value } }
    }

    fun updateMaxConcurrentRequests(value: Int) {
        _maxConcurrentRequests.value = value
        viewModelScope.launch { dataStore.edit { it[MAX_CONCURRENT_KEY] = value } }
    }

    fun updateProxy(value: String) {
        _proxy.value = value
        viewModelScope.launch { dataStore.edit { it[PROXY_KEY] = value } }
    }

    fun updateMdOutput(value: Boolean) {
        _mdOutput.value = value
        viewModelScope.launch { dataStore.edit { it[MD_OUTPUT_KEY] = value } }
    }

    fun updateMediaCountLimit(value: Int) {
        _mediaCountLimit.value = value
        viewModelScope.launch { dataStore.edit { it[MEDIA_COUNT_LIMIT_KEY] = value } }
    }

    companion object {
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val CT0_KEY = stringPreferencesKey("ct0")
        val SAVE_PATH_KEY = stringPreferencesKey("save_path")
        val HAS_RETWEET_KEY = booleanPreferencesKey("has_retweet")
        val HIGHLIGHTS_KEY = booleanPreferencesKey("high_lights")
        val LIKES_KEY = booleanPreferencesKey("likes")
        val TIME_RANGE_KEY = stringPreferencesKey("time_range")
        val DOWN_LOG_KEY = booleanPreferencesKey("down_log")
        val AUTO_SYNC_KEY = booleanPreferencesKey("auto_sync")
        val IMAGE_FORMAT_KEY = stringPreferencesKey("image_format")
        val HAS_VIDEO_KEY = booleanPreferencesKey("has_video")
        val LOG_OUTPUT_KEY = booleanPreferencesKey("log_output")
        val MAX_CONCURRENT_KEY = intPreferencesKey("max_concurrent_requests")
        val PROXY_KEY = stringPreferencesKey("proxy")
        val MD_OUTPUT_KEY = booleanPreferencesKey("md_output")
        val MEDIA_COUNT_LIMIT_KEY = intPreferencesKey("media_count_limit")
    }
}
