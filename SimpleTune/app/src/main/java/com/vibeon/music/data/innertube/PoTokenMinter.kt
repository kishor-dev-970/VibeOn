package com.vibeon.music.data.innertube

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vibeon.music.util.TubeJson
import com.vibeon.music.util.jsonObjectOrNull
import com.vibeon.music.util.str
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Proof-of-Origin token minter.
 *
 * YouTube now requires a PO token (Proof-of-Origin) on the ANDROID / MWEB InnerTube
 * clients before it will return stream URLs. We do not re-implement BotGuard.
 * Instead a hidden [WebView] loads a YouTube watch page, lets YouTube's own web
 * player mint the token, and we harvest the `pot=` value off the player's network
 * requests (fetch/XHR hook + resource-timing sweep), bound to the page's visitorData.
 */
@Singleton
class PoTokenMinter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        val TAG = "PoToken"
        const val SEED_VIDEO_ID = "dQw4w9WgXcQ"
        val SEED_URL = "https://m.youtube.com/watch?v=$SEED_VIDEO_ID"
        // PO tokens go stale; re-mint after this window.
        const val REFRESH_MS = 20 * 60 * 1000L
        const val MINT_POLL_MS = 600L
        const val MINT_TIMEOUT_MS = 45_000L
    }

    data class Minted(val pot: String, val visitorData: String)

    private val mutex = Mutex()
    private var cached: Minted? = null
    private var cachedAt = 0L
    private var webView: WebView? = null

    /** Installed on every page right after commit; wraps network APIs to record `pot=`. */
    private val hookScript = """
        (function () {
            if (window.__potHookInstalled) return;
            window.__potHookInstalled = true;
            window.__pot = null;
            function scan(u) {
                try {
                    if (!u) return;
                    var s = (typeof u === 'string') ? u : (u && u.url) ? u.url : '';
                    if (s.indexOf('pot=') === -1) return;
                    var m = s.match(/[?&]pot=([^&]+)/);
                    if (m && m[1]) window.__pot = window.__pot || decodeURIComponent(m[1]);
                } catch (e) {}
            }
            var of = window.fetch;
            if (of) window.fetch = function (input) { scan(input); return of.apply(this, arguments); };
            var oo = XMLHttpRequest.prototype.open;
            XMLHttpRequest.prototype.open = function (method, url) { scan(url); return oo.apply(this, arguments); };
        })();
    """

    private val pollScript = """
        (function () {
            try {
                var v = document.querySelector('video');
                if (v) { v.muted = true; if (v.paused) { var p = v.play(); if (p && p.catch) p.catch(function(){}); } }
            } catch (e) {}
            var pot = window.__pot || null;
            if (!pot) {
                try {
                    var es = performance.getEntriesByType('resource') || [];
                    for (var i = es.length - 1; i >= 0; i--) {
                        var n = es[i].name || '';
                        if (n.indexOf('pot=') === -1) continue;
                        var m = n.match(/[?&]pot=([^&]+)/);
                        if (m && m[1]) { pot = decodeURIComponent(m[1]); break; }
                    }
                } catch (e) {}
            }
            var d = (window.ytcfg && ytcfg.data_) ? ytcfg.data_ : {};
            var vd = d.VISITOR_DATA || (window.ytcfg && ytcfg.get ? ytcfg.get('VISITOR_DATA') : null);
            if (!pot) return null;
            return JSON.stringify({ pot: pot, visitorData: vd || null });
        })();
    """

    /**
     * Returns a cached valid token or mints a new one. Safe to call from any thread.
     */
    suspend fun get(): Minted? = mutex.withLock {
        if (cached != null && SystemClock.elapsedRealtime() - cachedAt < REFRESH_MS) {
            return cached
        }
        val minted = withContext(Dispatchers.Main) { mintOnMain() }
        if (minted != null) {
            cached = minted
            cachedAt = SystemClock.elapsedRealtime()
            Log.i(TAG, "minted pot=${minted.pot.take(12)}... vd=${minted.visitorData.take(12)}...")
        } else {
            Log.w(TAG, "mint failed")
        }
        cached
    }

    /** Forcibly clear the cached token (call when /player rejects a pot). */
    suspend fun invalidate() {
        mutex.withLock {
            cached = null
            cachedAt = 0L
        }
        withContext(Dispatchers.Main) { webView?.stopLoading() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun mintOnMain(): Minted? {
        val view = webView ?: createWebView().also { webView = it }
        runCatching { view.stopLoading() }
        view.loadUrl(SEED_URL)

        val deadline = SystemClock.uptimeMillis() + MINT_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            // Re-apply the hook on every pass: JS context is lost across navigations,
            // and m.youtube re-renders aggressively.
            view.evaluateJavascript(hookScript, null)
            delay(50)
            view.evaluateJavascript(hookScript, null)

            val result = evaluateOnMain(view, pollScript)
            if (result != null && result != "null") {
                val value = parseResult(result)
                if (value != null) {
                    view.stopLoading()
                    return value
                }
            }
            delay(MINT_POLL_MS)
        }
        view.stopLoading()
        return null
    }

    /** Runs [script] in [view] and suspends until the result callback fires. */
    private suspend fun evaluateOnMain(view: WebView, script: String): String? =
        withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { continuation ->
                runCatching {
                    view.evaluateJavascript(script) { continuation.resume(it) }
                }.onFailure {
                    continuation.resume(null)
                }
            }
        }

    /** evaluateJavascript returns a JSON-escaped string of our inner JSON — unwrap once. */
    private fun parseResult(escaped: String): Minted? =
        try {
            val outer = TubeJson.parseToJsonElement(escaped) as? JsonPrimitive
            val inner = outer?.contentOrNull ?: return null
            val obj = TubeJson.parseToJsonElement(inner).jsonObjectOrNull ?: return null
            val pot = obj.str("pot")?.takeIf { it.isNotBlank() } ?: return null
            val vd = obj.str("visitorData")?.takeIf { it.isNotBlank() } ?: return null
            Minted(pot, vd)
        } catch (e: Exception) {
            Log.w(TAG, "poll parse failed: $escaped", e)
            null
        }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView =
        WebView(context.applicationContext).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_DEFAULT
                javaScriptCanOpenWindowsAutomatically = false
            }
            webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail,
                ): Boolean = true
            }
            setBackgroundColor(0x00000000)
            alpha = 0f
            visibility = android.view.View.INVISIBLE
        }
}