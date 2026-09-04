package com.mastercompanion.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mastercompanion.MainActivity
import com.mastercompanion.MasterCompanionApp
import com.mastercompanion.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

import com.mastercompanion.server.CommandBridgeServer
import javax.inject.Inject

@AndroidEntryPoint
class CommandBridgeService : Service() {

    @Inject
    lateinit var server: CommandBridgeServer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Timber.i("CommandBridgeService created")
        startForeground(NOTIFICATION_ID, buildNotification())
        server.start(webPort = 8060, bridgePort = 8420)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVER -> {
                Timber.i("CommandBridgeService received STOP_SERVER action")
                server.stop()
            }
            ACTION_START_SERVER -> {
                Timber.i("CommandBridgeService received START_SERVER action")
                server.start(webPort = 8060, bridgePort = 8420)
            }
            else -> {
                Timber.i("CommandBridgeService started (Ktor HTTP bridge & Web dashboard)")
                if (!server.isRunning.value) {
                    server.start(webPort = 8060, bridgePort = 8420)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        serviceScope.cancel()
        Timber.i("CommandBridgeService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MasterCompanionApp.CHANNEL_COMMAND_BRIDGE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Bridge Active: Web (:8060) & Commands (:8420)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START_SERVER = "com.mastercompanion.action.START_SERVER"
        const val ACTION_STOP_SERVER = "com.mastercompanion.action.STOP_SERVER"
        private const val NOTIFICATION_ID = 1002
    }
}
