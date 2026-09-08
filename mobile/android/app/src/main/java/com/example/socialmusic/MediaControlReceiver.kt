package com.example.socialmusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            MediaNotificationManager.ACTION_PLAY -> MediaNotificationManager.handlePlay()
            MediaNotificationManager.ACTION_PAUSE -> MediaNotificationManager.handlePause()
            MediaNotificationManager.ACTION_NEXT -> MediaNotificationManager.handleNext()
            MediaNotificationManager.ACTION_PREV -> MediaNotificationManager.handlePrev()
            MediaNotificationManager.ACTION_STOP -> MediaNotificationManager.handleStop(context)
        }
    }
}
