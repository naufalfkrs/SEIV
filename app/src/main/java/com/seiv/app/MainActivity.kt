package com.seiv.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.seiv.app.data.SeivStore
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.data.remote.SupabaseApi
import com.seiv.app.push.SeivMessagingService
import com.seiv.app.ui.PushRouter
import com.seiv.app.ui.SeivApp
import com.seiv.app.ui.theme.SeivTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SeivStore.init(applicationContext)
        SupabaseApi.init(applicationContext)
        SeivSync.refreshInBackground()
        SeivMessagingService.createChannel(this)
        if (SeivSync.online && SupabaseApi.hasSession) SeivMessagingService.syncToken()
        askNotificationPermission()
        handleRoute(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SeivTheme { SeivApp() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Kembali dari latar belakang: ambil status terbaru (mis. sesi baru dikonfirmasi admin)
        SeivSync.refreshInBackground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRoute(intent)
        SeivSync.refreshInBackground() // status sesi mungkin baru berubah
    }

    /** Notifikasi yang diketuk membawa rute tujuan (mis. detail sesi). */
    private fun handleRoute(intent: Intent?) {
        intent?.getStringExtra(SeivMessagingService.EXTRA_ROUTE)?.let { PushRouter.pending = it }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
