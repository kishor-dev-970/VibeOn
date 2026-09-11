package com.vibeon.music.data.innertube

import android.util.Log
import com.vibeon.music.core.Constants
import com.vibeon.music.util.TubeJson
import com.vibeon.music.util.jsonObjectOrNull
import com.vibeon.music.util.obj
import com.vibeon.music.util.str
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InnerTubeClient @Inject constructor() {

    private val http = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        expectSuccess = false
        engine {
            config {
                retryOnConnectionFailure(true)
                followRedirects(true)
                followSslRedirects(true)
            }
        }
    }

    private fun musicContext(): JsonObject = buildJsonObject {
        put(
            "client",
            buildJsonObject {
                put("clientName", "WEB_REMIX")
                put("clientVersion", Constants.WEB_REMIX_CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
                put("timeZone", "UTC")
            }
        )
        put(
            "capabilities",
            buildJsonObject {
                put("inlinePlayback", true)
                put("silentPlayback", true)
            }
        )
    }

    private fun androidContext(visitorData: String?): JsonObject = buildJsonObject {
        put(
            "client",
            buildJsonObject {
                put("clientName", "ANDROID")
                put("clientVersion", Constants.ANDROID_CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
                put("androidSdkVersion", Constants.ANDROID_SDK_VERSION)
                visitorData?.let { put("visitorData", it) }
            }
        )
    }

    private suspend fun call(
        path: String,
        requestBody: JsonObject,
        music: Boolean = true,
    ): JsonObject? {
        val base = if (music) Constants.YOUTUBE_MUSIC_API else Constants.YOUTUBE_API
        val url = "$base/$path?key=${Constants.INNERTUBE_KEY}&prettyPrint=false"
        return try {
            val response = http.post(url) {
                header("User-Agent", if (music) Constants.DESKTOP_UA else Constants.ANDROID_UA)
                header("Origin", if (music) Constants.MUSIC_BASE else Constants.YOUTUBE_BASE)
                header("Referer", if (music) "${Constants.MUSIC_BASE}/" else "${Constants.YOUTUBE_BASE}/")
                requestBody.obj("context")?.obj("client")?.str("visitorData")
                    ?.let { header("X-Goog-Visitor-Id", it) }
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            val responseText = response.bodyAsText()
            Log.i("InnerTube", "call $path -> ${response.status.value}, len=${responseText.length}")
            if (!response.status.value.toString().startsWith("2")) {
                Log.w("InnerTube", "non-2xx for $path: ${responseText.take(300)}")
                return null
            }
            toJson(responseText)
        } catch (e: Exception) {
            Log.e("InnerTube", "call $path FAILED", e)
            null
        }
    }

    private fun toJson(raw: String): JsonObject? =
        try {
            TubeJson.parseToJsonElement(raw).jsonObjectOrNull
        } catch (e: Exception) {
            null
        }

    suspend fun search(query: String, params: String?): JsonObject? =
        call(
            "search",
            buildJsonObject {
                put("context", musicContext())
                put("query", query)
                params?.let { put("params", it) }
            },
            music = true,
        )

    suspend fun browse(browseId: String, params: String? = null): JsonObject? =
        call(
            "browse",
            buildJsonObject {
                put("context", musicContext())
                put("browseId", browseId)
                put("isUPA", false)
                params?.let { put("params", it) }
            },
            music = true,
        )

    suspend fun continuation(token: String): JsonObject? =
        call(
            "browse",
            buildJsonObject {
                put("context", musicContext())
                put("continuation", token)
            },
            music = true,
        )

    /**
 * Internal: executes a PO-token-minted /player request for [videoId].
 */
suspend fun player(videoId: String, minted: PoTokenMinter.Minted?): JsonObject? =
    call(
        "player",
        buildJsonObject {
            put("context", androidContext(minted?.visitorData))
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            if (minted != null) {
                put(
                    "serviceIntegrityDimensions",
                    buildJsonObject { put("poToken", minted.pot) }
                )
            }
        },
        music = false,
    )
}