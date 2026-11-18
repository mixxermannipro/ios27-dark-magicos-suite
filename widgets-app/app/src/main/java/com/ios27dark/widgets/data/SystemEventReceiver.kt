package com.ios27dark.widgets.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ios27dark.widgets.widgets.WidgetUpdater
import java.util.concurrent.TimeUnit

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                schedulePeriodicUpdates(context)
                WidgetUpdater.updateAllWidgets(context)
            }
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                WidgetUpdater.updateBatteryWidgets(context)
            }
        }
    }

    companion object {
        private const val WORK_NAME = "ios27_widget_periodic_refresh"

        fun schedulePeriodicUpdates(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
