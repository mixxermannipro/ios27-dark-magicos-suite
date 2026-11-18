package com.ios27dark.widgets.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarEventInfo(
    val title: String,
    val timeLabel: String,
    val color: Int
)

object CalendarHelper {

    fun hasCalendarPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun getUpcomingEvents(ctx: Context, limit: Int = 3): List<CalendarEventInfo> {
        if (!hasCalendarPermission(ctx)) return emptyList()

        val events = mutableListOf<CalendarEventInfo>()
        val now = System.currentTimeMillis()
        val endRange = now + 7L * 24L * 60L * 60L * 1000L

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, endRange)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR
        )

        try {
            ctx.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val timeFmt = SimpleDateFormat("HH:mm", Locale.GERMANY)
                val dayFmt = SimpleDateFormat("EEE, dd.MM.", Locale.GERMANY)
                val todayCal = Calendar.getInstance()

                while (cursor.moveToNext() && events.size < limit) {
                    val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "Termin"
                    val begin = cursor.getLong(1)
                    val end = cursor.getLong(2)
                    val allDay = cursor.getInt(3) == 1
                    val color = cursor.getInt(4).takeIf { it != 0 } ?: 0xFF0A84FF.toInt()

                    val evCal = Calendar.getInstance().apply { timeInMillis = begin }
                    val isToday = evCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        evCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

                    val label = when {
                        allDay && isToday -> "Heute · Ganztägig"
                        allDay -> "${dayFmt.format(Date(begin))} · Ganztägig"
                        isToday -> "${timeFmt.format(Date(begin))} – ${timeFmt.format(Date(end))}"
                        else -> "${dayFmt.format(Date(begin))} · ${timeFmt.format(Date(begin))}"
                    }

                    events.add(CalendarEventInfo(title, label, color))
                }
            }
        } catch (_: Exception) {
        }
        return events
    }
}
