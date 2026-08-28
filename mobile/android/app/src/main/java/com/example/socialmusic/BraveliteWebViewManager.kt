package com.example.socialmusic

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter

class BraveliteWebViewManager : SimpleViewManager<BraveliteWebView>() {

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(reactContext: ThemedReactContext): BraveliteWebView =
        BraveliteWebView(reactContext)

    @ReactProp(name = "videoId")
    fun setVideoId(view: BraveliteWebView, videoId: String) {
        if (videoId.isNotEmpty()) view.loadVideo(videoId, true)
    }

    override fun getCommandsMap(): Map<String, Int> = mapOf(
        "loadVideo" to COMMAND_LOAD_VIDEO,
        "loadWatch" to COMMAND_LOAD_WATCH,
        "play" to COMMAND_PLAY,
        "pause" to COMMAND_PAUSE,
        "seekTo" to COMMAND_SEEK_TO,
        "stop" to COMMAND_STOP
    )

    override fun receiveCommand(view: BraveliteWebView, commandId: Int, args: ReadableArray?) {
        when (commandId) {
            COMMAND_LOAD_VIDEO -> view.loadVideo(
                args?.getString(0) ?: "",
                args?.getBoolean(1) ?: true
            )
            COMMAND_LOAD_WATCH -> view.loadWatchFallback()
            COMMAND_PLAY -> view.play()
            COMMAND_PAUSE -> view.pause()
            COMMAND_SEEK_TO -> view.seekTo(args?.getDouble(0)?.toFloat() ?: 0f)
            COMMAND_STOP -> view.stop()
        }
    }

    override fun addEventEmitters(reactContext: ThemedReactContext, view: BraveliteWebView) {
        view.onPlaybackStateListener = { state ->
            val payload = Arguments.createMap().apply {
                putString("videoId", state.videoId)
                putDouble("currentTime", state.currentTime.toDouble())
                putBoolean("paused", state.paused)
                putString("title", state.title)
                putBoolean("ended", state.ended)
                putBoolean("error", state.error)
            }
            reactContext.getJSModule(RCTEventEmitter::class.java)
                .receiveEvent(view.id, EVENT_PLAYBACK_STATE, payload)
        }
    }

    companion object {
        const val REACT_CLASS = "BraveliteYouTubeView"
        private const val EVENT_PLAYBACK_STATE = "topPlaybackState"
        private const val COMMAND_LOAD_VIDEO = 1
        private const val COMMAND_LOAD_WATCH = 2
        private const val COMMAND_PLAY = 3
        private const val COMMAND_PAUSE = 4
        private const val COMMAND_SEEK_TO = 5
        private const val COMMAND_STOP = 6
    }
}