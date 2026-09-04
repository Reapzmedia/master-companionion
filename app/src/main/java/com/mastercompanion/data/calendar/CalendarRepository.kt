package com.mastercompanion.data.calendar

import android.Manifest
import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.mastercompanion.domain.model.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    private val _latestReminder = MutableStateFlow<CalendarEvent?>(null)
    val latestReminder: StateFlow<CalendarEvent?> = _latestReminder.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _syncedAccount = MutableStateFlow<String?>(null)
    val syncedAccount: StateFlow<String?> = _syncedAccount.asStateFlow()

    init {
        // Start background periodic refresher
        scope.launch {
            while (isActive) {
                refreshCalendar()
                delay(180_000L) // Refresh every 3 minutes
            }
        }
    }

    fun refreshCalendar() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        _hasPermission.value = granted

        if (!granted) {
            _events.value = emptyList()
            _latestReminder.value = null
            _syncedAccount.value = null
            return
        }

        try {
            // 1. Detect active Google Account synced to CalendarContract
            val detectedAccount = queryGoogleAccount()
            _syncedAccount.value = detectedAccount

            // 2. Query Calendar Instances
            val now = System.currentTimeMillis()
            val windowEnd = now + (30L * 86400_000L) // Next 30 days

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now - (30 * 60_000L)) // Include events in progress
            ContentUris.appendId(builder, windowEnd)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR
            )

            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            val parsedList = mutableListOf<CalendarEvent>()

            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val colorIdx = it.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) it.getString(titleIdx) ?: "Untitled Event" else "Untitled Event"
                    val desc = if (descIdx >= 0) it.getString(descIdx) ?: "" else ""
                    val begin = if (beginIdx >= 0) it.getLong(beginIdx) else now
                    val end = if (endIdx >= 0) it.getLong(endIdx) else (begin + 3600_000L)
                    val loc = if (locIdx >= 0) it.getString(locIdx) ?: "" else ""
                    val allDay = if (allDayIdx >= 0) it.getInt(allDayIdx) == 1 else false
                    val color = if (colorIdx >= 0) it.getInt(colorIdx) else 0

                    parsedList.add(
                        CalendarEvent(
                            id = id,
                            title = title,
                            description = desc,
                            startTimeMillis = begin,
                            endTimeMillis = end,
                            location = loc,
                            isAllDay = allDay,
                            color = color
                        )
                    )
                }
            }

            // Zero filler text: only emit genuine events
            _events.value = parsedList
            _latestReminder.value = parsedList.firstOrNull { it.endTimeMillis >= now }
        } catch (e: Exception) {
            _events.value = emptyList()
            _latestReminder.value = null
        }
    }

    /**
     * Finds the primary Google account associated with calendars on this device.
     */
    private fun queryGoogleAccount(): String? {
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val selection = "(${CalendarContract.Calendars.ACCOUNT_TYPE} = ?)"
        val selectionArgs = arrayOf("com.google")

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                if (cursor.moveToFirst() && nameIdx >= 0) {
                    cursor.getString(nameIdx)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Triggers an immediate system sync for all Google Calendar accounts.
     */
    fun triggerGoogleSync() {
        scope.launch {
            try {
                val googleEmail = _syncedAccount.value
                if (!googleEmail.isNullOrBlank()) {
                    val account = Account(googleEmail, "com.google")
                    val bundle = Bundle().apply {
                        putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    }
                    ContentResolver.requestSync(account, CalendarContract.AUTHORITY, bundle)
                }
                delay(2000L)
                refreshCalendar()
            } catch (_: Exception) {}
        }
    }

    /**
     * Intent to open Google Calendar on the device.
     */
    fun getOpenCalendarIntent(): Intent {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
