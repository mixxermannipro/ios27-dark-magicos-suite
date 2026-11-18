package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.CalendarHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CalendarWidgets.updateSmall(context, appWidgetIds)
    }
}

class CalendarMediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CalendarWidgets.updateMedium(context, appWidgetIds)
    }
}

object CalendarWidgets {

    fun updateSmall(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val now = Date()
        val weekday = SimpleDateFormat("EEEE", Locale.GERMANY).format(now).uppercase()
        val day = SimpleDateFormat("d", Locale.GERMANY).format(now)
        val hasPerm = CalendarHelper.hasCalendarPermission(ctx)
        val events = CalendarHelper.getUpcomingEvents(ctx, 1)

        val nextText = when {
            !hasPerm -> "Kalenderzugriff erlauben"
            events.isNotEmpty() -> "${events[0].title}\n${events[0].timeLabel}"
            else -> "Keine Termine heute"
        }

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_calendar_small)
            rv.setTextViewText(R.id.cal_weekday, weekday)
            rv.setTextViewText(R.id.cal_day, day)
            rv.setTextViewText(R.id.cal_next_event, nextText)
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 301))
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateMedium(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val now = Date()
        val weekday = SimpleDateFormat("EEEE", Locale.GERMANY).format(now).uppercase()
        val day = SimpleDateFormat("d", Locale.GERMANY).format(now)
        val month = SimpleDateFormat("MMMM yyyy", Locale.GERMANY).format(now)
        val hasPerm = CalendarHelper.hasCalendarPermission(ctx)
        val events = CalendarHelper.getUpcomingEvents(ctx, 2)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_calendar_medium)
            rv.setTextViewText(R.id.cal_weekday, weekday)
            rv.setTextViewText(R.id.cal_day, day)
            rv.setTextViewText(R.id.cal_month, month)

            if (!hasPerm) {
                rv.setViewVisibility(R.id.cal_event_row_1, View.VISIBLE)
                rv.setTextViewText(R.id.cal_ev1_title, "Kalender-Berechtigung fehlt")
                rv.setTextViewText(R.id.cal_ev1_time, "Tippen zum Aktivieren")
                rv.setViewVisibility(R.id.cal_event_row_2, View.GONE)
            } else if (events.isEmpty()) {
                rv.setViewVisibility(R.id.cal_event_row_1, View.VISIBLE)
                rv.setTextViewText(R.id.cal_ev1_title, "Keine anstehenden Termine")
                rv.setTextViewText(R.id.cal_ev1_time, "Nächste 7 Tage frei")
                rv.setViewVisibility(R.id.cal_event_row_2, View.GONE)
            } else {
                rv.setViewVisibility(R.id.cal_event_row_1, View.VISIBLE)
                rv.setTextViewText(R.id.cal_ev1_title, events[0].title)
                rv.setTextViewText(R.id.cal_ev1_time, events[0].timeLabel)
                if (events.size > 1) {
                    rv.setViewVisibility(R.id.cal_event_row_2, View.VISIBLE)
                    rv.setTextViewText(R.id.cal_ev2_title, events[1].title)
                    rv.setTextViewText(R.id.cal_ev2_time, events[1].timeLabel)
                } else {
                    rv.setViewVisibility(R.id.cal_event_row_2, View.GONE)
                }
            }

            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 302))
            mgr.updateAppWidget(id, rv)
        }
    }
}
