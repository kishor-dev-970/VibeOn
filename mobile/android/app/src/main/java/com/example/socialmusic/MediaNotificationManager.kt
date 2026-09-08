package com.example.socialmusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object MediaNotificationManager {
    const val CHANNEL_ID = "vibeon_media_playback"
    const val NOTIFICATION_ID = 1001

    const val ACTION_PLAY = "com.example.socialmusic.action.MEDIA_PLAY"
    const val ACTION_PAUSE = "com.example.socialmusic.action.MEDIA_PAUSE"
    const val ACTION_NEXT = "com.example.socialmusic.action.MEDIA_NEXT"
    const val ACTION_PREV = "com.example.socialmusic.action.MEDIA_PREV"
    const val ACTION_STOP = "com.example.socialmusic.action.MEDIA_STOP"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val imageExecutor = Executors.newSingleThreadExecutor()

    private var mediaSession: MediaSessionCompat? = null

    @Volatile
    var currentVideoId: String = ""
        private set

    @Volatile
    var currentTitle: String = ""
        private set

    @Volatile
    var currentArtist: String = ""
        private set

    @Volatile
    var currentThumbnailUrl: String = ""
        private set

    @Volatile
    var currentIsPlaying: Boolean = false
        private set

    @Volatile
    var currentPositionMs: Long = 0L
        private set

    private var cachedArtworkUrl: String = ""
    private var cachedArtworkBitmap: Bitmap? = null

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    private fun acquireLocks(context: Context) {
        try {
            if (wakeLock == null) {
                val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "VibeOn:MediaPlaybackWakeLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours timeout
            }
        } catch (_: Exception) {}

        try {
            if (wifiLock == null) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm.createWifiLock(mode, "VibeOn:MediaPlaybackWifiLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
    }

    fun getOrCreateSession(context: Context): MediaSessionCompat {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(context.applicationContext, "VibeOnMediaSession").apply {
                setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        handlePlay()
                    }

                    override fun onPause() {
                        handlePause()
                    }

                    override fun onSkipToNext() {
                        handleNext()
                    }

                    override fun onSkipToPrevious() {
                        handlePrev()
                    }

                    override fun onStop() {
                        handleStop(context)
                    }

                    override fun onSeekTo(pos: Long) {
                        handleSeekTo(pos)
                    }
                })
                isActive = true
            }
        }
        return mediaSession!!
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "VibeOn Media Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Playback controls and lock screen media card"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun updatePlayback(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String,
        isPlaying: Boolean,
        positionMs: Long = 0L
    ) {
        val metadataChanged = (
            videoId != currentVideoId ||
            isPlaying != currentIsPlaying ||
            currentTitle != title.ifBlank { "Now Playing" } ||
            currentArtist != artist.ifBlank { "YouTube Music" }
        )

        currentVideoId = videoId
        currentTitle = title.ifBlank { "Now Playing" }
        currentArtist = artist.ifBlank { "YouTube Music" }
        currentThumbnailUrl = thumbnailUrl
        currentIsPlaying = isPlaying
        currentPositionMs = positionMs

        if (isPlaying) {
            acquireLocks(context)
            PlaybackService.keepAlive(context.applicationContext)
        } else {
            releaseLocks()
        }

        createChannel(context)
        val session = getOrCreateSession(context)

        // Update MediaSession PlaybackState (this updates seekbar in System UI smoothly)
        val stateInt = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = (
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SEEK_TO
        )
        val pbState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(stateInt, positionMs, 1.0f)
            .build()
        session.setPlaybackState(pbState)

        // Only rebuild notification when metadata or play/pause state changes.
        // Calling notify() on 500ms time ticks constantly recreates RemoteViews and drops clicks!
        if (!metadataChanged && cachedArtworkUrl == thumbnailUrl) {
            return
        }

        // Update MediaSession Metadata
        val metaBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "VibeOn")
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)

        if (cachedArtworkUrl == thumbnailUrl && cachedArtworkBitmap != null) {
            metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cachedArtworkBitmap)
            metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, cachedArtworkBitmap)
        }
        session.setMetadata(metaBuilder.build())

        // Post notification immediately with current artwork
        val notif = buildNotification(context, cachedArtworkBitmap)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            nm.notify(NOTIFICATION_ID, notif)
        } catch (_: Exception) {}

        // Download artwork asynchronously if new
        if (thumbnailUrl.isNotBlank() && cachedArtworkUrl != thumbnailUrl) {
            imageExecutor.execute {
                val bmp = fetchBitmap(thumbnailUrl)
                if (bmp != null) {
                    cachedArtworkUrl = thumbnailUrl
                    cachedArtworkBitmap = bmp
                    mainHandler.post {
                        if (currentThumbnailUrl == thumbnailUrl) {
                            val updatedMeta = MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "VibeOn")
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentVideoId)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bmp)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bmp)
                                .build()
                            session.setMetadata(updatedMeta)

                            val updatedNotif = buildNotification(context, bmp)
                            try {
                                nm.notify(NOTIFICATION_ID, updatedNotif)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    fun buildNotification(context: Context, artwork: Bitmap?): Notification {
        createChannel(context)
        val session = getOrCreateSession(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, MediaControlReceiver::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleAction = if (currentIsPlaying) ACTION_PAUSE else ACTION_PLAY
        val toggleIntent = PendingIntent.getBroadcast(
            context,
            12,
            Intent(context, MediaControlReceiver::class.java).setAction(toggleAction),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getBroadcast(
            context,
            13,
            Intent(context, MediaControlReceiver::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getBroadcast(
            context,
            14,
            Intent(context, MediaControlReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Authentic MediaStyle notification (compact view has Prev [0], Play/Pause [1], Next [2])
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(session.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopIntent)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setStyle(mediaStyle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle.ifBlank { "VibeOn" })
            .setContentText(currentArtist.ifBlank { "YouTube Music" })
            .setSubText("VibeOn")
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(currentIsPlaying)
            .addAction(R.drawable.ic_skip_previous, "Previous", prevIntent)
            .addAction(
                if (currentIsPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (currentIsPlaying) "Pause" else "Play",
                toggleIntent
            )
            .addAction(R.drawable.ic_skip_next, "Next", nextIntent)

        if (artwork != null) {
            builder.setLargeIcon(artwork)
        }

        return builder.build()
    }

    fun dismiss(context: Context) {
        releaseLocks()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            nm.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
        mediaSession?.let {
            val pbState = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f)
                .build()
            it.setPlaybackState(pbState)
        }
    }

    private fun targetWebView(): BraveliteWebView? {
        return BraveliteWebView.activePlayerView ?: BraveliteWebView.browseWebView
    }

    fun handlePlay() {
        mainHandler.post {
            targetWebView()?.play()
            PlaybackService.instance()?.let { svc ->
                svc.playerInstance()?.let { p -> if (!p.isPlaying) p.play() }
            }
        }
    }

    fun handlePause() {
        mainHandler.post {
            targetWebView()?.pause()
            PlaybackService.instance()?.let { svc ->
                svc.playerInstance()?.let { p -> if (p.isPlaying) p.pause() }
            }
        }
    }

    fun handleNext() {
        mainHandler.post {
            targetWebView()?.nextTrack()
            PlaybackService.pipNext()
        }
    }

    fun handlePrev() {
        mainHandler.post {
            targetWebView()?.prevTrack()
            PlaybackService.pipPrev()
        }
    }

    fun handleStop(context: Context) {
        mainHandler.post {
            targetWebView()?.pause()
            PlaybackService.stopAudio(context)
            dismiss(context)
        }
    }

    fun handleSeekTo(pos: Long) {
        mainHandler.post {
            val target = targetWebView()
            target?.seekTo((pos / 1000f))
            PlaybackService.seek(target?.context ?: return@post, pos)
        }
    }

    private fun fetchBitmap(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.connect()
            val input = conn.inputStream
            val original = BitmapFactory.decodeStream(input)
            input.close()
            conn.disconnect()

            if (original != null && (original.width > 512 || original.height > 512)) {
                Bitmap.createScaledBitmap(original, 512, 512, true)
            } else {
                original
            }
        } catch (_: Exception) {
            null
        }
    }
}
