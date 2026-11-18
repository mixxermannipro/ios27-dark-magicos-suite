package com.ios27dark.widgets.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.ios27dark.widgets.MainActivity

object WidgetUpdater {

    fun openAppPendingIntent(ctx: Context, requestCode: Int): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            ctx,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun broadcastPendingIntent(
        ctx: Context,
        cls: Class<*>,
        action: String,
        requestCode: Int,
        configure: (Intent.() -> Unit)? = null
    ): PendingIntent {
        val intent = Intent(ctx, cls).apply {
            this.action = action
            configure?.invoke(this)
        }
        return PendingIntent.getBroadcast(
            ctx,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateAllWidgets(ctx: Context) {
        updateClass(ctx, ClockAnalogWidgetProvider::class.java) { ClockWidgets.updateAnalog(ctx, it) }
        updateClass(ctx, ClockDigitalWidgetProvider::class.java) { ClockWidgets.updateDigital(ctx, it) }
        updateClass(ctx, WorldClockWidgetProvider::class.java) { ClockWidgets.updateWorldClock(ctx, it) }
        updateWeatherWidgets(ctx)
        updateCalendarWidgets(ctx)
        updateBatteryWidgets(ctx)
        updateStocksWidgets(ctx)
        updateMusicWidgets(ctx)
        updateRemindersWidgets(ctx)
        updatePhotosWidgets(ctx)
    }

    fun updateWeatherWidgets(ctx: Context) {
        updateClass(ctx, WeatherSmallWidgetProvider::class.java) { WeatherWidgets.updateSmall(ctx, it) }
        updateClass(ctx, WeatherMediumWidgetProvider::class.java) { WeatherWidgets.updateMedium(ctx, it) }
    }

    fun updateCalendarWidgets(ctx: Context) {
        updateClass(ctx, CalendarSmallWidgetProvider::class.java) { CalendarWidgets.updateSmall(ctx, it) }
        updateClass(ctx, CalendarMediumWidgetProvider::class.java) { CalendarWidgets.updateMedium(ctx, it) }
    }

    fun updateBatteryWidgets(ctx: Context) {
        updateClass(ctx, BatterySmallWidgetProvider::class.java) { BatteryWidgets.updateSmall(ctx, it) }
        updateClass(ctx, BatteryMediumWidgetProvider::class.java) { BatteryWidgets.updateMedium(ctx, it) }
    }

    fun updateStocksWidgets(ctx: Context) {
        updateClass(ctx, StocksSmallWidgetProvider::class.java) { StocksWidgets.updateSmall(ctx, it) }
        updateClass(ctx, StocksMediumWidgetProvider::class.java) { StocksWidgets.updateMedium(ctx, it) }
    }

    fun updateMusicWidgets(ctx: Context) {
        updateClass(ctx, MusicWidgetProvider::class.java) { MusicWidgetProvider.update(ctx, it) }
    }

    fun updateRemindersWidgets(ctx: Context) {
        updateClass(ctx, RemindersSmallWidgetProvider::class.java) { RemindersWidgets.updateSmall(ctx, it) }
        updateClass(ctx, RemindersMediumWidgetProvider::class.java) { RemindersWidgets.updateMedium(ctx, it) }
    }

    fun updatePhotosWidgets(ctx: Context) {
        updateClass(ctx, PhotosSmallWidgetProvider::class.java) { PhotosWidgets.updateSmall(ctx, it) }
        updateClass(ctx, PhotosLargeWidgetProvider::class.java) { PhotosWidgets.updateLarge(ctx, it) }
    }

    private fun updateClass(ctx: Context, cls: Class<*>, block: (IntArray) -> Unit) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
        if (ids.isNotEmpty()) {
            block(ids)
        }
    }
}
