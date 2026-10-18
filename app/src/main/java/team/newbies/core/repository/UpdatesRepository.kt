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
    private val fallbackJsUrl: String = DEFAULT_DATA_JS
) {
    companion object {
        const val DEFAULT_JSON_FEED = "https://newbiesteam.surge.sh/updates.json"
        const val DEFAULT_DATA_JS = "https://newbiesteam.surge.sh/data.js"
    }

    fun fetchLatestUpdate(): UpdatePost? {
        // 1. Primary: JSON Endpoint
        try {
            val jsonText = httpGet(jsonUrl)
            if (!jsonText.isNullOrBlank()) {
                val posts = FeedParser.parseJsonFeed(jsonText)
                if (posts.isNotEmpty()) {
                    return posts.first()
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: data.js Regex
        try {
            val jsText = httpGet(fallbackJsUrl)
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
                setRequestProperty("User-Agent", "NewbiesTeam-Android/1.2.1 (KMP-Ready)")
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
