package com.example.socialmusic

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import org.json.JSONArray

class TrendingModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "TrendingBridge"

    private val mainHandler = Handler(Looper.getMainLooper())

    @ReactMethod
    fun getTrendingIndia(promise: Promise) {
        val urls = listOf(
            "https://m.youtube.com/feed/music?gl=IN&hl=en-IN",
            "https://m.youtube.com/feed/trending?gl=IN&hl=en-IN"
        )
        scrapeFirst(urls, 0, promise)
    }

    @ReactMethod
    fun search(query: String, promise: Promise) {
        val q = Uri.encode(query)
        scrape("https://m.youtube.com/results?search_query=$q&gl=IN&hl=en-IN") { arr ->
            promise.resolve(arr)
        }
    }

    private fun scrapeFirst(urls: List<String>, index: Int, promise: Promise) {
        if (index >= urls.size) {
            promise.resolve(Arguments.createArray())
            return
        }
        scrape(urls[index]) { arr ->
            Log.d("TrendingBridge", "scrape '${urls[index]}' -> ${arr.size()} items")
            if (arr.size() > 0) promise.resolve(arr)
            else scrapeFirst(urls, index + 1, promise)
        }
    }

    private fun scrape(url: String, onResult: (WritableArray) -> Unit) {
        mainHandler.post {
            val activity = reactContext.currentActivity ?: reactContext.getCurrentActivity()
            if (activity == null) {
                onResult(Arguments.createArray())
                return@post
            }
            try {
                val wv = WebView(activity)
                wv.settings.javaScriptEnabled = true
                wv.settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                wv.layoutParams = ViewGroup.LayoutParams(1, 1)
                wv.visibility = android.view.View.GONE
                // Do NOT attach to decorView: a non-React view added to the activity window
                // corrupts React's Fabric view bookkeeping and crashes on tab switches.
                // A detached WebView still loads URLs and runs JS.

                var settled = false
                val cleanup = {
                    wv.destroy()
                }

                fun finish() {
                    if (settled) return
                    settled = true
                    wv.evaluateJavascript(SCRAPER_JS) { result ->
                        try {
                            onResult(parse(result))
                        } catch (e: Exception) {
                            onResult(Arguments.createArray())
                        } finally {
                            cleanup()
                        }
                    }
                }

                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("TrendingBridge", "onPageFinished $url")
                        mainHandler.postDelayed({ finish() }, 2500)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        Log.d("TrendingBridge", "onReceivedError $errorCode $description")
                        if (!settled) {
                            settled = true
                            onResult(Arguments.createArray())
                            cleanup()
                        }
                    }
                }

                mainHandler.postDelayed({
                    if (!settled) {
                        Log.d("TrendingBridge", "timeout, resolving empty")
                        settled = true
                        onResult(Arguments.createArray())
                        cleanup()
                    }
                }, 15000)

                Log.d("TrendingBridge", "scrape start url=$url")
                wv.loadUrl(url)
            } catch (e: Exception) {
                onResult(Arguments.createArray())
            }
        }
    }

    private fun parse(result: String?): WritableArray {
        val arr = Arguments.createArray()
        if (result.isNullOrBlank()) return arr
        try {
            val json = JSONArray(result)
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                val map = Arguments.createMap()
                map.putString("videoId", o.optString("videoId"))
                map.putString("title", o.optString("title"))
                map.putString("channel", o.optString("channel"))
                map.putString("thumbnailUrl", o.optString("thumbnailUrl"))
                arr.pushMap(map)
            }
        } catch (e: Exception) {
        }
        return arr
    }

    companion object {
        private const val SCRAPER_JS = """
(function(){
  function walk(node, out){
    if(!node || typeof node !== 'object') return;
    if(Array.isArray(node)){ for(var i=0;i<node.length;i++) walk(node[i], out); return; }
    if(typeof node.videoId === 'string' && /^[A-Za-z0-9_-]{11}$/.test(node.videoId)){
      var title='';
      if(node.title && node.title.runs) title = node.title.runs.map(function(r){return r.text||'';}).join('');
      else if(node.title && node.title.simpleText) title = node.title.simpleText;
      var author='';
      if(node.shortBylineText && node.shortBylineText.runs) author = node.shortBylineText.runs.map(function(r){return r.text||'';}).join('');
      else if(node.longBylineText && node.longBylineText.runs) author = node.longBylineText.runs.map(function(r){return r.text||'';}).join('');
      out.push({videoId:node.videoId, title:(title||'').trim(), channel:(author||'').trim(), thumbnailUrl:'https://i.ytimg.com/vi/'+node.videoId+'/mqdefault.jpg'});
      return;
    }
    for(var k in node){ if(Object.prototype.hasOwnProperty.call(node,k)) walk(node[k], out); }
  }
  var out=[];
  try { if(window.ytInitialData) walk(window.ytInitialData, out); } catch(e){}
  if(out.length===0){
    try {
      document.querySelectorAll('a[href*="watch?v="]').forEach(function(a){
        var m=(a.getAttribute('href')||'').match(/[?&]v=([^&]+)/);
        if(!m) return;
        out.push({videoId:m[1], title:((a.textContent)||a.getAttribute('title')||'').trim(), channel:'', thumbnailUrl:'https://i.ytimg.com/vi/'+m[1]+'/mqdefault.jpg'});
      });
    } catch(e){}
  }
  var seen={}; var res=[];
  out.forEach(function(o){ if(!seen[o.videoId]){ seen[o.videoId]=1; res.push(o); } });
  return res.slice(0,100);
})()
"""
    }
}
