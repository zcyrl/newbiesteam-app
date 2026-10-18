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
import team.newbies.core.model.UpdatePost
import team.newbies.core.repository.UpdatesRepository
import team.newbies.core.router.UrlRouter

/**
 * Android WorkManager background worker backed by KMP-ready UpdatesRepository.
 */
class UpdatesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "newbiesteam_updates"
        const val NOTIFICATION_ID = 4720
        const val PREFS_NAME = "newbiesteam_prefs"
        const val KEY_LAST_POST_ID = "last_seen_post_id"
    }

    private val repository = UpdatesRepository()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val latestUpdate: UpdatePost = repository.fetchLatestUpdate() ?: return@withContext Result.retry()

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSeenId = prefs.getString(KEY_LAST_POST_ID, null)
            val currentId = latestUpdate.id

            if (currentId.isNotEmpty()) {
                if (lastSeenId == null) {
                    // First run: save current without notifying
                    prefs.edit().putString(KEY_LAST_POST_ID, currentId).apply()
                } else if (currentId != lastSeenId) {
                    // New post detected!
                    sendNotification(latestUpdate)
                    prefs.edit().putString(KEY_LAST_POST_ID, currentId).apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendNotification(post: UpdatePost) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val destinationUrl = if (post.targetUrl.isNotBlank()) post.targetUrl else UrlRouter.UPDATES_SECTION_URL

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("target_url", destinationUrl)
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
            .setContentTitle(post.notificationTitle)
            .setContentText(post.notificationBody)
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
