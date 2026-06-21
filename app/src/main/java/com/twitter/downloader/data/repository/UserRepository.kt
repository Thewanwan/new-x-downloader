package com.twitter.downloader.data.repository

import com.twitter.downloader.data.local.dao.UserDao
import com.twitter.downloader.data.local.entity.UserEntity
import com.twitter.downloader.data.remote.TwitterApi
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val api: TwitterApi
) {

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun getUserByScreenName(screenName: String): UserEntity? {
        return try {
            userDao.getUserByScreenName(screenName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun addUser(screenName: String, cookie: String): Result<UserEntity> {
        if (screenName.isBlank()) {
            return Result.failure(Exception("用户名不能为空"))
        }
        if (cookie.isBlank()) {
            return Result.failure(Exception("Cookie不能为空，请先在设置中配置"))
        }

        val existingUser = userDao.getUserByScreenName(screenName)
        if (existingUser != null) {
            return Result.failure(Exception("用户 @$screenName 已存在"))
        }

        return try {
            val userInfo = api.getUserInfo(screenName, cookie)
                ?: return Result.failure(Exception("无法获取用户信息，请检查用户名和Cookie"))

            val entity = UserEntity(
                screenName = screenName,
                displayName = userInfo.name,
                restId = userInfo.restId,
                cookie = cookie
            )

            val id = userDao.insertUser(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: UserEntity) {
        try {
            userDao.updateUser(user)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteUser(userId: Long) {
        try {
            userDao.deleteUser(userId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateDownloadStats(userId: Long, count: Int) {
        try {
            userDao.updateDownloadStats(userId, System.currentTimeMillis(), count)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
