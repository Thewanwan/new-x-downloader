package com.twitter.downloader.data.repository

import com.twitter.downloader.data.local.dao.UserDao
import com.twitter.downloader.data.local.entity.UserEntity
import com.twitter.downloader.data.remote.TwitterApi
import com.twitter.downloader.util.Logger
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
            Logger.e("UserRepo", "查询用户失败", e)
            null
        }
    }

    suspend fun addUser(screenName: String, cookie: String): Result<UserEntity> {
        Logger.i("UserRepo", "添加用户: @$screenName")
        Logger.d("UserRepo", "Cookie长度: ${cookie.length}")

        if (screenName.isBlank()) {
            Logger.w("UserRepo", "用户名为空")
            return Result.failure(Exception("用户名不能为空"))
        }
        if (cookie.isBlank()) {
            Logger.w("UserRepo", "Cookie为空")
            return Result.failure(Exception("Cookie不能为空，请先在设置中配置"))
        }

        if (!cookie.contains("auth_token=") || !cookie.contains("ct0=")) {
            Logger.w("UserRepo", "Cookie格式错误: ${cookie.take(50)}...")
            return Result.failure(Exception("Cookie格式错误，请检查auth_token和ct0"))
        }

        val existingUser = userDao.getUserByScreenName(screenName)
        if (existingUser != null) {
            Logger.w("UserRepo", "用户已存在: @$screenName")
            return Result.failure(Exception("用户 @$screenName 已存在"))
        }

        return try {
            Logger.i("UserRepo", "正在获取用户信息...")
            val userInfo = api.getUserInfo(screenName, cookie)

            if (userInfo == null) {
                Logger.e("UserRepo", "获取用户信息返回null")
                return Result.failure(Exception("无法获取用户信息，请检查用户名和Cookie是否正确"))
            }

            Logger.i("UserRepo", "获取成功: ${userInfo.name} (ID: ${userInfo.restId})")

            val entity = UserEntity(
                screenName = screenName,
                displayName = userInfo.name,
                restId = userInfo.restId,
                cookie = cookie
            )

            val id = userDao.insertUser(entity)
            Logger.i("UserRepo", "用户添加成功: @$screenName (DB ID: $id)")
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Logger.e("UserRepo", "添加用户异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: UserEntity) {
        try {
            userDao.updateUser(user)
        } catch (e: Exception) {
            Logger.e("UserRepo", "更新用户失败", e)
        }
    }

    suspend fun deleteUser(userId: Long) {
        try {
            Logger.i("UserRepo", "删除用户: ID=$userId")
            userDao.deleteUser(userId)
        } catch (e: Exception) {
            Logger.e("UserRepo", "删除用户失败", e)
        }
    }

    suspend fun updateDownloadStats(userId: Long, count: Int) {
        try {
            userDao.updateDownloadStats(userId, System.currentTimeMillis(), count)
        } catch (e: Exception) {
            Logger.e("UserRepo", "更新下载统计失败", e)
        }
    }
}
