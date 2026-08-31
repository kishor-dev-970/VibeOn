package com.example.socialmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import com.facebook.react.ReactApplication
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "socialmusic_playback"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_AUDIO = "com.example.socialmusic.action.START_AUDIO"
        private const val ACTION_START_CAPTURED = "com.example.socialmusic.action.START_CAPTURED"
        private const val ACTION_TOGGLE = "com.example.socialmusic.action.TOGGLE"
        const val ACTION_STOP_AUDIO = "com.example.socialmusic.action.STOP_AUDIO"
        private const val ACTION_IDLE = "com.example.socialmusic.action.IDLE"
        private const val ACTION_SEEK = "com.example.socialmusic.action.SEEK"
        const val ACTION_PIP_NEXT = "com.example.socialmusic.action.PIP_NEXT"
        const val ACTION_PIP_PREV = "com.example.socialmusic.action.PIP_PREV"
        private const val EXTRA_VIDEO_ID = "videoId"
        private const val EXTRA_POSITION_MS = "positionMs"
        private const val EXTRA_TITLE = "title"
        private const val INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        class Snapshot(
            @Volatile var videoId: String,
            @Volatile var title: String,
            @Volatile var playing: Boolean,
            @Volatile var positionMs: Long
        )

        @Volatile
        private var snapshot: Snapshot? = null

        private var instanceRef: WeakReference<PlaybackService>? = null

        @Volatile
        private var pendingHandoff: Intent? = null

        fun snapshot(): Snapshot? = snapshot

        fun livePositionMs(): Long {
            val svc = instanceRef?.get() ?: return snapshot?.positionMs ?: -1L
            return svc.currentPositionSafe()
        }

        fun startAudio(context: Context, videoId: String, positionMs: Long, title: String) {
            val intent = Intent(context, PlaybackService::class.java)
                .setAction(ACTION_START_AUDIO)
                .putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_POSITION_MS, positionMs)
                .putExtra(EXTRA_TITLE, title)
            deliverSafely(context, intent)
        }

        fun startCaptured(context: Context, videoId: String, title: String) {
            val intent = Intent(context, PlaybackService::class.java)
                .setAction(ACTION_START_CAPTURED)
                .putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title)
            deliverSafely(context, intent)
        }

        fun startIdle(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, PlaybackService::class.java).setAction(ACTION_IDLE)
                )
            } catch (_: Exception) {
            }
        }

        fun stopAudio(context: Context) {
            deliverSafely(
                context,
                Intent(context, PlaybackService::class.java).setAction(ACTION_STOP_AUDIO)
            )
        }

        fun toggle(context: Context) {
            deliverSafely(
                context,
                Intent(context, PlaybackService::class.java).setAction(ACTION_TOGGLE)
            )
        }

        fun seek(context: Context, ms: Long) {
            val svc = instanceRef?.get()
            if (svc != null && svc.foregroundReady) {
                svc.deliver(
                    Intent(ACTION_SEEK).putExtra(EXTRA_POSITION_MS, ms)
                )
            }
        }

        private fun deliverSafely(context: Context, intent: Intent) {
            val svc = instanceRef?.get()
            if (svc != null && svc.foregroundReady) {
                svc.deliver(intent)
                return
            }
            // Control-only actions (STOP/TOGGLE/SEEK) must NEVER start a fresh
            // service. startForegroundService() requires a matching startForeground()
            // within 5s, which those handlers don't call, and Android 12+ forbids
            // starting foreground services from the background. Doing so throws
            // RemoteServiceException / ForegroundServiceStartNotAllowedException and
            // crashes the ENTIRE app. With no live service there is nothing to control.
            if (intent.action == ACTION_START_AUDIO || intent.action == ACTION_START_CAPTURED) {
                try {
                    ContextCompat.startForegroundService(context, intent)
                } catch (_: Exception) {
                    synchronized(Companion::class.java) { pendingHandoff = intent }
                }
            }
        }

        fun flushPending() {
            val intent = synchronized(Companion::class.java) {
                val p = pendingHandoff
                pendingHandoff = null
                p
            } ?: return
            val svc = instanceRef?.get()
            if (svc != null && svc.foregroundReady) {
                svc.deliver(intent)
            } else {
                synchronized(Companion::class.java) { pendingHandoff = intent }
            }
        }
    }

    @Volatile
    var foregroundReady: Boolean = false
        private set

    fun deliver(intent: Intent) {
        mainHandler.post { handleCommand(intent) }
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentVideoId: String? = null
    private var displayTitle: String = ""

    private var candidates: List<String> = emptyList()
    private var candidateIndex = 0

    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            if (player != null && player!!.isPlaying) {
                publishSnapshot(playing = true, positionMs = currentPositionSafe())
            }
            mainHandler.postDelayed(this, 500L)
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val newPipeInitialized = AtomicBoolean(false)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", MOBILE_UA)
                    .build()
            )
        }
        .build()

    private val fastClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", MOBILE_UA)
                    .build()
            )
        }
        .build()

    @Volatile
    private var resolving = false

    private val resolveTimeoutRunnable = Runnable {
        if (resolving) {
            resolving = false
            failWith(getString(R.string.notify_timeout))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        instanceRef = WeakReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        handleCommand(intent)

    private fun handleCommand(intent: Intent?): Int {
        when (intent?.action) {
            ACTION_START_AUDIO -> {
                val id = intent.getStringExtra(EXTRA_VIDEO_ID)
                if (id.isNullOrBlank()) return stopQuietly()
                val pos = intent.getLongExtra(EXTRA_POSITION_MS, 0L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)

                val alreadyOnSameVideo = currentVideoId == id && player != null &&
                    (player!!.isPlaying || player!!.playWhenReady)
                if (alreadyOnSameVideo) return START_STICKY

                goForeground(getString(R.string.notification_loading))
                resolving = true
                mainHandler.removeCallbacks(resolveTimeoutRunnable)
                mainHandler.postDelayed(resolveTimeoutRunnable, 15000L)
                resolveAndPlay(id, pos.coerceAtLeast(0L), title)
            }

            ACTION_START_CAPTURED -> {
                val id = intent.getStringExtra(EXTRA_VIDEO_ID)
                if (id.isNullOrBlank()) return stopQuietly()
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
                val url = YouTubeAudioCapture.capturedFor(id)
                if (url.isNullOrBlank()) {
                    goForeground(title)
                    failWith(getString(R.string.notify_id_mismatch))
                    return START_STICKY
                }
                goForeground(title)
                currentVideoId = id
                displayTitle = title
                candidates = listOf(url)
                candidateIndex = 0
                resolving = false
                mainHandler.removeCallbacks(resolveTimeoutRunnable)
                beginPlayback(listOf(url), 0L, title, id)
            }

            ACTION_TOGGLE -> player?.let { if (it.isPlaying) it.pause() else it.play() }

            ACTION_SEEK -> {
                val ms = intent.getLongExtra(EXTRA_POSITION_MS, 0L)
                player?.seekTo(ms)
            }

            ACTION_IDLE -> {
                goForeground(getString(R.string.app_name))
                return START_STICKY
            }

            ACTION_STOP_AUDIO -> return stopQuietly()

            ACTION_PIP_NEXT -> {
                VibEvents.emit(this, "onPipCommand") { it.putString("command", "next") }
            }

            ACTION_PIP_PREV -> {
                VibEvents.emit(this, "onPipCommand") { it.putString("command", "prev") }
            }

            else -> return START_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releasePlayerNow()
        if (instanceRef?.get() === this) instanceRef = null
        foregroundReady = false
        super.onDestroy()
    }

    fun currentPositionSafe(): Long = player?.currentPosition ?: snapshot?.positionMs ?: -1L

    private fun stopQuietly(): Int {
        resolving = false
        mainHandler.removeCallbacks(resolveTimeoutRunnable)
        mainHandler.removeCallbacks(positionUpdateRunnable)
        val id = currentVideoId
        val title = displayTitle
        releasePlayerNow()
        snapshot = null
        currentVideoId = null
        YouTubeAudioCapture.clear()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (id != null) emitOnStopped(id, title)
        return START_NOT_STICKY
    }

    private fun failWith(message: String): Int {
        resolving = false
        mainHandler.removeCallbacks(resolveTimeoutRunnable)
        releasePlayerNow()
        val id = currentVideoId
        if (id != null) {
            emitOnStateChange(id, displayTitle, false, 0L, 0L)
        }
        snapshot = null
        goForeground(message)
        mainHandler.postDelayed({ stopSelf() }, 5000L)
        return START_NOT_STICKY
    }

    private fun releasePlayerNow() {
        player?.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
    }

    // ---------- stream resolution ----------

    private fun resolveAndPlay(videoId: String, positionMs: Long, fallbackTitle: String) {
        updateNotificationStage(fallbackTitle, getString(R.string.stage_session))
        executor.submit {
            val collected = LinkedHashSet<String>()
            var resolvedTitle = fallbackTitle

            // 1) Session-captured URL from the YouTube WebView tab.
            try {
                YouTubeAudioCapture.capturedFor(videoId)?.let { collected.add(it) }
            } catch (_: Exception) {
            }

            // 2) NewPipeExtractor
            try {
                mainHandler.post {
                    updateNotificationStage(
                        resolvedTitle,
                        getString(R.string.stage_newpipe)
                    )
                }
                if (newPipeInitialized.compareAndSet(false, true)) {
                    NewPipe.init(OkHttpDownloader(fastClient))
                }
                val info = StreamInfo.getInfo(
                    ServiceList.YouTube,
                    "https://www.youtube.com/watch?v=$videoId"
                )
                if (!info.name.isNullOrBlank()) resolvedTitle = info.name
                info.audioStreams
                    .filter { !it.content.isNullOrBlank() }
                    .sortedByDescending { it.averageBitrate }
                    .forEach { collected.add(it.content) }
            } catch (_: Exception) {
            }

            // 3) Direct InnerTube requests (multiple clients)
            if (collected.isEmpty()) {
                mainHandler.post {
                    updateNotificationStage(
                        resolvedTitle,
                        getString(R.string.stage_innertube)
                    )
                }
                for (client in innertubeClients()) {
                    try {
                        val url = innertubeAudioUrl(videoId, client) ?: continue
                        collected.add(url)
                        break
                    } catch (_: Exception) {
                    }
                }
            }

            // 4) Last resort: freshest captured stream even if ID didn't match
            if (collected.isEmpty()) {
                try {
                    YouTubeAudioCapture.latest()?.let { collected.add(it) }
                } catch (_: Exception) {
                }
            }

            if (collected.isEmpty()) {
                val msg = if (YouTubeAudioCapture.hasCaptured()) {
                    getString(R.string.notify_id_mismatch)
                } else {
                    getString(R.string.notify_extract_failed)
                }
                mainHandler.post { failWith(msg) }
            } else {
                mainHandler.post {
                    beginPlayback(collected.toList(), positionMs, resolvedTitle, videoId)
                }
            }
        }
    }

    private class InnertubeClient(val name: String, val version: String, val ua: String)

    private fun innertubeClients(): List<InnertubeClient> = listOf(
        InnertubeClient(
            "ANDROID",
            "19.09.37",
            "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip"
        ),
        InnertubeClient(
            "IOS",
            "19.29.1",
            "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X)"
        )
    )

    private fun innertubeAudioUrl(videoId: String, client: InnertubeClient): String? {
        val clientJson = JSONObject()
            .put("clientName", client.name)
            .put("clientVersion", client.version)
        if (client.name == "ANDROID") {
            clientJson.put("androidSdkVersion", 30)
        } else {
            clientJson.put("deviceModel", "iPhone16,2")
        }
        val payload = JSONObject()
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .put("context", JSONObject().put("client", clientJson))
        val req = okhttp3.Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_KEY")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .header("User-Agent", client.ua)
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val status = json.optJSONObject("playabilityStatus")?.optString("status")
            if (status != "OK") return null
            val formats = json.optJSONObject("streamingData")
                ?.optJSONArray("adaptiveFormats") ?: return null
            var best: String? = null
            var bestBps = -1
            for (i in 0 until formats.length()) {
                val f = formats.optJSONObject(i) ?: continue
                val mime = f.optString("mimeType")
                if (!mime.startsWith("audio/")) continue
                val url = f.optString("url")
                if (url.isBlank()) continue
                val bps = f.optInt("bitrate", 0)
                if (bps > bestBps) {
                    bestBps = bps
                    best = url
                }
            }
            return best
        }
    }

    // ---------- playback ----------

    private fun beginPlayback(
        urls: List<String>,
        positionMs: Long,
        title: String,
        videoId: String
    ) {
        resolving = false
        mainHandler.removeCallbacks(resolveTimeoutRunnable)
        candidates = urls
        candidateIndex = 0
        currentVideoId = videoId
        displayTitle = title
        playCandidate(positionMs)
    }

    private fun playCandidate(positionMs: Long) {
        if (candidateIndex >= candidates.size) {
            failWith(getString(R.string.notify_stream_failed))
            return
        }
        try {
            val url = candidates[candidateIndex]
            updateNotificationStage(
                displayTitle,
                getString(R.string.stage_player) + " " + (candidateIndex + 1) + "/" + candidates.size
            )

            if (player == null) {
                player = ExoPlayer.Builder(this)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true
                    )
                    .setHandleAudioBecomingNoisy(true)
                    .setWakeMode(C.WAKE_MODE_LOCAL)
                    .build()
                player!!.addListener(playerListener)
                // Active MediaSession powers the PiP play/pause control and the
                // system media hub.
                mediaSession = MediaSession.Builder(this, player!!).build()
            }

            try {
                player?.setPlaylistMetadata(
                    MediaMetadata.Builder()
                        .setTitle(displayTitle)
                        .setArtist(getString(R.string.app_name))
                        .build()
                )
            } catch (_: Exception) {
            }

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(MOBILE_UA)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))

            val source = ProgressiveMediaSource.Factory(httpFactory)
                .createMediaSource(
                    MediaItem.Builder()
                        .setUri(url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(displayTitle)
                                .setArtist(getString(R.string.app_name))
                                .build()
                        )
                        .build()
                )

            player!!.setMediaSource(source)
            player!!.seekTo(positionMs)
            player!!.prepare()
            player!!.playWhenReady = true

            publishSnapshot(playing = true, positionMs = positionMs)
            mainHandler.removeCallbacks(positionUpdateRunnable)
            mainHandler.post(positionUpdateRunnable)
        } catch (e: Exception) {
            candidateIndex++
            playCandidate(positionMs)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishSnapshot(playing = isPlaying, positionMs = currentPositionSafe())
            updateNotification(displayTitle)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    emitOnComplete()
                    mainHandler.post { stopQuietly() }
                }
                Player.STATE_READY -> {
                    publishSnapshot(playing = player?.isPlaying == true, positionMs = currentPositionSafe())
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            candidateIndex++
            val pos = currentPositionSafe().coerceAtLeast(0L)
            if (candidateIndex < candidates.size) {
                releasePlayerNow()
                playCandidate(pos)
            } else {
                mainHandler.post { failWith(getString(R.string.notify_stream_failed)) }
            }
        }
    }

    private fun publishSnapshot(playing: Boolean, positionMs: Long) {
        val id = currentVideoId ?: return
        val existing = snapshot
        if (existing != null && existing.videoId == id) {
            existing.playing = playing
            existing.positionMs = positionMs
        } else {
            snapshot = Snapshot(id, displayTitle, playing, positionMs)
        }
        val dur = player?.duration ?: 0L
        emitOnStateChange(id, displayTitle, playing, positionMs, if (dur <= 0) 0L else dur)
    }

    // ---------- notification ----------

    private fun goForeground(text: String) {
        foregroundReady = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(text, ""),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(text, ""))
        }
    }

    private fun updateNotification(title: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(title, getString(R.string.notification_text_audio)))
    }

    private fun updateNotificationStage(title: String, stage: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(title, stage))
    }

    private fun buildNotification(title: String, stage: String): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val toggleIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, PlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP_AUDIO),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val isPlaying = player?.isPlaying == true
        val toggleLabel =
            if (isPlaying) getString(R.string.action_pause) else getString(R.string.action_play)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title.ifBlank { getString(R.string.app_name) })
            .setContentText(stage.ifBlank { getString(R.string.notification_text_audio) })
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, toggleLabel, toggleIntent)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // ---------- React Native device event ----------

    private fun emitOnStateChange(
        videoId: String,
        title: String,
        playing: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        try {
            val app = applicationContext as? ReactApplication ?: return
            val reactContext: ReactContext = app.reactHost?.currentReactContext ?: return
            if (!reactContext.hasActiveReactInstance()) return
            val map = Arguments.createMap()
            map.putString("videoId", videoId)
            map.putString("title", title)
            map.putBoolean("playing", playing)
            map.putDouble("positionMs", positionMs.toDouble())
            map.putDouble("durationMs", durationMs.toDouble())
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("onStateChange", map)
        } catch (_: Exception) {
        }
    }

    private fun emitOnComplete() {
        try {
            val app = applicationContext as? ReactApplication ?: return
            val reactContext: ReactContext = app.reactHost?.currentReactContext ?: return
            if (!reactContext.hasActiveReactInstance()) return
            val id = currentVideoId ?: return
            val map = Arguments.createMap()
            map.putString("videoId", id)
            map.putString("title", displayTitle)
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("onPlaybackComplete", map)
        } catch (_: Exception) {
        }
    }

    private fun emitOnStopped(videoId: String, title: String) {
        VibEvents.emit(this, "onPlaybackStopped") { map ->
            map.putString("videoId", videoId)
            map.putString("title", title)
        }
    }

    private class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {
        @Throws(IOException::class)
        override fun execute(request: Request): Response {
            val method = request.httpMethod().uppercase()
            val body = if (method == "POST" || method == "PUT") {
                request.dataToSend()?.toRequestBody()
            } else {
                null
            }
            val builder = okhttp3.Request.Builder().url(request.url()).method(method, body)
            request.headers().forEach { (key, values) ->
                values.forEach { value -> builder.header(key, value) }
            }
            client.newCall(builder.build()).execute().use { resp ->
                val responseBody = resp.body?.string() ?: ""
                return Response(
                    resp.code,
                    resp.message,
                    resp.headers.toMultimap(),
                    responseBody,
                    resp.request.url.toString()
                )
            }
        }
    }
}

/** Emits React Native device events from any Android context that has a live React context. */
object VibEvents {
    fun emit(context: Context, eventName: String, build: (WritableMap) -> Unit) {
        try {
            val app = context.applicationContext as? ReactApplication ?: return
            val reactContext: ReactContext = app.reactHost?.currentReactContext ?: return
            if (!reactContext.hasActiveReactInstance()) return
            val map = Arguments.createMap()
            build(map)
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, map)
        } catch (_: Exception) {
        }
    }
}
