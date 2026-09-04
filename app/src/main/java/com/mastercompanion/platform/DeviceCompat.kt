package com.mastercompanion.platform

import android.os.Build

data class ChargingControlNode(
    val path: String,
    val disableValue: String = "0",
    val enableValue: String = "1",
    val description: String = ""
)

object DeviceCompat {

    /**
     * Determines whether the current device is a Google Pixel device.
     */
    fun isPixelDevice(): Boolean {
        return Build.MANUFACTURER.equals("Google", ignoreCase = true)
    }

    /**
     * Determines whether the current device is a Huawei device (e.g. P20 Lite).
     */
    fun isHuaweiDevice(): Boolean {
        return Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
               Build.BRAND.equals("HUAWEI", ignoreCase = true) ||
               Build.BRAND.equals("HONOR", ignoreCase = true)
    }

    /**
     * Checks whether the current operating system is Android 9 (Pie) or older.
     */
    fun isLegacyAndroid(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
    }

    /**
     * Candidate paths for battery voltage in sysfs.
     */
    val candidateVoltagePaths = listOf(
        "/sys/class/power_supply/battery/voltage_now",
        "/sys/devices/platform/google,battery/power_supply/battery/voltage_now",
        "/sys/class/power_supply/battery/batt_vol",
        "/sys/class/power_supply/Battery/voltage_now",
        "/sys/class/power_supply/bms/voltage_now"
    )

    /**
     * Candidate paths for battery current in sysfs.
     */
    val candidateCurrentPaths = listOf(
        "/sys/class/power_supply/battery/current_now",
        "/sys/devices/platform/google,battery/power_supply/battery/current_now",
        "/sys/class/power_supply/battery/batt_current",
        "/sys/class/power_supply/Battery/current_now",
        "/sys/class/power_supply/bms/current_now"
    )

    /**
     * Candidate paths for battery temperature in sysfs (in tenths of °C, e.g. 315 = 31.5°C).
     */
    val candidateTempPaths = listOf(
        "/sys/class/power_supply/battery/temp",
        "/sys/devices/platform/google,battery/power_supply/battery/temp",
        "/sys/class/power_supply/Battery/temp"
    )

    /**
     * Candidate paths for battery charging status in sysfs.
     */
    val candidateStatusPaths = listOf(
        "/sys/class/power_supply/battery/status",
        "/sys/devices/platform/google,battery/power_supply/battery/status",
        "/sys/class/power_supply/Battery/status"
    )

    /**
     * Comprehensive ACC-grade candidate nodes for disabling / enabling battery charging.
     * Includes polarity values (disableValue and enableValue) so inverted nodes don't accidentally enable charge.
     */
    val candidateChargingControlNodes = listOf(
        // 1. Google Pixel 6/7/8/9 (Tensor GS201, cheetah/pantah) standard
        ChargingControlNode("/sys/class/power_supply/battery/charging_enabled", "0", "1", "Pixel Standard Charging Switch"),
        ChargingControlNode("/sys/devices/platform/google,battery/power_supply/battery/charging_enabled", "0", "1", "Google Battery Platform Switch"),
        // 2. Huawei / Honor (P20 Lite Kirin 659)
        ChargingControlNode("/sys/class/power_supply/Battery/charging_enabled", "0", "1", "Huawei Battery Switch"),
        ChargingControlNode("/sys/class/hw_power/charger/charge_data/enable_charger", "0", "1", "Huawei Hardware Charger Switch"),
        // 3. Inverted input_suspend / charge_disable nodes (1 = disconnect power / suspend, 0 = normal)
        ChargingControlNode("/sys/class/power_supply/battery/input_suspend", "1", "0", "Battery Input Suspend"),
        ChargingControlNode("/sys/class/power_supply/Battery/input_suspend", "1", "0", "Huawei Battery Input Suspend"),
        ChargingControlNode("/sys/class/power_supply/battery/charge_disable", "1", "0", "Battery Charge Disable"),
        // 4. Android 14/15 QPR & Linux power supply standard charge control limit
        ChargingControlNode("/sys/class/power_supply/battery/charge_control_limit_max", "0", "1", "Charge Control Limit"),
        ChargingControlNode("/sys/class/power_supply/battery/store_mode", "1", "0", "Store / Bypass Mode"),
        // 5. Generic PMIC / USB charger nodes
        ChargingControlNode("/sys/class/power_supply/usb/charging_enabled", "0", "1", "USB Charger Switch"),
        ChargingControlNode("/sys/class/power_supply/main/charging_enabled", "0", "1", "Main Charger Switch")
    )

    val candidateChargingControlPaths = candidateChargingControlNodes.map { it.path }

    fun getChargingControlPath(): String = candidateChargingControlPaths.first()
}
