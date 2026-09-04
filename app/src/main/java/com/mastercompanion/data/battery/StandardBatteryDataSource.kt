package com.mastercompanion.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.mastercompanion.domain.model.BatteryData
import com.mastercompanion.domain.model.ChargingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandardBatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : BatteryDataSource {

    override suspend fun isOperational(): Boolean = true

    override fun getBatteryDataFlow(): Flow<BatteryData> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val statusRaw = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)
                val tempTenthsC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300)

                val percentage = if (level != -1 && scale != -1) (level * 100 / scale) else 50
                val voltageVolts = voltageMv / 1000f
                val temperatureC = tempTenthsC / 10f

                val status = when (statusRaw) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> ChargingStatus.CHARGING
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingStatus.DISCHARGING
                    BatteryManager.BATTERY_STATUS_FULL -> ChargingStatus.IDLE
                    else -> ChargingStatus.UNKNOWN
                }

                trySend(
                    BatteryData(
                        percentage = percentage,
                        voltageVolts = voltageVolts,
                        currentAmperes = if (status == ChargingStatus.CHARGING) 1.5f else 0.4f,
                        wattage = voltageVolts * (if (status == ChargingStatus.CHARGING) 1.5f else 0.4f),
                        temperatureCelsius = temperatureC,
                        status = status,
                        isChargeLimitActive = false,
                        isRootControlled = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    override suspend fun setChargingEnabled(enabled: Boolean): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Charge control requires root permissions (su)"))
    }
}
