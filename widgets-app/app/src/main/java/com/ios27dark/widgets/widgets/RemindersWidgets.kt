package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.PrefsRepository

class RemindersSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        RemindersWidgets.updateSmall(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        RemindersWidgets.handleToggleIntent(context, intent)
    }
}

class RemindersMediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        RemindersWidgets.updateMedium(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        RemindersWidgets.handleToggleIntent(context, intent)
    }
}

object RemindersWidgets {
    const val ACTION_TOGGLE = "com.ios27dark.widgets.ACTION_TOGGLE_REMINDER"
    private const val EXTRA_INDEX = "reminder_index"

    fun handleToggleIntent(ctx: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            val idx = intent.getIntExtra(EXTRA_INDEX, -1)
            if (idx >= 0) {
                PrefsRepository.toggleReminder(ctx, idx)
                WidgetUpdater.updateRemindersWidgets(ctx)
            }
        }
    }

    fun updateSmall(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val items = PrefsRepository.getReminders(ctx)
        val openCount = items.count { !it.done }

        val rows = intArrayOf(R.id.rem_row_1, R.id.rem_row_2, R.id.rem_row_3)
        val icons = intArrayOf(R.id.rem_icon_1, R.id.rem_icon_2, R.id.rem_icon_3)
        val texts = intArrayOf(R.id.rem_text_1, R.id.rem_text_2, R.id.rem_text_3)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_reminders_small)
            rv.setTextViewText(R.id.rem_count, openCount.toString())

            for (i in 0 until 3) {
                val item = items.getOrNull(i)
                if (item == null) {
                    rv.setViewVisibility(rows[i], View.INVISIBLE)
                    continue
                }
                rv.setViewVisibility(rows[i], View.VISIBLE)
                rv.setImageViewResource(icons[i], if (item.done) R.drawable.ic_check_done else R.drawable.ic_check_todo)
                rv.setTextViewText(texts[i], item.title)
                rv.setTextColor(texts[i], if (item.done) Color.parseColor("#636366") else Color.WHITE)
                rv.setInt(
                    texts[i],
                    "setPaintFlags",
                    if (item.done) (Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG) else Paint.ANTI_ALIAS_FLAG
                )
                rv.setOnClickPendingIntent(
                    rows[i],
                    WidgetUpdater.broadcastPendingIntent(
                        ctx,
                        RemindersSmallWidgetProvider::class.java,
                        ACTION_TOGGLE,
                        700 + i
                    ) { putExtra(EXTRA_INDEX, i) }
                )
            }
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 710))
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateMedium(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val items = PrefsRepository.getReminders(ctx)
        val openCount = items.count { !it.done }

        val rows = intArrayOf(R.id.rem_row_1, R.id.rem_row_2, R.id.rem_row_3, R.id.rem_row_4)
        val icons = intArrayOf(R.id.rem_icon_1, R.id.rem_icon_2, R.id.rem_icon_3, R.id.rem_icon_4)
        val texts = intArrayOf(R.id.rem_text_1, R.id.rem_text_2, R.id.rem_text_3, R.id.rem_text_4)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_reminders_medium)
            rv.setTextViewText(R.id.rem_count, "$openCount offen")

            for (i in 0 until 4) {
                val item = items.getOrNull(i)
                if (item == null) {
                    rv.setViewVisibility(rows[i], View.INVISIBLE)
                    continue
                }
                rv.setViewVisibility(rows[i], View.VISIBLE)
                rv.setImageViewResource(icons[i], if (item.done) R.drawable.ic_check_done else R.drawable.ic_check_todo)
                rv.setTextViewText(texts[i], item.title)
                rv.setTextColor(texts[i], if (item.done) Color.parseColor("#636366") else Color.WHITE)
                rv.setInt(
                    texts[i],
                    "setPaintFlags",
                    if (item.done) (Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG) else Paint.ANTI_ALIAS_FLAG
                )
                rv.setOnClickPendingIntent(
                    rows[i],
                    WidgetUpdater.broadcastPendingIntent(
                        ctx,
                        RemindersMediumWidgetProvider::class.java,
                        ACTION_TOGGLE,
                        720 + i
                    ) { putExtra(EXTRA_INDEX, i) }
                )
            }
            rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 730))
            mgr.updateAppWidget(id, rv)
        }
    }
}
