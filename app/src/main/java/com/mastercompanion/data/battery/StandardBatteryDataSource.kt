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
                val voltageVolts = if (voltageMv in 2500..5500) voltageMv / 1000f else 4.15f
                val temperatureC = tempTenthsC / 10f

                val status = when (statusRaw) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> ChargingStatus.CHARGING
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingStatus.DISCHARGING
                    BatteryManager.BATTERY_STATUS_FULL -> ChargingStatus.IDLE
                    else -> ChargingStatus.UNKNOWN
                }

                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val rawCurrentMicroAmps = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
                val currentMagnitude = if (rawCurrentMicroAmps != Int.MIN_VALUE && rawCurrentMicroAmps != 0) {
                    kotlin.math.abs(rawCurrentMicroAmps) / 1_000_000f
                } else {
                    val avgCurrent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) ?: 0
                    if (avgCurrent != Int.MIN_VALUE && avgCurrent != 0) kotlin.math.abs(avgCurrent) / 1_000_000f else (if (status == ChargingStatus.CHARGING) 1.2f else 0.35f)
                }

                val signedCurrent = when (status) {
                    ChargingStatus.CHARGING -> currentMagnitude
                    ChargingStatus.DISCHARGING -> -currentMagnitude
                    ChargingStatus.IDLE -> 0.0f
                    else -> currentMagnitude
                }

                val wattage = kotlin.math.abs(voltageVolts * signedCurrent)

                trySend(
                    BatteryData(
                        percentage = percentage,
                        voltageVolts = voltageVolts,
                        currentAmperes = signedCurrent,
                        wattage = wattage,
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
