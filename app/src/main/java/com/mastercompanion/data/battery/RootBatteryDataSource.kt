package com.mastercompanion.data.battery

import android.content.Context
import android.os.BatteryManager
import com.mastercompanion.di.IoDispatcher
import com.mastercompanion.domain.model.BatteryData
import com.mastercompanion.domain.model.ChargingStatus
import com.mastercompanion.platform.DeviceCompat
import com.mastercompanion.platform.root.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class RootBatteryDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootShell: RootShell,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BatteryDataSource {

    private var resolvedVoltagePath: String? = null
    private var resolvedCurrentPath: String? = null
    private var resolvedControlPath: String? = null
    private var resolvedStatusPath: String? = null

    private var isChargeLimitEngaged = false

    override suspend fun isOperational(): Boolean {
        return rootShell.isRootAvailable()
    }

    override fun getBatteryDataFlow(): Flow<BatteryData> = flow {
        resolveSysfsPaths()
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        while (true) {
            val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            // Read Voltage (microvolts to Volts)
            val voltageVolts = readSysfsFloat(resolvedVoltagePath) { raw ->
                if (raw > 100_000) raw / 1_000_000f else raw / 1_000f
            } ?: 4.2f

            // Read Current (microamperes to Amperes)
            val currentAmps = readSysfsFloat(resolvedCurrentPath) { raw ->
                if (abs(raw) > 10_000) raw / 1_000_000f else raw / 1_000f
            } ?: 0.5f

            // Calculate live Wattage
            val wattage = abs(voltageVolts * currentAmps)

            // Status string
            val rawStatus = resolvedStatusPath?.let { path ->
                rootShell.readSysfs(path).getOrNull()?.trim()
            } ?: ""

            val status = when {
                isChargeLimitEngaged -> ChargingStatus.LIMIT_ENGAGED
                rawStatus.contains("Charging", ignoreCase = true) || currentAmps > 0.05f -> ChargingStatus.CHARGING
                rawStatus.contains("Discharging", ignoreCase = true) || currentAmps < -0.05f -> ChargingStatus.DISCHARGING
                rawStatus.contains("Full", ignoreCase = true) -> ChargingStatus.IDLE
                else -> ChargingStatus.IDLE
            }

            emit(
                BatteryData(
                    percentage = percentage,
                    voltageVolts = voltageVolts,
                    currentAmperes = currentAmps,
                    wattage = wattage,
                    temperatureCelsius = 31.5f,
                    status = status,
                    isChargeLimitActive = isChargeLimitEngaged,
                    isRootControlled = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            delay(1500)
        }
    }.flowOn(ioDispatcher)

    override suspend fun setChargingEnabled(enabled: Boolean): Result<Unit> {
        resolveSysfsPaths()
        val path = resolvedControlPath ?: return Result.failure(IllegalStateException("No sysfs charge control path found"))
        val value = if (enabled) "1" else "0"
        Timber.i("Writing charging_enabled=$value to $path via root shell")
        val result = rootShell.writeSysfs(path, value)
        if (result.isSuccess) {
            isChargeLimitEngaged = !enabled
        }
        return result
    }

    private suspend fun resolveSysfsPaths() {
        if (resolvedVoltagePath == null) {
            resolvedVoltagePath = DeviceCompat.candidateVoltagePaths.firstOrNull { path ->
                rootShell.readSysfs(path).isSuccess
            }
        }
        if (resolvedCurrentPath == null) {
            resolvedCurrentPath = DeviceCompat.candidateCurrentPaths.firstOrNull { path ->
                rootShell.readSysfs(path).isSuccess
            }
        }
        if (resolvedControlPath == null) {
            resolvedControlPath = DeviceCompat.candidateChargingControlPaths.firstOrNull { path ->
                rootShell.readSysfs(path).isSuccess
            }
        }
        if (resolvedStatusPath == null) {
            resolvedStatusPath = DeviceCompat.candidateStatusPaths.firstOrNull { path ->
                rootShell.readSysfs(path).isSuccess
            }
        }
    }

    private suspend fun readSysfsFloat(path: String?, transform: (Float) -> Float): Float? {
        if (path == null) return null
        return try {
            val content = rootShell.readSysfs(path).getOrNull()?.trim()
            val rawValue = content?.toFloatOrNull()
            rawValue?.let(transform)
        } catch (e: Exception) {
            null
        }
    }
}
