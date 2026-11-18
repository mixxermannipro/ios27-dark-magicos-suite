package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.MediaControllerHelper
import com.ios27dark.widgets.render.WidgetCanvasRenderer

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV -> {
                MediaControllerHelper.handleAction(context, "PREV")
                WidgetUpdater.updateMusicWidgets(context)
            }
            ACTION_PLAY_PAUSE -> {
                if (!MediaControllerHelper.hasNotificationAccess(context)) {
                    MediaControllerHelper.openNotificationAccessSettings(context)
                } else {
                    MediaControllerHelper.handleAction(context, "PLAY_PAUSE")
                    WidgetUpdater.updateMusicWidgets(context)
                }
            }
            ACTION_NEXT -> {
                MediaControllerHelper.handleAction(context, "NEXT")
                WidgetUpdater.updateMusicWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_PREV = "com.ios27dark.widgets.ACTION_MEDIA_PREV"
        const val ACTION_PLAY_PAUSE = "com.ios27dark.widgets.ACTION_MEDIA_PLAY_PAUSE"
        const val ACTION_NEXT = "com.ios27dark.widgets.ACTION_MEDIA_NEXT"

        fun update(ctx: Context, ids: IntArray) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val (state, artBmp) = MediaControllerHelper.getCurrentMediaState(ctx)

            val coverRounded = if (artBmp != null) {
                WidgetCanvasRenderer.renderRoundedBitmap(artBmp, 220, 220, 32f)
            } else {
                WidgetCanvasRenderer.renderDefaultPhotoArt(220, 220, 32f)
            }

            for (id in ids) {
                val rv = RemoteViews(ctx.packageName, R.layout.widget_music)
                rv.setTextViewText(R.id.music_source, state.sourceApp)
                rv.setTextViewText(R.id.music_title, state.title)
                rv.setTextViewText(R.id.music_artist, state.artist)
                rv.setImageViewBitmap(R.id.music_cover, coverRounded)
                rv.setImageViewResource(
                    R.id.music_btn_play_pause,
                    if (state.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
                )

                rv.setOnClickPendingIntent(
                    R.id.music_btn_prev,
                    WidgetUpdater.broadcastPendingIntent(ctx, MusicWidgetProvider::class.java, ACTION_PREV, 601)
                )
                rv.setOnClickPendingIntent(
                    R.id.music_btn_play_pause,
                    WidgetUpdater.broadcastPendingIntent(ctx, MusicWidgetProvider::class.java, ACTION_PLAY_PAUSE, 602)
                )
                rv.setOnClickPendingIntent(
                    R.id.music_btn_next,
                    WidgetUpdater.broadcastPendingIntent(ctx, MusicWidgetProvider::class.java, ACTION_NEXT, 603)
                )
                rv.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(ctx, 604))

                mgr.updateAppWidget(id, rv)
            }
        }
    }
}
