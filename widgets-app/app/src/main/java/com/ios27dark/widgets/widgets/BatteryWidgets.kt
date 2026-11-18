package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.BatteryHelper
import com.ios27dark.widgets.render.WidgetCanvasRenderer

class BatterySmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        BatteryWidgets.updateSmall(context, appWidgetIds)
    }
}

class BatteryMediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        BatteryWidgets.updateMedium(context, appWidgetIds)
    }
}

object BatteryWidgets {

    private data class Slot(val name: String, val level: Int, val charging: Boolean, val kind: String)

    private fun buildSlots(ctx: Context): List<Slot> {
        val phone = BatteryHelper.getPhoneBattery(ctx)
        val phoneName = Build.MODEL.take(12).ifBlank { "Smartphone" }
        val bt = BatteryHelper.getConnectedBluetoothDevices(ctx)

        val slots = mutableListOf(Slot(phoneName, phone.level, phone.isCharging, "phone"))
        for (d in bt.take(3)) {
            slots.add(Slot(d.name, d.level, false, d.kind))
        }
        while (slots.size < 4) {
            slots.add(Slot("Frei", -1, false, "audio"))
        }
        return slots
    }

    fun updateSmall(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val slots = buildSlots(ctx)
        val ringIds = intArrayOf(R.id.bat_ring_1, R.id.bat_ring_2, R.id.bat_ring_3, R.id.bat_ring_4)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_battery_small)
            for (i in 0 until 4) {
                val s = slots[i]
                val bmp = WidgetCanvasRenderer.renderBatteryRing(s.level, s.charging, s.kind, 150, showPercentInside = true)
                rv.setImageViewBitmap(ringIds[i], bmp)
            }
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 401))
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateMedium(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val slots = buildSlots(ctx)
        val ringIds = intArrayOf(R.id.bat_ring_1, R.id.bat_ring_2, R.id.bat_ring_3, R.id.bat_ring_4)
        val nameIds = intArrayOf(R.id.bat_name_1, R.id.bat_name_2, R.id.bat_name_3, R.id.bat_name_4)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_battery_medium)
            for (i in 0 until 4) {
                val s = slots[i]
                val bmp = WidgetCanvasRenderer.renderBatteryRing(s.level, s.charging, s.kind, 180, showPercentInside = true)
                rv.setImageViewBitmap(ringIds[i], bmp)
                rv.setTextViewText(nameIds[i], if (s.level >= 0) s.name else "Nicht verb.")
            }
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 402))
            mgr.updateAppWidget(id, rv)
        }
    }
}
