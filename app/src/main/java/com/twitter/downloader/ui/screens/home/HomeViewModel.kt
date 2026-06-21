package com.twitter.downloader.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twitter.downloader.TwitterDownloaderApp
import com.twitter.downloader.data.remote.TwitterApi
import com.twitter.downloader.data.repository.UserRepository
import com.twitter.downloader.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as TwitterDownloaderApp).database
    private val repository = UserRepository(db.userDao(), TwitterApi())

    val users = repository.getAllUsers()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState

    fun addUser(screenName: String, cookie: String) {
        viewModelScope.launch {
            Logger.i("User", "尝试添加用户: @$screenName")
            _uiState.value = HomeUiState.Loading("正在添加用户...")
            val result = repository.addUser(screenName, cookie)
            result.onSuccess {
                Logger.logUserAdded(screenName)
                _uiState.value = HomeUiState.Success("用户添加成功")
            }.onFailure {
                Logger.e("User", "添加用户失败: ${it.message}")
                _uiState.value = HomeUiState.Error(it.message ?: "添加失败")
            }
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            Logger.i("User", "删除用户: ID=$userId")
            repository.deleteUser(userId)
        }
    }

    fun clearState() {
        _uiState.value = HomeUiState.Idle
    }
}

sealed class HomeUiState {
    data object Idle : HomeUiState()
    data class Loading(val message: String) : HomeUiState()
    data class Success(val message: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
