package com.mastercompanion.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.mastercompanion.MainActivity
import com.mastercompanion.data.prefs.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * BroadcastReceiver listening for device charging & docking events.
 * Automatically turns on the screen and launches Master Companion when power is connected.
 */
@AndroidEntryPoint
class PowerConnectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_POWER_CONNECTED) {
            Timber.i("External power connected / docked. Checking auto-launch preference...")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isAutoLaunchEnabled = preferencesRepository.autoLaunchOnChargingFlow.first()
                    if (isAutoLaunchEnabled) {
                        Timber.i("Auto-launch on charging is enabled. Waking screen and launching MainActivity...")

                        // Wake the screen
                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        @Suppress("DEPRECATION")
                        val wakeLock = powerManager?.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "mastercompanion:charging_dock_wake"
                        )
                        wakeLock?.acquire(3000L)

                        // Launch MainActivity
                        val launchIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        context.startActivity(launchIntent)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to auto-launch on power connected: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
