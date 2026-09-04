package com.mastercompanion

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mastercompanion.ui.theme.MasterCompanionTheme
import com.mastercompanion.ui.theme.PureBlack
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while docked on desk
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Setup edge-to-edge immersive landscape view
        enableImmersiveMode()

        // Handle incoming intent (e.g. Spotify OAuth callback deep link)
        handleIntent(intent)

        setContent {
            MasterCompanionTheme {
                DashboardRootScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "mastercompanion" && uri.host == "spotify") {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            Timber.i("Spotify OAuth Callback received. code present: ${code != null}, error: $error")
            // Pass authorization code to SpotifyAuthManager via ViewModel / Event Bus
        }
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun DashboardRootScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Master Companion Standby Dashboard",
            color = Color.White
        )
    }
}
