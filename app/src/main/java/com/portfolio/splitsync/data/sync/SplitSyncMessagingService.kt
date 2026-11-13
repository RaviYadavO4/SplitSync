package com.portfolio.splitsync.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.portfolio.splitsync.R
import com.portfolio.splitsync.data.repository.ExpenseRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplitSyncMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var repository: ExpenseRepositoryImpl

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Silent data payload aane par background sync trigger karein
        repository.triggerBackgroundSync()

        val title = message.notification?.title ?: "New Group Expense"
        val body = message.notification?.body ?: "Someone added an expense in your group."
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "splitsync_expenses_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Expense Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
