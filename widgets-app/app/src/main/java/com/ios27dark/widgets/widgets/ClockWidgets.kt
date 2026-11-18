package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.PrefsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class ClockAnalogWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ClockWidgets.updateAnalog(context, appWidgetIds)
    }
}

class ClockDigitalWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ClockWidgets.updateDigital(context, appWidgetIds)
    }
}

class WorldClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ClockWidgets.updateWorldClock(context, appWidgetIds)
    }
}

object ClockWidgets {

    fun updateAnalog(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val tzShort = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT, Locale.GERMANY)
        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_clock_analog)
            rv.setTextViewText(R.id.clock_city_label, tzShort.uppercase())
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 101))
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateDigital(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val now = Date()
        val weekday = SimpleDateFormat("EEEE", Locale.GERMANY).format(now).uppercase()
        val dateStr = SimpleDateFormat("d. MMMM", Locale.GERMANY).format(now)
        val tzShort = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT, Locale.GERMANY)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_clock_digital)
            rv.setTextViewText(R.id.digital_weekday, weekday)
            rv.setTextViewText(R.id.digital_date, dateStr)
            rv.setTextViewText(R.id.digital_tz, "Ortszeit · $tzShort")
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 102))
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateWorldClock(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val zones = PrefsRepository.getWorldZones(ctx)
        val localTz = TimeZone.getDefault()
        val now = System.currentTimeMillis()
        val localOffset = localTz.getOffset(now)

        val cityIds = intArrayOf(R.id.wc_city_1, R.id.wc_city_2, R.id.wc_city_3, R.id.wc_city_4)
        val offsetIds = intArrayOf(R.id.wc_offset_1, R.id.wc_offset_2, R.id.wc_offset_3, R.id.wc_offset_4)
        val clockIds = intArrayOf(R.id.wc_time_1, R.id.wc_time_2, R.id.wc_time_3, R.id.wc_time_4)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_world_clock)
            for (i in 0 until 4) {
                val z = zones.getOrNull(i) ?: continue
                val tz = TimeZone.getTimeZone(z.zoneId)
                val diffHours = ((tz.getOffset(now) - localOffset) / 3600000.0).roundToInt()
                val sign = if (diffHours >= 0) "+$diffHours" else "$diffHours"
                rv.setTextViewText(cityIds[i], z.city)
                rv.setTextViewText(offsetIds[i], "${sign}h")
                rv.setString(clockIds[i], "setTimeZone", z.zoneId)
            }
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 103))
            mgr.updateAppWidget(id, rv)
        }
    }
}
