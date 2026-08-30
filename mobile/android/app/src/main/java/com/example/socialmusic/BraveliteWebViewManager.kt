package com.example.socialmusic

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.Event

class BraveliteWebViewManager : SimpleViewManager<BraveliteWebView>() {

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(reactContext: ThemedReactContext): BraveliteWebView =
        BraveliteWebView(reactContext)

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "topPlaybackState" to mapOf("registrationName" to "onTopPlaybackState")
        )
    }

    @ReactProp(name = "videoId")
    fun setVideoId(view: BraveliteWebView, videoId: String) {
        if (videoId.isNotEmpty()) view.loadVideo(videoId, true)
    }

    @ReactProp(name = "url")
    fun setUrl(view: BraveliteWebView, url: String) {
        if (url.isNotEmpty()) view.loadBrowseUrl(url)
    }

    override fun getCommandsMap(): Map<String, Int> = mapOf(
        "loadVideo" to COMMAND_LOAD_VIDEO,
        "loadWatch" to COMMAND_LOAD_WATCH,
        "play" to COMMAND_PLAY,
        "pause" to COMMAND_PAUSE,
        "seekTo" to COMMAND_SEEK_TO,
        "stop" to COMMAND_STOP,
        "loadUrl" to COMMAND_LOAD_URL
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
            COMMAND_LOAD_URL -> view.loadBrowseUrl(args?.getString(0) ?: "")
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
            val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
            val dispatcher = UIManagerHelper.getEventDispatcher(reactContext, surfaceId)
            dispatcher?.dispatchEvent(PlaybackStateEvent(surfaceId, view.id, payload))
        }
    }

    private class PlaybackStateEvent(
        surfaceId: Int,
        viewId: Int,
        private val data: WritableMap
    ) : Event<PlaybackStateEvent>(surfaceId, viewId) {
        override fun getEventName(): String = EVENT_PLAYBACK_STATE
        override fun getEventData(): WritableMap = data
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
        private const val COMMAND_LOAD_URL = 7
    }
}