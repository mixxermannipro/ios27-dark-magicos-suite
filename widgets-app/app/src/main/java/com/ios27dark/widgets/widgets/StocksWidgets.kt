package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.PrefsRepository
import com.ios27dark.widgets.data.StocksClient
import com.ios27dark.widgets.render.WidgetCanvasRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class StocksSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        StocksWidgets.updateSmall(context, appWidgetIds)
        StocksWidgets.refreshAsync(context, goAsync())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == StocksWidgets.ACTION_REFRESH_STOCKS) {
            StocksWidgets.refreshAsync(context, goAsync())
        }
    }
}

class StocksMediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        StocksWidgets.updateMedium(context, appWidgetIds)
        StocksWidgets.refreshAsync(context, goAsync())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == StocksWidgets.ACTION_REFRESH_STOCKS) {
            StocksWidgets.refreshAsync(context, goAsync())
        }
    }
}

object StocksWidgets {
    const val ACTION_REFRESH_STOCKS = "com.ios27dark.widgets.ACTION_REFRESH_STOCKS"

    fun refreshAsync(ctx: Context, pending: BroadcastReceiver.PendingResult?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                StocksClient.fetchAndCacheStocks(ctx.applicationContext)
                WidgetUpdater.updateStocksWidgets(ctx.applicationContext)
            } finally {
                pending?.finish()
            }
        }
    }

    private fun fmtPrice(p: Double): String = String.format(Locale.US, "%.2f", p)

    private fun fmtPct(pct: Double): String {
        val sign = if (pct >= 0.0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", sign, pct)
    }

    fun updateSmall(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val quotes = PrefsRepository.getStockQuotes(ctx)
        val q = quotes.firstOrNull() ?: return
        val pos = q.changePct >= 0.0
        val chart = WidgetCanvasRenderer.renderSparkline(q.sparkline, pos, 280, 100)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_stocks_small)
            rv.setTextViewText(R.id.stock1_symbol, q.symbol)
            rv.setTextViewText(R.id.stock1_name, q.displayName)
            rv.setTextViewText(R.id.stock1_price, fmtPrice(q.price))
            rv.setTextViewText(R.id.stock1_change, fmtPct(q.changePct))
            rv.setInt(
                R.id.stock1_change,
                "setBackgroundResource",
                if (pos) R.drawable.bg_badge_green else R.drawable.bg_badge_red
            )
            rv.setImageViewBitmap(R.id.stock1_chart, chart)
            rv.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetUpdater.broadcastPendingIntent(ctx, StocksSmallWidgetProvider::class.java, ACTION_REFRESH_STOCKS, 501)
            )
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateMedium(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val quotes = PrefsRepository.getStockQuotes(ctx)

        val rows = intArrayOf(R.id.stock_row_1, R.id.stock_row_2, R.id.stock_row_3)
        val syms = intArrayOf(R.id.sm_sym_1, R.id.sm_sym_2, R.id.sm_sym_3)
        val names = intArrayOf(R.id.sm_name_1, R.id.sm_name_2, R.id.sm_name_3)
        val charts = intArrayOf(R.id.sm_chart_1, R.id.sm_chart_2, R.id.sm_chart_3)
        val prices = intArrayOf(R.id.sm_price_1, R.id.sm_price_2, R.id.sm_price_3)
        val chgs = intArrayOf(R.id.sm_chg_1, R.id.sm_chg_2, R.id.sm_chg_3)

        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_stocks_medium)
            for (i in 0 until 3) {
                val q = quotes.getOrNull(i)
                if (q == null) {
                    rv.setViewVisibility(rows[i], View.GONE)
                    continue
                }
                rv.setViewVisibility(rows[i], View.VISIBLE)
                val pos = q.changePct >= 0.0
                rv.setTextViewText(syms[i], q.displayName)
                rv.setTextViewText(names[i], q.symbol)
                rv.setTextViewText(prices[i], fmtPrice(q.price))
                rv.setTextViewText(chgs[i], fmtPct(q.changePct))
                rv.setInt(
                    chgs[i],
                    "setBackgroundResource",
                    if (pos) R.drawable.bg_badge_green else R.drawable.bg_badge_red
                )
                rv.setImageViewBitmap(
                    charts[i],
                    WidgetCanvasRenderer.renderSparkline(q.sparkline, pos, 200, 64)
                )
            }
            rv.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetUpdater.broadcastPendingIntent(ctx, StocksMediumWidgetProvider::class.java, ACTION_REFRESH_STOCKS, 502)
            )
            mgr.updateAppWidget(id, rv)
        }
    }
}
