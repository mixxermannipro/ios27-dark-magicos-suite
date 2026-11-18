package com.ios27dark.widgets.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ios27dark.widgets.widgets.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            WeatherClient.fetchAndCacheWeather(applicationContext)
            StocksClient.fetchAndCacheStocks(applicationContext)
        } catch (_: Exception) {
        }
        WidgetUpdater.updateAllWidgets(applicationContext)
        Result.success()
    }
}
