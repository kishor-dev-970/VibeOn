package com.example.socialmusic

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.facebook.react.bridge.ReactContext
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

class BraveliteWebView(context: Context) : WebView(context) {

    data class PlaybackState(
        val videoId: String,
        val currentTime: Float,
        val paused: Boolean,
        val title: String,
        val ended: Boolean,
        val error: Boolean
    )

    @Volatile
    var lastPlaybackState: PlaybackState? = null
        private set

    var onPlaybackStateListener: ((PlaybackState) -> Unit)? = null
        set(value) {
            field = value
            lastPlaybackState?.let { notifyState(it) }
        }

    val blockedCount = AtomicInteger(0)

    private var customViewContainer: android.widget.FrameLayout? = null
    private var customViewCallback: android.webkit.WebChromeClient.CustomViewCallback? = null
    private var fullscreenActivity: Activity? = null
    var useDesktop: Boolean = false

    private val AUDIO_ITAGS = setOf(
        "139", "140", "141", "249", "250", "251",
        "256", "257", "258", "327", "599", "600"
    )

    private fun captureAudioStream(uri: android.net.Uri) {
        val host = uri.host ?: return
        if (!host.contains("googlevideo.com")) return
        if (!uri.path.orEmpty().contains("videoplayback")) return
        val itag = uri.getQueryParameter("itag") ?: return
        val mime = uri.getQueryParameter("mime") ?: ""
        if (!mime.startsWith("audio") && itag !in AUDIO_ITAGS) return

        val builder = StringBuilder("https://").append(host).append(uri.path).append('?')
        var first = true
        for (key in uri.queryParameterNames) {
            if (key == "range") continue
            val value = uri.getQueryParameter(key) ?: continue
            if (!first) builder.append('&')
            first = false
            builder.append(key).append('=').append(android.net.Uri.encode(value))
        }
        if (first) return
        val url = builder.toString() + "&ratebypass=yes"
        YouTubeAudioCapture.store(url, uri.getQueryParameter("id"))
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // This WebView instance itself is the React-managed view (no separate inner WebView
    // is held as a child, which avoids Fabric view-tree desync on tab switches).

    @Volatile
    private var currentVideoId: String? = null

    @Volatile
    private var browseUrl: String? = null

    private var watchFallbackLoaded = false
    private var adblockJs: String? = null
    private var hideAdsCssJs: String? = null

    @Volatile
    private var hasError = false

    private val STATE_HOOK_JS =
        "(function(){if(window.__blStateHook)return;window.__blStateHook=true;" +
            "setInterval(function(){try{" +
            "var v=document.querySelector('video');" +
            "var m=(location.href||'').match(/[?&]v=([A-Za-z0-9_-]{11})/);" +
            "var txt=document.body?document.body.innerText:'';" +
            "var err=!!(document.querySelector('.ytp-error,.ytp-error-message')||" +
            "(txt&&/Video player configuration error|This video is unavailable|" +
            "Playback on other apps disabled|An error occurred/i.test(txt)));" +
            "MusicAppBridge.onPlaybackState(m?m[1]:'',v?v.currentTime:-1," +
            "(v?v.paused:true),(document.title||'').replace(/\\s*[-|]\\s*YouTube.*$/i,'')," +
            "v?!!v.ended:false,err);}catch(e){}},500);})();"

    companion object {
        private const val TAG = "BraveliteWebView"

        var activeFullscreen: BraveliteWebView? = null
        var browseWebView: BraveliteWebView? = null

        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        private fun embedUrl(videoId: String, autoplay: Boolean) =
            "https://www.youtube.com/embed/${videoId}?autoplay=${if (autoplay) 1 else 0}" +
                "&playsinline=1&rel=0&modestbranding=1&color=white"

        private fun watchUrl(videoId: String) =
            "https://www.youtube.com/watch?v=${videoId}&autoplay=1"
    }

    init {
        setupWebView()
        loadAdblockAssets()
    }

    private fun setupWebView() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "WebView must be created on the main thread" }

        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(0xFF0F0F0F.toInt())
        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = if (useDesktop) DESKTOP_UA else MOBILE_UA
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, false)
        }
        addJavascriptInterface(Bridge(), "MusicAppBridge")
        webViewClient = Client()
        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onShowCustomView(view: android.view.View?, callback: android.webkit.WebChromeClient.CustomViewCallback?) {
                if (view == null) return
                customViewCallback = callback
                activeFullscreen = this@BraveliteWebView
                if (customViewContainer == null) {
                    customViewContainer = android.widget.FrameLayout(context).apply {
                        setBackgroundColor(0xFF000000.toInt())
                    }
                }
                customViewContainer?.removeAllViews()
                customViewContainer?.addView(
                    view,
                    android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                // Add to the activity window content so the video SurfaceView composites
                // correctly (reparenting into the WebView's own parent caused a black screen).
                val activity = (context as? com.facebook.react.bridge.ReactContext)?.currentActivity
                if (activity != null) {
                    fullscreenActivity = activity
                    // Force landscape while in fullscreen video.
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    val lp = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    activity.runOnUiThread { activity.addContentView(customViewContainer, lp) }
                } else {
                    addView(
                        customViewContainer,
                        android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    customViewContainer?.bringToFront()
                }
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }
    }

    private fun loadAdblockAssets() {
        try {
            context.assets.open("youtube.js").bufferedReader().use { adblockJs = it.readText() }
            val css = context.assets.open("youtube.css").bufferedReader().use { it.readText() }
            val escaped = StringBuilder("\"")
            for (ch in css) {
                when (ch) {
                    '\\' -> escaped.append("\\\\")
                    '"' -> escaped.append("\\\"")
                    '\n' -> escaped.append("\\n")
                    '\r' -> escaped.append("\\r")
                    else -> escaped.append(ch)
                }
            }
            escaped.append("\"")
            hideAdsCssJs = "(function(){try{" +
                "if(document.getElementById('bl-css'))return;" +
                "var s=document.createElement('style');s.id='bl-css';" +
                "s.textContent=" + escaped + ";" +
                "(document.head||document.documentElement).appendChild(s);}catch(e){}})()"
        } catch (_: Exception) {
            hideAdsCssJs = null
        }
    }

    private fun injectAdblock() {
        hideAdsCssJs?.let { evaluateJavascript(it, null) }
        val baseJs = adblockJs ?: ""
        val script =
            ";window.__braveliteBlocked=" + blockedCount.get() + ";" +
                STATE_HOOK_JS +
                baseJs
        evaluateJavascript(script, null)
    }

    fun loadBrowseUrl(url: String) {
        Log.d(TAG, "loadBrowseUrl $url")
        browseUrl = url
        browseWebView = this
        currentVideoId = null
        hasError = false
        watchFallbackLoaded = false
        lastPlaybackState = null
        this.loadUrl(url)
    }

    fun loadVideo(videoId: String, autoplay: Boolean) {
        if (currentVideoId == videoId && !hasError) return
        Log.d(TAG, "loadVideo $videoId autoplay=$autoplay")
        browseUrl = null
        currentVideoId = videoId
        hasError = false
        watchFallbackLoaded = false
        lastPlaybackState = null
        // YouTube embeds require an HTTP Referer (error 153 otherwise); a direct
        // top-level WebView navigation sends none, so add one explicitly.
        this.loadUrl(
            embedUrl(videoId, autoplay),
            mapOf("Referer" to "https://www.youtube-nocookie.com/")
        )
    }

    fun loadWatchFallback() {
        val id = currentVideoId ?: return
        browseUrl = null
        if (watchFallbackLoaded) return
        watchFallbackLoaded = true
        this.loadUrl(watchUrl(id))
    }

    fun play() {
        evaluate("try{var v=document.querySelector('video');if(v)v.play();}catch(e){}")
    }

    fun pause() {
        evaluate("try{var v=document.querySelector('video');if(v)v.pause();}catch(e){}")
    }

    fun seekTo(seconds: Float) {
        evaluate("try{var v=document.querySelector('video');if(v&&isFinite(v.duration))v.currentTime=$seconds;}catch(e){}")
    }

    fun stop() {
        watchFallbackLoaded = true
        evaluate("try{var v=document.querySelector('video');if(v){v.pause();v.src='';}}catch(e){}")
        this.loadUrl("about:blank")
    }

    fun currentPlayingId(): String? = currentVideoId

    private fun evaluate(js: String) {
        mainHandler.post {
            try {
                evaluateJavascript(js, null)
            } catch (_: Exception) {
            }
        }
    }

    private fun notifyState(state: PlaybackState) {
        try {
            onPlaybackStateListener?.invoke(state)
        } catch (_: Exception) {
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onPlaybackState(
            videoId: String?,
            t: Double,
            paused: Boolean,
            title: String?,
            ended: Boolean,
            error: Boolean
        ) {
            mainHandler.post {
                if (error) hasError = true
                val st = PlaybackState(
                    videoId ?: "",
                    t.toFloat(),
                    paused,
                    title ?: "",
                    ended,
                    error
                )
                lastPlaybackState = st
                notifyState(st)
            }
        }
    }

    private inner class Client : WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            if (BraveliteAdBlocker.isBlocked(request.url)) {
                blockedCount.incrementAndGet()
                Log.d(TAG, "blocked ${request.url}")
                return WebResourceResponse(
                    "text/plain",
                    "utf-8",
                    ByteArrayInputStream(ByteArray(0))
                )
            }
            captureAudioStream(request.url)
            return null
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme?.lowercase()
            return scheme != "http" && scheme != "https"
        }

        override fun onPageFinished(view: WebView, url: String?) {
            Log.d(TAG, "page finished: $url")
            injectAdblock()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                Log.e(TAG, "main frame error ${error.errorCode} ${error.description} for ${request.url}")
            }
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            Log.w(TAG, "render process gone; reloading")
            mainHandler.post {
                val id = currentVideoId
                if (browseUrl != null) {
                    this@BraveliteWebView.loadUrl(browseUrl!!)
                } else if (id != null && !watchFallbackLoaded) {
                    this@BraveliteWebView.loadUrl(
                        embedUrl(id, true),
                        mapOf("Referer" to "https://www.youtube-nocookie.com/")
                    )
                } else {
                    this@BraveliteWebView.loadUrl("about:blank")
                }
            }
            return true
        }
    }

    fun exitFullscreen(): Boolean {
        if (customViewContainer != null && (customViewContainer?.childCount ?: 0) > 0) {
            mainHandler.post { hideCustomView() }
            return true
        }
        return false
    }

    private fun hideCustomView() {
        customViewContainer?.let { (it.parent as? ViewGroup)?.removeView(it) }
        customViewContainer?.removeAllViews()
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        activeFullscreen = null
        fullscreenActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        fullscreenActivity = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (activeFullscreen === this) activeFullscreen = null
        if (browseWebView === this) browseWebView = null
    }

    fun pauseVideo() {
        mainHandler.post {
            try {
                evaluateJavascript(
                    "javascript:(function(){var v=document.querySelector('video');if(v)v.pause();})();",
                    null
                )
            } catch (_: Exception) {
            }
        }
    }
}
