package com.seiv.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seiv.app.MainActivity
import com.seiv.app.R
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.data.remote.SupabaseApi

/**
 * Menerima push notification dari Firebase Cloud Messaging (FCM).
 * Server (Edge Function push-notify) mengirim pesan "data" berisi title, body, dan route
 * agar notifikasi selalu ditampilkan oleh aplikasi dengan gaya SEIV dan bisa membuka layar tujuan.
 */
class SeivMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Token baru (mis. aplikasi dipasang ulang): daftarkan ke server bila sudah login
        SupabaseApi.init(applicationContext)
        SeivSync.registerPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: return
        val body = message.data["body"] ?: message.notification?.body.orEmpty()
        show(this, title, body, message.data["route"])
    }

    companion object {
        const val CHANNEL_ID = "seiv_updates"
        const val EXTRA_ROUTE = "seiv_route"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(CHANNEL_ID, "Kabar sesi & pengingat", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Status sesi konseling, pesan konselor, dan pengingat dari SEIV"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun show(context: Context, title: String, body: String, route: String?) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            createChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                route?.let { putExtra(EXTRA_ROUTE, it) }
            }
            val pending = PendingIntent.getActivity(
                context, (route ?: title).hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFF4F7A6F.toInt())
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending)
                .build()
            NotificationManagerCompat.from(context).notify((route ?: title).hashCode(), n)
        }

        /** Ambil token FCM perangkat ini lalu daftarkan ke akun yang sedang login. */
        fun syncToken() {
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { SeivSync.registerPushToken(it) }
            }
        }
    }
}
