package com.example.socialmusic

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class BraveliteFullscreenModule(context: ReactApplicationContext) :
    ReactContextBaseJavaModule(context) {

    override fun getName(): String = "BraveliteFullscreen"

    @ReactMethod
    fun exitFullscreen() {
        BraveliteWebView.activeFullscreen?.exitFullscreen()
    }

    @ReactMethod
    fun closeYouTubePlayer() {
        BraveliteWebView.activeFullscreen?.exitFullscreen()
        BraveliteWebView.browseWebView?.pauseVideo()
    }
}
