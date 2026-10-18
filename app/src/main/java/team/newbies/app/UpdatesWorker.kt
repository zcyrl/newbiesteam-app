package team.newbies.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class UpdatesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "newbiesteam_updates"
        const val NOTIFICATION_ID = 4720
        const val PREFS_NAME = "newbiesteam_prefs"
        const val KEY_LAST_POST_ID = "last_seen_post_id"
        const val FEED_JSON_URL = "https://newbiesteam.surge.sh/updates.json"
        const val FEED_DATA_JS_URL = "https://newbiesteam.surge.sh/data.js"
        const val DEFAULT_TARGET_URL = "https://newbiesteam.surge.sh/#updates"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val latestUpdate = fetchLatestUpdate() ?: return@withContext Result.retry()

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSeenId = prefs.getString(KEY_LAST_POST_ID, null)

            val currentId = latestUpdate.optString("id", "")
            val title = latestUpdate.optString("title", "New Release Available")
            val by = latestUpdate.optString("by", "NewbiesTeam")
            val tag = latestUpdate.optString("tag", "Update")

            if (currentId.isNotEmpty()) {
                if (lastSeenId == null) {
                    // First run: save current without notifying
                    prefs.edit().putString(KEY_LAST_POST_ID, currentId).apply()
                } else if (currentId != lastSeenId) {
                    // New post detected!
                    sendNotification(title, "$tag by $by • Tap to view")
                    prefs.edit().putString(KEY_LAST_POST_ID, currentId).apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun fetchLatestUpdate(): JSONObject? {
        // Try updates.json first
        try {
            val jsonText = httpGet(FEED_JSON_URL)
            if (!jsonText.isNullOrBlank() && jsonText.trim().startsWith("[")) {
                val array = JSONArray(jsonText)
                if (array.length() > 0) {
                    return array.getJSONObject(0)
                }
            }
        } catch (_: Exception) {}

        // Fallback to data.js parsing
        try {
            val jsText = httpGet(FEED_DATA_JS_URL)
            if (!jsText.isNullOrBlank()) {
                val idPattern = Pattern.compile("id\\s*:\\s*[\"']([^\"']+)[\"']")
                val titlePattern = Pattern.compile("title\\s*:\\s*[\"']([^\"']+)[\"']")
                val byPattern = Pattern.compile("by\\s*:\\s*[\"']([^\"']+)[\"']")
                val tagPattern = Pattern.compile("tag\\s*:\\s*[\"']([^\"']+)[\"']")

                val idMatcher = idPattern.matcher(jsText)
                val titleMatcher = titlePattern.matcher(jsText)
                val byMatcher = byPattern.matcher(jsText)
                val tagMatcher = tagPattern.matcher(jsText)

                if (idMatcher.find() && titleMatcher.find()) {
                    val obj = JSONObject()
                    obj.put("id", idMatcher.group(1))
                    obj.put("title", titleMatcher.group(1))
                    obj.put("by", if (byMatcher.find()) byMatcher.group(1) else "NewbiesTeam")
                    obj.put("tag", if (tagMatcher.find()) tagMatcher.group(1) else "Update")
                    return obj
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
                setRequestProperty("User-Agent", "NewbiesTeam-Android-App")
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

    private fun sendNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("target_url", DEFAULT_TARGET_URL)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setColor(Color.parseColor("#d97757"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
