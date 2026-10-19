package team.newbies.core.repository

import team.newbies.core.model.UpdatePost
import team.newbies.core.parser.FeedParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repository interface & implementation for fetching and synchronizing live NewbiesTeam hub updates.
 * Ready for extraction into KMP commonMain network module.
 */
class UpdatesRepository(
    private val jsonUrl: String = DEFAULT_JSON_FEED,
    private val mirrorJsonUrl: String = MIRROR_JSON_FEED,
    private val fallbackJsUrl: String = DEFAULT_DATA_JS
) {
    companion object {
        const val DEFAULT_JSON_FEED = "https://newbiesteam.surge.sh/updates.json"
        const val MIRROR_JSON_FEED = "https://newbiesbase.surge.sh/updates.json"
        const val DEFAULT_DATA_JS = "https://newbiesteam.surge.sh/data.js"
    }

    fun fetchLatestUpdate(): UpdatePost? {
        val timestamp = System.currentTimeMillis()

        // 1. Primary: JSON Endpoint with cache-busting
        try {
            val jsonText = httpGet("$jsonUrl?t=$timestamp")
            if (!jsonText.isNullOrBlank()) {
                val posts = FeedParser.parseJsonFeed(jsonText)
                if (posts.isNotEmpty()) {
                    return posts.first()
                }
            }
        } catch (_: Exception) {}

        // 2. Mirror: JSON Endpoint with cache-busting
        try {
            val mirrorText = httpGet("$mirrorJsonUrl?t=$timestamp")
            if (!mirrorText.isNullOrBlank()) {
                val posts = FeedParser.parseJsonFeed(mirrorText)
                if (posts.isNotEmpty()) {
                    return posts.first()
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: data.js Regex with cache-busting
        try {
            val jsText = httpGet("$fallbackJsUrl?t=$timestamp")
            if (!jsText.isNullOrBlank()) {
                val fallbackPost = FeedParser.parseDataJsFallback(jsText)
                if (fallbackPost != null) {
                    return fallbackPost
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun httpGet(urlStr: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("User-Agent", "NewbiesTeam-Android/1.2.3")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            if (connection.responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                null
            }
        } finally {
            connection?.disconnect()
        }
    }
}
