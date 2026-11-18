package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.PrefsRepository
import com.ios27dark.widgets.data.WeatherClient
import com.ios27dark.widgets.render.WidgetCanvasRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WeatherWidgets.updateSmall(context, appWidgetIds)
        WeatherWidgets.refreshAsync(context, goAsync())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WeatherWidgets.ACTION_REFRESH_WEATHER) {
            WeatherWidgets.refreshAsync(context, goAsync())
        }
    }
}

class WeatherMediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WeatherWidgets.updateMedium(context, appWidgetIds)
        WeatherWidgets.refreshAsync(context, goAsync())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WeatherWidgets.ACTION_REFRESH_WEATHER) {
            WeatherWidgets.refreshAsync(context, goAsync())
        }
    }
}

object WeatherWidgets {
    const val ACTION_REFRESH_WEATHER = "com.ios27dark.widgets.ACTION_REFRESH_WEATHER"

    fun refreshAsync(ctx: Context, pending: android.content.BroadcastReceiver.PendingResult?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WeatherClient.fetchAndCacheWeather(ctx.applicationContext)
                WidgetUpdater.updateWeatherWidgets(ctx.applicationContext)
            } finally {
                pending?.finish()
            }
        }
    }

    fun updateSmall(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val w = PrefsRepository.getWeather(ctx)
        val iconBmp = WidgetCanvasRenderer.renderWeatherIcon(w.weatherCode, w.isDay, 96)
        val desc = WeatherClient.wmoDescriptionDe(w.weatherCode)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_weather_small)
            rv.setTextViewText(R.id.weather_city, w.city)
            rv.setTextViewText(R.id.weather_temp, "${w.tempC}°")
            rv.setImageViewBitmap(R.id.weather_icon, iconBmp)
            rv.setTextViewText(R.id.weather_desc, desc)
            rv.setTextViewText(R.id.weather_hl, "H:${w.highC}°  T:${w.lowC}°")
            rv.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetUpdater.broadcastPendingIntent(ctx, WeatherSmallWidgetProvider::class.java, ACTION_REFRESH_WEATHER, 201)
            )
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateMedium(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val w = PrefsRepository.getWeather(ctx)
        val mainIcon = WidgetCanvasRenderer.renderWeatherIcon(w.weatherCode, w.isDay, 96)
        val desc = WeatherClient.wmoDescriptionDe(w.weatherCode)

        val tIds = intArrayOf(R.id.wh_time_1, R.id.wh_time_2, R.id.wh_time_3, R.id.wh_time_4, R.id.wh_time_5)
        val iIds = intArrayOf(R.id.wh_icon_1, R.id.wh_icon_2, R.id.wh_icon_3, R.id.wh_icon_4, R.id.wh_icon_5)
        val tempIds = intArrayOf(R.id.wh_temp_1, R.id.wh_temp_2, R.id.wh_temp_3, R.id.wh_temp_4, R.id.wh_temp_5)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_weather_medium)
            rv.setTextViewText(R.id.weather_city, w.city)
            rv.setTextViewText(R.id.weather_temp, "${w.tempC}°")
            rv.setImageViewBitmap(R.id.weather_icon, mainIcon)
            rv.setTextViewText(R.id.weather_desc, desc)
            rv.setTextViewText(R.id.weather_hl, "H:${w.highC}°  T:${w.lowC}°")

            for (idx in 0 until 5) {
                val h = w.hourly.getOrNull(idx) ?: continue
                rv.setTextViewText(tIds[idx], h.hourLabel)
                rv.setImageViewBitmap(iIds[idx], WidgetCanvasRenderer.renderWeatherIcon(h.weatherCode, true, 64))
                rv.setTextViewText(tempIds[idx], "${h.tempC}°")
            }

            rv.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetUpdater.broadcastPendingIntent(ctx, WeatherMediumWidgetProvider::class.java, ACTION_REFRESH_WEATHER, 202)
            )
            mgr.updateAppWidget(id, rv)
        }
    }
}
