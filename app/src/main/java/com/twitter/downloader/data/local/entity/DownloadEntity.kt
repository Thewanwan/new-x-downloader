package com.twitter.downloader.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("mediaUrl")]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val mediaUrl: String,
    val mediaType: String,
    val tweetId: String,
    val tweetTime: Long,
    val tweetContent: String = "",
    val filePath: String = "",
    val status: String = STATUS_DOWNLOADED,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_DOWNLOADED = "downloaded"
        const val STATUS_PENDING = "pending"
    }
}
