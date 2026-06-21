package com.twitter.downloader.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TwitterApi {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val bearerToken = "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA"

    private fun buildHeaders(cookie: String, csrfToken: String, referer: String): Map<String, String> {
        return mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Authorization" to "Bearer $bearerToken",
            "Cookie" to cookie,
            "X-CSRF-Token" to csrfToken,
            "Referer" to referer
        )
    }

    private fun extractCsrfToken(cookie: String): String {
        val regex = Regex("ct0=(.*?);")
        return regex.find(cookie)?.groupValues?.get(1) ?: ""
    }

    private fun getJsonString(obj: kotlinx.serialization.json.JsonObject?, key: String): String {
        return try {
            obj?.get(key)?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getJsonLong(obj: kotlinx.serialization.json.JsonObject?, key: String): Long {
        return try {
            obj?.get(key)?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getJsonInt(obj: kotlinx.serialization.json.JsonObject?, key: String): Int {
        return try {
            obj?.get(key)?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getUserInfo(screenName: String, cookie: String): UserInfo? {
        val csrfToken = extractCsrfToken(cookie)
        if (csrfToken.isEmpty()) return null

        val headers = buildHeaders(cookie, csrfToken, "https://x.com/$screenName")

        val variables = """{"screen_name":"$screenName","withSafetyModeUserFields":false}"""
        val features = """{"hidden_profile_likes_enabled":false,"hidden_profile_subscriptions_enabled":true,"responsive_web_graphql_exclude_directive_enabled":true,"verified_phone_label_enabled":false,"subscriptions_verification_info_verified_since_enabled":true,"highlights_tweets_tab_ui_enabled":true,"creator_subscriptions_tweet_preview_api_enabled":true,"responsive_web_graphql_skip_user_profile_image_extensions_enabled":false,"responsive_web_graphql_timeline_navigation_enabled":true}"""

        val url = "https://twitter.com/i/api/graphql/xc8f1g7BYqr6VTzTbvNlGw/UserByScreenName?variables=$variables&features=$features"

        return try {
            val request = Request.Builder()
                .url(url)
                .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                .get()
                .build()

            val response = client.newCall(request).execute()

            when (response.code) {
                429 -> throw Exception("API次数已超限，请稍后再试")
                401, 403 -> throw Exception("Cookie无效或已过期，请重新获取")
                404 -> throw Exception("用户不存在: @$screenName")
            }

            val body = response.body?.string() ?: return null

            if (body.contains("Rate limit exceeded")) {
                throw Exception("API次数已超限")
            }

            val data = json.parseToJsonElement(body).jsonObject
            val userResult = data["data"]?.jsonObject?.get("user")?.jsonObject?.get("result")?.jsonObject
                ?: return null
            val legacy = userResult["legacy"]?.jsonObject ?: return null

            UserInfo(
                restId = getJsonString(userResult, "rest_id"),
                screenName = screenName,
                name = getJsonString(legacy, "name"),
                statusesCount = getJsonInt(legacy, "statuses_count"),
                mediaCount = getJsonInt(legacy, "media_count")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getUserMedia(
        userId: String,
        cursor: String?,
        cookie: String
    ): MediaResponse {
        val csrfToken = extractCsrfToken(cookie)
        if (csrfToken.isEmpty()) {
            return MediaResponse.Error("Cookie无效，请检查ct0值")
        }

        val headers = buildHeaders(cookie, csrfToken, "https://x.com/")

        val cursorPart = if (cursor != null) ""","cursor":"$cursor",""" else ""
        val variables = """{"userId":"$userId","count":200,$cursorPart"includePromotedContent":false,"withClientEventToken":false,"withBirdwatchNotes":false,"withVoice":true,"withV2Timeline":true}"""
        val features = """{"rweb_video_screen_enabled":false,"profile_label_improvements_pcf_label_in_post_enabled":true,"rweb_tipjar_consumption_enabled":true,"verified_phone_label_enabled":false,"creator_subscriptions_tweet_preview_api_enabled":true,"responsive_web_graphql_timeline_navigation_enabled":true,"responsive_web_graphql_skip_user_profile_image_extensions_enabled":false,"premium_content_api_read_enabled":false,"communities_web_enable_tweet_community_results_fetch":true,"c9s_tweet_anatomy_moderator_badge_enabled":true,"responsive_web_grok_analyze_button_fetch_trends_enabled":false,"responsive_web_grok_analyze_post_followups_enabled":true,"responsive_web_jetfuel_frame":false,"responsive_web_grok_share_attachment_enabled":true,"articles_preview_enabled":true,"responsive_web_edit_tweet_api_enabled":true,"graphql_is_translatable_rweb_tweet_is_translatable_enabled":true,"view_counts_everywhere_api_enabled":true,"longform_notetweets_consumption_enabled":true,"responsive_web_twitter_article_tweet_consumption_enabled":true,"tweet_awards_web_tipping_enabled":false,"responsive_web_grok_show_grok_translated_post":false,"responsive_web_grok_analysis_button_from_backend":false,"creator_subscriptions_quote_tweet_preview_enabled":false,"freedom_of_speech_not_reach_fetch_enabled":true,"standardized_nudges_misinfo":true,"tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled":true,"longform_notetweets_rich_text_read_enabled":true,"longform_notetweets_inline_media_enabled":true,"responsive_web_grok_image_annotation_enabled":true,"responsive_web_enhance_cards_enabled":false}"""

        val url = "https://twitter.com/i/api/graphql/Le6KlbilFmSu-5VltFND-Q/UserMedia?variables=$variables&features=$features"

        return try {
            val request = Request.Builder()
                .url(urlEncode(url))
                .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                .get()
                .build()

            val response = client.newCall(request).execute()

            when (response.code) {
                429 -> return MediaResponse.Error("API次数已超限，请稍后再试")
                401, 403 -> return MediaResponse.Error("Cookie无效或已过期")
            }

            val body = response.body?.string() ?: return MediaResponse.Error("获取数据失败")

            if (body.contains("Rate limit exceeded")) {
                return MediaResponse.Error("API次数已超限")
            }

            parseMediaResponse(body)
        } catch (e: java.net.SocketTimeoutException) {
            MediaResponse.Error("网络超时，请检查网络连接")
        } catch (e: java.net.UnknownHostException) {
            MediaResponse.Error("无法解析主机，请检查网络连接")
        } catch (e: java.net.ConnectException) {
            MediaResponse.Error("连接失败，请检查网络连接")
        } catch (e: Exception) {
            e.printStackTrace()
            MediaResponse.Error(e.message ?: "未知错误")
        }
    }

    private fun parseMediaResponse(body: String): MediaResponse {
        return try {
            val data = json.parseToJsonElement(body).jsonObject
            val instructions = data["data"]?.jsonObject?.get("user")?.jsonObject?.get("result")?.jsonObject
                ?.get("timeline_v2")?.jsonObject?.get("timeline")?.jsonObject
                ?.get("instructions")?.jsonArray
                ?: return MediaResponse.Error("数据解析失败")

            if (instructions.isEmpty()) {
                return MediaResponse.Success(emptyList(), null)
            }

            val entries = instructions.last().jsonObject?.get("entries")?.jsonArray
                ?: return MediaResponse.Success(emptyList(), null)

            val mediaList = mutableListOf<MediaItem>()
            var nextCursor: String? = null

            for (entry in entries) {
                val entryObj = entry.jsonObject
                val entryId = entryObj["entryId"]?.jsonPrimitive?.content ?: continue

                if (entryId.contains("cursor-bottom")) {
                    nextCursor = entryObj["content"]?.jsonObject?.get("value")?.jsonPrimitive?.content
                    continue
                }

                if (!entryId.contains("tweet")) continue
                if (entryId.contains("promoted")) continue

                try {
                    val itemContent = entryObj["content"]?.jsonObject?.get("itemContent")?.jsonObject
                        ?: continue
                    val tweetResult = itemContent["tweet_results"]?.jsonObject?.get("result")?.jsonObject
                        ?: continue

                    val legacy = tweetResult["tweet"]?.jsonObject?.get("legacy")?.jsonObject
                        ?: tweetResult["legacy"]?.jsonObject
                        ?: continue

                    val editControl = tweetResult["tweet"]?.jsonObject?.get("edit_control")?.jsonObject
                        ?: tweetResult["edit_control"]?.jsonObject
                        ?: continue

                    val tweetTime = getJsonLong(editControl, "editable_until_msecs") - 3600000
                    val tweetId = getJsonString(legacy, "id_str")
                    val tweetContent = getJsonString(legacy, "full_text")
                    val favoriteCount = getJsonInt(legacy, "favorite_count")
                    val retweetCount = getJsonInt(legacy, "retweet_count")
                    val replyCount = getJsonInt(legacy, "reply_count")

                    val userResult = tweetResult["core"]?.jsonObject?.get("user_results")?.jsonObject?.get("result")?.jsonObject
                    val userLegacy = userResult?.get("legacy")?.jsonObject
                    val userName = getJsonString(userLegacy, "name")
                    val userScreenName = getJsonString(userLegacy, "screen_name")

                    if (legacy.containsKey("extended_entities")) {
                        val mediaArray = legacy["extended_entities"]?.jsonObject?.get("media")?.jsonArray
                            ?: continue

                        for (media in mediaArray) {
                            val mediaObj = media.jsonObject
                            val mediaUrl: String
                            val mediaType: String

                            if (mediaObj.containsKey("video_info")) {
                                val variants = mediaObj["video_info"]?.jsonObject?.get("variants")?.jsonArray
                                    ?: continue
                                mediaUrl = getHighestQualityVideo(variants)
                                mediaType = "video"
                            } else {
                                mediaUrl = mediaObj["media_url_https"]?.jsonPrimitive?.content ?: continue
                                mediaType = "image"
                            }

                            if (mediaUrl.isNotEmpty()) {
                                mediaList.add(
                                    MediaItem(
                                        mediaUrl = mediaUrl,
                                        mediaType = mediaType,
                                        tweetId = tweetId,
                                        tweetTime = tweetTime,
                                        tweetContent = tweetContent,
                                        userName = userName,
                                        userScreenName = "@$userScreenName",
                                        favoriteCount = favoriteCount,
                                        retweetCount = retweetCount,
                                        replyCount = replyCount
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            MediaResponse.Success(mediaList, nextCursor)
        } catch (e: Exception) {
            e.printStackTrace()
            MediaResponse.Error("数据解析失败: ${e.message}")
        }
    }

    private fun getHighestQualityVideo(variants: JsonArray): String {
        if (variants.isEmpty()) return ""

        if (variants.size == 1) {
            return try {
                variants[0].jsonObject["url"]?.jsonPrimitive?.content ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        var maxBitrate = 0L
        var highestUrl = ""

        for (variant in variants) {
            try {
                val obj = variant.jsonObject
                val bitrate = obj["bitrate"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                if (bitrate > maxBitrate) {
                    maxBitrate = bitrate
                    highestUrl = obj["url"]?.jsonPrimitive?.content ?: ""
                }
            } catch (e: Exception) {
                continue
            }
        }
        return highestUrl
    }

    private fun urlEncode(url: String): String {
        return url.replace("{", "%7B").replace("}", "%7D")
    }

    suspend fun downloadFile(url: String, maxRetries: Int = 3): ByteArray? {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                when (response.code) {
                    200 -> {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            return bytes
                        }
                    }
                    429 -> {
                        val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 30
                        Thread.sleep(retryAfter * 1000)
                        return@repeat
                    }
                    404 -> {
                        return null
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep(1000L * (attempt + 1))
                }
            } catch (e: java.net.UnknownHostException) {
                return null
            } catch (e: java.net.ConnectException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep(2000L * (attempt + 1))
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep(1000L * (attempt + 1))
                }
            }
        }

        lastException?.printStackTrace()
        return null
    }
}

data class UserInfo(
    val restId: String,
    val screenName: String,
    val name: String,
    val statusesCount: Int,
    val mediaCount: Int
)

sealed class MediaResponse {
    data class Success(val items: List<MediaItem>, val nextCursor: String?) : MediaResponse()
    data class Error(val message: String) : MediaResponse()
}

data class MediaItem(
    val mediaUrl: String,
    val mediaType: String,
    val tweetId: String,
    val tweetTime: Long,
    val tweetContent: String,
    val userName: String,
    val userScreenName: String,
    val favoriteCount: Int,
    val retweetCount: Int,
    val replyCount: Int
)
