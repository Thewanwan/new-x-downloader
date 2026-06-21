package com.twitter.downloader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenName: String,
    val displayName: String = "",
    val restId: String = "",
    val cookie: String = "",
    val lastDownloadTime: Long = 0,
    val totalCount: Int = 0
)
