package team.newbies.core.parser

import team.newbies.core.model.UpdatePost
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Feed parser capable of ingesting structured JSON feeds as well as legacy JS objects.
 * Designed to be modular for KMP serialization migration.
 */
object FeedParser {

    fun parseJsonFeed(jsonString: String): List<UpdatePost> {
        val list = mutableListOf<UpdatePost>()
        try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", "")
                    if (id.isNotEmpty()) {
                        list.add(
                            UpdatePost(
                                id = id,
                                title = obj.optString("title", "NewbiesTeam Update"),
                                author = obj.optString("by", "NewbiesTeam"),
                                tag = obj.optString("tag", "Update"),
                                body = obj.optString("body", ""),
                                date = obj.optString("date", ""),
                                targetUrl = obj.optString("url", "")
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseDataJsFallback(jsContent: String): UpdatePost? {
        return try {
            val idPattern = Pattern.compile("id\\s*:\\s*[\"']([^\"']+)[\"']")
            val titlePattern = Pattern.compile("title\\s*:\\s*[\"']([^\"']+)[\"']")
            val byPattern = Pattern.compile("by\\s*:\\s*[\"']([^\"']+)[\"']")
            val tagPattern = Pattern.compile("tag\\s*:\\s*[\"']([^\"']+)[\"']")

            val idMatcher = idPattern.matcher(jsContent)
            val titleMatcher = titlePattern.matcher(jsContent)
            val byMatcher = byPattern.matcher(jsContent)
            val tagMatcher = tagPattern.matcher(jsContent)

            if (idMatcher.find() && titleMatcher.find()) {
                val id = idMatcher.group(1) ?: ""
                val title = titleMatcher.group(1) ?: "New Release Available"
                val author = if (byMatcher.find()) byMatcher.group(1) ?: "NewbiesTeam" else "NewbiesTeam"
                val tag = if (tagMatcher.find()) tagMatcher.group(1) ?: "Update" else "Update"

                UpdatePost(
                    id = id,
                    title = title,
                    author = author,
                    tag = tag
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
