package com.seiv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.seiv.app.data.SeivStore
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.data.remote.SupabaseApi
import com.seiv.app.ui.SeivApp
import com.seiv.app.ui.theme.SeivTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SeivStore.init(applicationContext)
        SupabaseApi.init(applicationContext)
        SeivSync.refreshInBackground()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SeivTheme { SeivApp() }
        }
    }
}
