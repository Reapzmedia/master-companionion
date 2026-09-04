package com.mastercompanion.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChargingStatus {
    CHARGING,
    DISCHARGING,
    IDLE,
    LIMIT_ENGAGED,
    UNKNOWN
}

@Serializable
data class BatteryData(
    val percentage: Int = 0,
    val voltageVolts: Float = 0.0f,
    val currentAmperes: Float = 0.0f,
    val wattage: Float = 0.0f,
    val temperatureCelsius: Float = 0.0f,
    val status: ChargingStatus = ChargingStatus.UNKNOWN,
    val isChargeLimitActive: Boolean = false,
    val isRootControlled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
