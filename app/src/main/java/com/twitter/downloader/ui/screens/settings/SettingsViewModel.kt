package com.twitter.downloader.ui.screens.settings

import android.app.Application
import androidx.datastore.preferences.core.edit
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

    private val _cookie = MutableStateFlow("")
    val cookie: StateFlow<String> = _cookie

    private val _savePath = MutableStateFlow("")
    val savePath: StateFlow<String> = _savePath

    init {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _cookie.value = preferences[COOKIE_KEY] ?: ""
                _savePath.value = preferences[SAVE_PATH_KEY] ?: ""
            }
        }
    }

    fun updateCookie(value: String) {
        _cookie.value = value
        viewModelScope.launch {
            dataStore.edit { it[COOKIE_KEY] = value }
        }
    }

    fun updateSavePath(value: String) {
        _savePath.value = value
        viewModelScope.launch {
            dataStore.edit { it[SAVE_PATH_KEY] = value }
        }
    }

    companion object {
        val COOKIE_KEY = stringPreferencesKey("cookie")
        val SAVE_PATH_KEY = stringPreferencesKey("save_path")
    }
}
