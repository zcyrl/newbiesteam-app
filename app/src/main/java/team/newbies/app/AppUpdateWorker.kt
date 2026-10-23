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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * WorkManager background worker that checks for new official NewbiesTeam APP
 * releases (new APK version published on GitHub / data.js appReleases).
 *
 * Runs every ~3-4 hours (4h period with 3h flex window, registered in
 * NewbiesApp). Zero external push servers: the OS schedules the check.
 *
 * - Primary lookup: GitHub Releases API (repo zcyrl/newbiesteam-app).
 *   Plain HTTPS JSON, no Play Services required.
 * - Fallback: data.js appReleases from the multi-backend mirror chain.
 *
 * A notification fires only when the live version is NEWER than this build
 * and differs from the last version already announced (no re-nagging).
 */
class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "newbiesteam_app_updates"
        const val NOTIFICATION_ID = 4721
        const val PREFS_NAME = "newbiesteam_prefs"
        const val KEY_LAST_NOTIFIED = "last_notified_release_version"

        private const val GITHUB_LATEST =
            "https://api.github.com/repos/zcyrl/newbiesteam-app/releases/latest"

        private val DATA_JS_MIRRORS = listOf(
            "https://newbiesteam.pages.dev/data.js",
            "https://newbiesteam.surge.sh/data.js",
            "https://newbiesbase.surge.sh/data.js"
        )
    }

    data class ReleaseInfo(val version: String, val releaseUrl: String, val summary: String)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val latest = fetchLatestRelease() ?: return@withContext Result.retry()
            val installed = BuildConfig.VERSION_NAME.trimStart('v')

            if (isNewer(latest.version, installed)) {
                val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (prefs.getString(KEY_LAST_NOTIFIED, null) != latest.version) {
                    sendNotification(latest)
                    prefs.edit().putString(KEY_LAST_NOTIFIED, latest.version).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * 1) GitHub Releases API -> tag_name ("v3.1.3") + html_url + short body line.
     * 2) Fallback: data.js first appReleases entry with isLatest: true.
     */
    private fun fetchLatestRelease(): ReleaseInfo? {
        try {
            httpGet("$GITHUB_LATEST?t=${System.currentTimeMillis()}",
                headers = mapOf("Accept" to "application/vnd.github+json"))?.let { json ->
                val tag = Regex("\"tag_name\"\\s*:\\s*\"v?([0-9][0-9.]*)\"").find(json)?.groupValues?.get(1)
                val url = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
                val body = Regex("\"body\"\\s*:\\s*\"([^\"]{10,160})").find(json)?.groupValues?.get(1)
                if (!tag.isNullOrBlank()) {
                    return ReleaseInfo(tag, url ?: GITHUB_LATEST, body ?: "Tap to view changelog and update.")
                }
            }
        } catch (_: Exception) {}

        for (mirror in DATA_JS_MIRRORS) {
            try {
                val js = httpGet("$mirror?nc=${System.currentTimeMillis()}") ?: continue
                // first appReleases block carrying isLatest: true
                Regex("version:\\s*\"v?([0-9][0-9.]*)\"[^}]{0,600}?isLatest:\\s*true").find(js)
                    ?.let { m ->
                        val ver = m.groupValues[1]
                        val releaseUrl = Regex("releaseUrl:\\s*\"[^\"]*/tag/([^\"]+)\"").find(js)
                            ?.groupValues?.get(1)
                        val summary = Regex("summary:\\s*\"([^\"]{20,180})").find(js)?.groupValues?.get(1)
                        val url = if (releaseUrl != null)
                            "https://github.com/zcyrl/newbiesteam-app/releases/tag/$releaseUrl"
                        else
                            "https://newbiesteam.surge.sh/#app"
                        return ReleaseInfo(ver, url, summary ?: "Tap to view changelog and update.")
                    }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun httpGet(urlStr: String, headers: Map<String, String> = emptyMap()): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                instanceFollowRedirects = false
                for ((k, v) in headers) setRequestProperty(k, v)
                setRequestProperty("User-Agent", "NewbiesTeam-App/1.0")
            }
            val code = connection.responseCode
            if (code !in 200..299) return null
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun sendNotification(latest: ReleaseInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("target_url", "https://newbiesteam.surge.sh/#app")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("NewbiesTeam v${latest.version} is available")
            .setContentText(latest.summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(latest.summary))
            .setColor(Color.parseColor("#d97757"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    /** Numeric dot-version compare ("3.10.2" > "3.9.1"). Non-numeric parts compare lexically. */
    fun isNewer(candidate: String, installed: String): Boolean {
        val c = candidate.trimStart('v', 'v').split(".").map { it.toIntOrNull() ?: 0 }
        val i = installed.trimStart('v', 'v').split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(c.size, i.size)
        for (n in 0 until len) {
            val cv = c.getOrElse(n) { 0 }
            val iv = i.getOrElse(n) { 0 }
            if (cv > iv) return true
            if (cv < iv) return false
        }
        return false
    }
}
