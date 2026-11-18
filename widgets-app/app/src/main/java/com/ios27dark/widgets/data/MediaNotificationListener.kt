package com.ios27dark.widgets.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ios27dark.widgets.widgets.WidgetUpdater
import java.io.File
import java.io.FileOutputStream

class MediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        WidgetUpdater.updateMusicWidgets(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        WidgetUpdater.updateMusicWidgets(applicationContext)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        WidgetUpdater.updateMusicWidgets(applicationContext)
    }
}

object MediaControllerHelper {

    private const val ART_FILE_NAME = "now_playing_art.png"

    fun hasNotificationAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            ?: return false
        val cn = ComponentName(ctx, MediaNotificationListener::class.java).flattenToString()
        return flat.split(":").any { it.equals(cn, ignoreCase = true) }
    }

    fun openNotificationAccessSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    private fun getActiveController(ctx: Context): MediaController? {
        if (!hasNotificationAccess(ctx)) return null
        return try {
            val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return null
            val comp = ComponentName(ctx, MediaNotificationListener::class.java)
            val sessions = msm.getActiveSessions(comp)
            sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentMediaState(ctx: Context): Pair<MediaState, Bitmap?> {
        val perm = hasNotificationAccess(ctx)
        if (!perm) {
            return MediaState(
                title = "Zugriff aktivieren",
                artist = "Tippen für Benachrichtigungszugriff",
                sourceApp = "NOW PLAYING",
                isPlaying = false,
                hasActiveSession = false,
                hasPermission = false
            ) to null
        }

        val controller = getActiveController(ctx) ?: return MediaState(
            title = "Keine Wiedergabe",
            artist = "Spotify, Apple Music oder YT Music",
            sourceApp = "NOW PLAYING",
            isPlaying = false,
            hasActiveSession = false,
            hasPermission = true
        ) to loadCachedArt(ctx)

        val meta = controller.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unbekannter Titel"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unbekannter Interpret"

        val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (art != null) {
            saveCachedArt(ctx, art)
        }

        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        val sourceLabel = friendlyPlayerName(controller.packageName)

        return MediaState(
            title = title,
            artist = artist,
            sourceApp = sourceLabel,
            isPlaying = playing,
            hasActiveSession = true,
            hasPermission = true
        ) to (art ?: loadCachedArt(ctx))
    }

    fun handleAction(ctx: Context, action: String) {
        val controller = getActiveController(ctx) ?: return
        val controls = controller.transportControls
        when (action) {
            "PREV" -> controls.skipToPrevious()
            "NEXT" -> controls.skipToNext()
            "PLAY_PAUSE" -> {
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    controls.pause()
                } else {
                    controls.play()
                }
            }
        }
    }

    private fun friendlyPlayerName(pkg: String?): String = when (pkg) {
        "com.spotify.music" -> "SPOTIFY"
        "com.apple.android.music" -> "APPLE MUSIC"
        "com.google.android.apps.youtube.music" -> "YOUTUBE MUSIC"
        "com.google.android.youtube" -> "YOUTUBE"
        "com.hihonor.music", "com.huawei.music" -> "HONOR MUSIK"
        "com.netease.cloudmusic" -> "NETEASE MUSIC"
        "com.tencent.qqmusic" -> "QQ MUSIC"
        else -> "NOW PLAYING"
    }

    private fun saveCachedArt(ctx: Context, bmp: Bitmap) {
        try {
            val file = File(ctx.filesDir, ART_FILE_NAME)
            val scaled = Bitmap.createScaledBitmap(bmp, 240, 240, true)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (_: Exception) {
        }
    }

    private fun loadCachedArt(ctx: Context): Bitmap? {
        return try {
            val file = File(ctx.filesDir, ART_FILE_NAME)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (_: Exception) {
            null
        }
    }
}
