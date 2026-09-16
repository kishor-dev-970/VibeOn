package com.vibeon.music.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vibeon.music.MainActivity
import com.vibeon.music.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    private var mediaSession: MediaSession? = null

    private val mediaButtonCallback = object : MediaSession.Callback {
        override fun onMediaButtonEvent(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            val keyCode = @Suppress("DEPRECATION") when {
                Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)?.keyCode
                else -> intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.keyCode
            }
            return when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    playbackManager.playNext()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    playbackManager.playPrevious()
                    true
                }
                else -> super.onMediaButtonEvent(mediaSession, controllerInfo, intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Tapping the notification body opens the app straight to the Now Playing screen.
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val session = MediaSession.Builder(this, playbackManager.player)
            .setCallback(mediaButtonCallback)
            .setSessionActivity(sessionActivity)
            .build()
        mediaSession = session
        val notificationProvider = VibeOnMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playbackManager.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}