package com.twitter.downloader.data.repository

import com.twitter.downloader.data.local.dao.UserDao
import com.twitter.downloader.data.local.entity.UserEntity
import com.twitter.downloader.data.remote.TwitterApi
import com.twitter.downloader.data.remote.UserInfo
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val api: TwitterApi
) {

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun getUserByScreenName(screenName: String): UserEntity? =
        userDao.getUserByScreenName(screenName)

    suspend fun addUser(screenName: String, cookie: String): Result<UserEntity> {
        val userInfo = api.getUserInfo(screenName, cookie)
            ?: return Result.failure(Exception("无法获取用户信息，请检查用户名和Cookie"))

        val entity = UserEntity(
            screenName = screenName,
            displayName = userInfo.name,
            restId = userInfo.restId,
            cookie = cookie
        )

        val id = userDao.insertUser(entity)
        return Result.success(entity.copy(id = id))
    }

    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    suspend fun deleteUser(userId: Long) = userDao.deleteUser(userId)

    suspend fun updateDownloadStats(userId: Long, count: Int) {
        userDao.updateDownloadStats(userId, System.currentTimeMillis(), count)
    }
}
