package com.twitter.downloader.data.remote

import kotlinx.serialization.json.Json
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

    suspend fun getUserInfo(screenName: String, cookie: String): UserInfo? {
        val csrfToken = extractCsrfToken(cookie)
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
            val body = response.body?.string() ?: return null

            val data = json.parseToJsonElement(body).jsonObject
            val user = data["data"]!!.jsonObject["user"]!!.jsonObject["result"]!!.jsonObject
            val legacy = user["legacy"]!!.jsonObject

            UserInfo(
                restId = user["rest_id"]!!.jsonPrimitive.content,
                screenName = screenName,
                name = legacy["name"]!!.jsonPrimitive.content,
                statusesCount = legacy["statuses_count"]!!.jsonPrimitive.content.toIntOrNull() ?: 0,
                mediaCount = legacy["media_count"]!!.jsonPrimitive.content.toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUserMedia(
        userId: String,
        cursor: String?,
        cookie: String
    ): MediaResponse? {
        val csrfToken = extractCsrfToken(cookie)
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
            val body = response.body?.string() ?: return null

            if (body.contains("Rate limit exceeded")) {
                return MediaResponse.Error("API次数已超限")
            }

            parseMediaResponse(body)
        } catch (e: Exception) {
            e.printStackTrace()
            MediaResponse.Error(e.message ?: "Unknown error")
        }
    }

    private fun parseMediaResponse(body: String): MediaResponse {
        val data = json.parseToJsonElement(body).jsonObject
        val instructions = data["data"]!!.jsonObject["user"]!!.jsonObject["result"]!!
            .jsonObject["timeline_v2"]!!.jsonObject["timeline"]!!
            .jsonObject["instructions"]!!.jsonArray

        val entries = instructions.last().jsonObject["entries"]!!.jsonArray

        val mediaList = mutableListOf<MediaItem>()
        var nextCursor: String? = null

        for (entry in entries) {
            val entryObj = entry.jsonObject
            val entryId = entryObj["entryId"]?.jsonPrimitive?.content ?: continue

            if (entryId.contains("cursor-bottom")) {
                nextCursor = entryObj["content"]!!.jsonObject["value"]?.jsonPrimitive?.content
                continue
            }

            if (!entryId.contains("tweet")) continue
            if (entryId.contains("promoted")) continue

            try {
                val itemContent = entryObj["content"]!!.jsonObject["itemContent"]!!.jsonObject
                val tweetResult = itemContent["tweet_results"]!!.jsonObject["result"]!!.jsonObject

                val legacy = if (tweetResult.containsKey("tweet")) {
                    tweetResult["tweet"]!!.jsonObject["legacy"]!!.jsonObject
                } else {
                    tweetResult["legacy"]!!.jsonObject
                }

                val editControl = if (tweetResult.containsKey("tweet")) {
                    tweetResult["tweet"]!!.jsonObject["edit_control"]!!.jsonObject
                } else {
                    tweetResult["edit_control"]!!.jsonObject
                }

                val tweetTime = (editControl["editable_until_msecs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L) - 3600000
                val tweetId = legacy["id_str"]?.jsonPrimitive?.content ?: ""
                val tweetContent = legacy["full_text"]?.jsonPrimitive?.content ?: ""
                val favoriteCount = legacy["favorite_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val retweetCount = legacy["retweet_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val replyCount = legacy["reply_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                val userResult = tweetResult["core"]?.jsonObject?.get("user_results")?.jsonObject?.get("result")?.jsonObject
                val userLegacy = userResult?.get("legacy")?.jsonObject
                val userName = userLegacy?.get("name")?.jsonPrimitive?.content ?: ""
                val userScreenName = userLegacy?.get("screen_name")?.jsonPrimitive?.content ?: ""

                if (legacy.containsKey("extended_entities")) {
                    val mediaArray = legacy["extended_entities"]!!.jsonObject["media"]!!.jsonArray

                    for (media in mediaArray) {
                        val mediaObj = media.jsonObject
                        val mediaUrl: String
                        val mediaType: String

                        if (mediaObj.containsKey("video_info")) {
                            mediaUrl = getHighestQualityVideo(mediaObj["video_info"]!!.jsonObject["variants"]!!.jsonArray)
                            mediaType = "video"
                        } else {
                            mediaUrl = mediaObj["media_url_https"]!!.jsonPrimitive.content
                            mediaType = "image"
                        }

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
            } catch (e: Exception) {
                continue
            }
        }

        return MediaResponse.Success(mediaList, nextCursor)
    }

    private fun getHighestQualityVideo(variants: kotlinx.serialization.json.JsonArray): String {
        if (variants.size == 1) {
            return variants[0].jsonObject["url"]?.jsonPrimitive?.content ?: ""
        }

        var maxBitrate = 0L
        var highestUrl = ""

        for (variant in variants) {
            val obj = variant.jsonObject
            val bitrate = obj["bitrate"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            if (bitrate > maxBitrate) {
                maxBitrate = bitrate
                highestUrl = obj["url"]?.jsonPrimitive?.content ?: ""
            }
        }
        return highestUrl
    }

    private fun urlEncode(url: String): String {
        return url.replace("{", "%7B").replace("}", "%7D")
    }

    suspend fun downloadFile(url: String): ByteArray? {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
