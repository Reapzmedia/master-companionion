package com.mastercompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryDataTest {

    @Test
    fun `battery data computed properties map correctly`() {
        val data = BatteryData(
            percentage = 82,
            voltageVolts = 4.15f,
            currentAmperes = 1.25f,
            wattage = 5.18f,
            temperatureCelsius = 31.4f,
            status = ChargingStatus.CHARGING,
            isChargeLimitActive = true,
            isRootControlled = true
        )

        assertEquals(82, data.level)
        assertTrue(data.isCharging)
        assertTrue(data.isBypassed)
        assertEquals(4150, data.voltageMv)
        assertEquals(1250, data.currentMa)
        assertEquals(31.4f, data.temperatureC, 0.01f)
    }

    @Test
    fun `discharging battery status indicates not charging`() {
        val data = BatteryData(
            percentage = 50,
            status = ChargingStatus.DISCHARGING,
            isChargeLimitActive = false
        )

        assertFalse(data.isCharging)
        assertFalse(data.isBypassed)
    }
}
