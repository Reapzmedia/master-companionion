package com.mastercompanion.platform

import android.os.Build

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
        "/sys/class/power_supply/Battery/voltage_now",
        "/sys/class/power_supply/bms/voltage_now"
    )

    /**
     * Candidate paths for battery current in sysfs.
     */
    val candidateCurrentPaths = listOf(
        "/sys/class/power_supply/battery/current_now",
        "/sys/class/power_supply/Battery/current_now",
        "/sys/class/power_supply/bms/current_now"
    )

    /**
     * Candidate paths for disabling / enabling battery charging.
     */
    val candidateChargingControlPaths = listOf(
        "/sys/class/power_supply/battery/charging_enabled",
        "/sys/class/power_supply/battery/input_suspend",
        "/sys/class/power_supply/battery/charge_control_limit_max"
    )

    /**
     * Candidate paths for battery status string (e.g. Charging, Discharging, Full).
     */
    val candidateStatusPaths = listOf(
        "/sys/class/power_supply/battery/status",
        "/sys/class/power_supply/Battery/status"
    )
}
