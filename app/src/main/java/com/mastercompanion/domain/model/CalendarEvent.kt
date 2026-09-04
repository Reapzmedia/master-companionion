package com.mastercompanion.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Domain model representing an event or reminder queried from Android's CalendarContract
 * (which natively synchronizes with the active Google Calendar account on device).
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val location: String = "",
    val isAllDay: Boolean = false,
    val color: Int = 0,
    val reminderMinutes: Int? = null
) {
    val isOngoing: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startTimeMillis..endTimeMillis
        }

    fun formattedTimeRange(use24Hour: Boolean = true): String {
        if (isAllDay) return "All Day"
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val startStr = sdf.format(Date(startTimeMillis))
        val endStr = sdf.format(Date(endTimeMillis))
        return "$startStr – $endStr"
    }

    fun relativeTimeString(): String {
        val now = System.currentTimeMillis()
        if (isOngoing) return "Happening Now"

        val diffMillis = startTimeMillis - now
        if (diffMillis < 0) return "Past"

        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffMinutes < 60 -> "In ${diffMinutes}m"
            diffHours < 24 -> "In ${diffHours}h ${diffMinutes % 60}m"
            diffDays == 1L -> "Tomorrow"
            else -> "In ${diffDays}d"
        }
    }
}
