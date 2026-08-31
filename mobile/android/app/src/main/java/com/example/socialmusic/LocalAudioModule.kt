package com.example.socialmusic

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class LocalAudioModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "LocalAudio"

    @ReactMethod
    fun play(videoId: String, title: String) {
        PlaybackService.startAudio(reactContext.applicationContext, videoId, 0L, title)
    }

    @ReactMethod
    fun playCaptured(videoId: String, title: String) {
        PlaybackService.startCaptured(reactContext.applicationContext, videoId, title)
    }

    @ReactMethod
    fun toggle() {
        PlaybackService.toggle(reactContext.applicationContext)
    }

    @ReactMethod
    fun stop() {
        PlaybackService.stopAudio(reactContext.applicationContext)
    }

    @ReactMethod
    fun seek(ms: Double) {
        PlaybackService.seek(reactContext.applicationContext, ms.toLong())
    }

    @ReactMethod
    fun isPlaying(promise: Promise) {
        promise.resolve(PlaybackService.snapshot()?.playing ?: false)
    }

    @ReactMethod
    fun getVersionName(promise: Promise) {
        try {
            val pm = reactContext.packageManager
            val info = pm.getPackageInfo(reactContext.packageName, 0)
            promise.resolve(info.versionName ?: "")
        } catch (e: Exception) {
            promise.resolve("")
        }
    }

    @ReactMethod
    fun getVersionCode(promise: Promise) {
        try {
            val pm = reactContext.packageManager
            val info = pm.getPackageInfo(reactContext.packageName, 0)
            promise.resolve(info.versionCode.toLong())
        } catch (e: Exception) {
            promise.resolve(0)
        }
    }

    @ReactMethod
    fun getPosition(promise: Promise) {
        promise.resolve(PlaybackService.livePositionMs())
    }

    @ReactMethod
    fun getVideoId(promise: Promise) {
        promise.resolve(PlaybackService.snapshot()?.videoId ?: "")
    }

    @ReactMethod
    fun enterPip(promise: Promise) {
        val entered = MainActivity.enterPipGlobal()
        promise.resolve(entered)
    }

    @ReactMethod
    fun setPipAutoEnter(enabled: Boolean) {
        MainActivity.setPipAutoEnterGlobal(enabled)
    }

    override fun initialize() {
        super.initialize()
    }
}
