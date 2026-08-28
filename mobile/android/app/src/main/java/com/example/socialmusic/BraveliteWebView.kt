package com.example.socialmusic

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

class BraveliteWebView(context: Context) : android.widget.FrameLayout(context) {

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

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var webView: WebView? = null

    @Volatile
    private var currentVideoId: String? = null

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
            "var err=!!document.querySelector('.ytp-error,.ytp-error-message');" +
            "MusicAppBridge.onPlaybackState(m?m[1]:'',v?v.currentTime:-1," +
            "(v?v.paused:true),(document.title||'').replace(/\\s*[-|]\\s*YouTube.*$/i,'')," +
            "v?!!v.ended:false,err);}catch(e){}},500);})();"

    companion object {
        private const val TAG = "BraveliteWebView"

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
        getOrCreateWebView()
        loadAdblockAssets()
    }

    private fun getOrCreateWebView(): WebView {
        webView?.let { return it }
        check(Looper.myLooper() == Looper.getMainLooper()) { "WebView must be created on the main thread" }

        val wv = WebView(context)
        wv.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        wv.setBackgroundColor(0xFF0F0F0F.toInt())
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = MOBILE_UA
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, false)
        }
        wv.addJavascriptInterface(Bridge(), "MusicAppBridge")
        wv.webViewClient = Client()
        addView(wv)
        webView = wv
        return wv
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
        val wv = webView ?: return
        hideAdsCssJs?.let { wv.evaluateJavascript(it, null) }
        val baseJs = adblockJs ?: ""
        val script =
            ";window.__braveliteBlocked=" + blockedCount.get() + ";" +
                STATE_HOOK_JS +
                ";(" + baseJs + ")();"
        wv.evaluateJavascript(script, null)
    }

    fun loadVideo(videoId: String, autoplay: Boolean) {
        if (currentVideoId == videoId && !hasError) return
        Log.d(TAG, "loadVideo $videoId autoplay=$autoplay")
        currentVideoId = videoId
        hasError = false
        watchFallbackLoaded = false
        lastPlaybackState = null
        getOrCreateWebView().loadUrl(embedUrl(videoId, autoplay))
    }

    fun loadWatchFallback() {
        val id = currentVideoId ?: return
        if (watchFallbackLoaded) return
        watchFallbackLoaded = true
        getOrCreateWebView().loadUrl(watchUrl(id))
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
        getOrCreateWebView().loadUrl("about:blank")
    }

    fun currentPlayingId(): String? = currentVideoId

    private fun evaluate(js: String) {
        mainHandler.post {
            try {
                webView?.evaluateJavascript(js, null)
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

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            val dying = webView
            if (dying === view) {
                try {
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.destroy()
                } catch (_: Exception) {
                }
                webView = null
            }
            mainHandler.post {
                val id = currentVideoId
                if (id != null && !watchFallbackLoaded) getOrCreateWebView().loadUrl(embedUrl(id, true))
            }
            return true
        }
    }
}