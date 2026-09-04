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
import com.mastercompanion.data.battery.BatteryRepository
import com.mastercompanion.domain.model.ChargingStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BatteryGuardService : Service() {

    @Inject
    lateinit var batteryRepository: BatteryRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Timber.i("BatteryGuardService created")
        startForeground(NOTIFICATION_ID, buildNotification("Battery Guard & Wattage Monitor Active"))
        observeBatteryData()
    }

    private fun observeBatteryData() {
        serviceScope.launch {
            batteryRepository.batteryData.collect { data ->
                val statusDesc = when (data.status) {
                    ChargingStatus.LIMIT_ENGAGED -> "80% Limit (AC Bypass)"
                    ChargingStatus.CHARGING -> "Charging (${String.format("%.1f", data.wattage)} W)"
                    ChargingStatus.DISCHARGING -> "Discharging (${String.format("%.1f", data.wattage)} W)"
                    ChargingStatus.IDLE -> "Idle (${data.percentage}%)"
                    ChargingStatus.UNKNOWN -> "${data.percentage}%"
                }
                val notification = buildNotification("${data.percentage}% • $statusDesc")
                val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("BatteryGuardService started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Timber.i("BatteryGuardService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MasterCompanionApp.CHANNEL_BATTERY_GUARD)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
