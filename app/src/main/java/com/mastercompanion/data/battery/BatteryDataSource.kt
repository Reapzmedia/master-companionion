package com.mastercompanion.data.battery

import com.mastercompanion.domain.model.BatteryData
import kotlinx.coroutines.flow.Flow

interface BatteryDataSource {
    fun getBatteryDataFlow(): Flow<BatteryData>
    suspend fun setChargingEnabled(enabled: Boolean): Result<Unit>
    suspend fun isOperational(): Boolean
}
