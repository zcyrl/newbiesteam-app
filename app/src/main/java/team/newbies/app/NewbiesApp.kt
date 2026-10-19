package team.newbies.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.color.DynamicColors
import java.util.concurrent.TimeUnit

class NewbiesApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply Material 3 Adaptive Dynamic Colors if available on device
        DynamicColors.applyToActivitiesIfAvailable(this)

        createNotificationChannel()
        scheduleUpdatesWorker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(UpdatesWorker.CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.parseColor("#d97757")
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleUpdatesWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 15-minute periodic background check
        val periodicRequest = PeriodicWorkRequestBuilder<UpdatesWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NewbiesTeamUpdatesSync",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )

        // Immediate one-time background check on application startup
        val immediateRequest = OneTimeWorkRequestBuilder<UpdatesWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueue(immediateRequest)
    }
}
