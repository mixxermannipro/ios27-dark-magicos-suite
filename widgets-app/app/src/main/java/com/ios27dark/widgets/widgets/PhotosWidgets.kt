package com.ios27dark.widgets.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.ios27dark.widgets.R
import com.ios27dark.widgets.data.PrefsRepository
import com.ios27dark.widgets.render.WidgetCanvasRenderer

class PhotosSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        PhotosWidgets.updateSmall(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        PhotosWidgets.handleIntent(context, intent)
    }
}

class PhotosLargeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        PhotosWidgets.updateLarge(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        PhotosWidgets.handleIntent(context, intent)
    }
}

object PhotosWidgets {
    const val ACTION_NEXT_PHOTO = "com.ios27dark.widgets.ACTION_NEXT_PHOTO"

    fun handleIntent(ctx: Context, intent: Intent) {
        if (intent.action == ACTION_NEXT_PHOTO) {
            val uris = PrefsRepository.getPhotoUris(ctx)
            if (uris.size > 1) {
                PrefsRepository.advancePhotoIndex(ctx)
                WidgetUpdater.updatePhotosWidgets(ctx)
            } else {
                ctx.startActivity(
                    Intent(ctx, com.ios27dark.widgets.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
            }
        }
    }

    private fun loadSelectedPhotoBitmap(ctx: Context, targetSize: Int): Pair<Bitmap, String> {
        val uris = PrefsRepository.getPhotoUris(ctx)
        if (uris.isNotEmpty()) {
            val idx = PrefsRepository.getPhotoIndex(ctx).coerceIn(0, uris.lastIndex)
            val uriStr = uris[idx]
            try {
                val uri = Uri.parse(uriStr)
                val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, optsBounds)
                }
                var sample = 1
                val maxDim = maxOf(optsBounds.outWidth, optsBounds.outHeight)
                while (maxDim / sample > targetSize * 2) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                if (decoded != null) {
                    val caption = if (uris.size > 1) "Foto ${idx + 1}/${uris.size}" else "Ausgewähltes Foto"
                    return WidgetCanvasRenderer.renderRoundedBitmap(decoded, targetSize, targetSize, targetSize * 0.11f) to caption
                }
            } catch (_: Exception) {
            }
        }
        return WidgetCanvasRenderer.renderDefaultPhotoArt(targetSize, targetSize, targetSize * 0.11f) to "Tippen zum Auswählen"
    }

    fun updateSmall(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val (bmp, caption) = loadSelectedPhotoBitmap(ctx, 380)
        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_photos_small)
            rv.setImageViewBitmap(R.id.photos_image, bmp)
            rv.setTextViewText(R.id.photos_caption, caption)
            rv.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetUpdater.broadcastPendingIntent(ctx, PhotosSmallWidgetProvider::class.java, ACTION_NEXT_PHOTO, 801)
            )
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateLarge(ctx: Context, ids: IntArray) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val (bmp, caption) = loadSelectedPhotoBitmap(ctx, 640)
        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_photos_large)
            rv.setImageViewBitmap(R.id.photos_image, bmp)
            rv.setTextViewText(R.id.photos_caption, caption)
            rv.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetUpdater.broadcastPendingIntent(ctx, PhotosLargeWidgetProvider::class.java, ACTION_NEXT_PHOTO, 802)
            )
            mgr.updateAppWidget(id, rv)
        }
    }
}
