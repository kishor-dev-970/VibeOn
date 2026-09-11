package com.vibeon.music.data.network

import com.vibeon.music.BuildConfig
import com.vibeon.music.data.settings.SettingsRepository
import com.vibeon.music.domain.model.Song
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ServerUser(
    val id: String = "",
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val name: String? = null,
    val code: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val user: ServerUser = ServerUser(),
)

@Serializable
data class ServerNowPlaying(
    val videoId: String? = null,
    val title: String? = null,
    val channel: String? = null,
    val thumbnailUrl: String? = null,
    val isPlaying: Boolean = false,
    val updatedAt: String? = null,
)

@Serializable
data class ServerFriend(
    val id: String = "",
    val name: String? = null,
    val code: String? = null,
    val avatarUrl: String? = null,
    val nowPlaying: ServerNowPlaying? = null,
    val lastActive: String? = null,
)

@Serializable
data class FriendsResponse(
    val friends: List<ServerFriend> = emptyList(),
)

@Serializable
data class ServerSongStat(
    val title: String? = null,
    val channel: String? = null,
    val thumbnailUrl: String? = null,
    val playCount: Long = 0,
)

@Serializable
data class ServerRecentPlay(
    val title: String? = null,
    val channel: String? = null,
    val thumbnailUrl: String? = null,
    val playedAt: String? = null,
)

@Serializable
data class ServerStats(
    val totalSongs: Long = 0,
    val totalPlays: Long = 0,
    val estimatedMinutes: Long = 0,
    val nowPlaying: ServerNowPlaying? = null,
    val songsListened: List<ServerSongStat> = emptyList(),
    val recentPlays: List<ServerRecentPlay> = emptyList(),
)

@Serializable
private data class PutSong(
    val videoId: String = "",
    val title: String = "",
    val channel: String = "",
    val thumbnailUrl: String? = null,
)

@Serializable
private data class NowPlayingPut(
    val song: PutSong,
    val isPlaying: Boolean,
)

@Singleton
class SocialApiClient @Inject constructor(
    private val settings: SettingsRepository,
) {
    companion object {
        val json: Json = Json { ignoreUnknownKeys = true }
    }

    private val baseUrl: String = BuildConfig.SOCIAL_API_URL
    private val io: CoroutineDispatcher = Dispatchers.IO

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

    private suspend fun tokenOrNull(): String? = settings.authToken.first()

    suspend fun signInWithName(firstName: String, lastName: String): Result<AuthResponse> =
        withContext(io) {
            runCatching {
                val body = buildJsonObject {
                    put("firstName", firstName)
                    put("lastName", lastName)
                }
                val response = http.post("$baseUrl/api/auth/signup") {
                    header("Accept", "application/json")
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
                if (!response.status.isSuccess()) {
                    error("Could not sign in (${response.status.value})")
                }
                json.decodeFromString<AuthResponse>(response.bodyAsText())
            }
        }

    suspend fun fetchFriendsActivity(): List<ServerFriend> =
        withContext(io) {
            val token = tokenOrNull() ?: return@withContext emptyList()
            runCatching {
                val response = http.get("$baseUrl/api/activity") {
                    bearerAuth(token)
                }
                if (!response.status.isSuccess()) {
                    return@runCatching emptyList()
                }
                json.decodeFromString<FriendsResponse>(response.bodyAsText()).friends
            }.getOrElse { emptyList() }
        }

    suspend fun fetchFriendStats(userId: String): ServerStats? =
        withContext(io) {
            val token = tokenOrNull() ?: return@withContext null
            runCatching {
                val response = http.get("$baseUrl/api/friends/$userId/stats") {
                    bearerAuth(token)
                }
                if (response.status != HttpStatusCode.OK) return@runCatching null
                json.decodeFromString<ServerStats>(response.body())
            }.getOrNull()
        }

    suspend fun updateNowPlaying(song: Song, isPlaying: Boolean) {
        val token = tokenOrNull() ?: return
        withContext(io) {
            runCatching {
                val channel = song.artists.joinToString(", ") { it.name }
                val body = json.encodeToString(
                    NowPlayingPut(
                        song = PutSong(
                            videoId = song.videoId,
                            title = song.title,
                            channel = channel,
                            thumbnailUrl = song.thumbnailUrl,
                        ),
                        isPlaying = isPlaying,
                    )
                )
                val response = http.put("$baseUrl/api/activity") {
                    bearerAuth(token)
                    header("Accept", "application/json")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                response.status == HttpStatusCode.OK // discard, networking is fire-and-forget
            }
        }
    }

    suspend fun clearNowPlaying() {
        val token = tokenOrNull() ?: return
        withContext(io) {
            runCatching {
                http.delete("$baseUrl/api/activity") {
                    bearerAuth(token)
                }
            }
        }
    }

    suspend fun latestReleaseTag(): String? =
        withContext(io) {
            runCatching {
                val response = http.get("https://api.github.com/repos/kishor-dev-970/VibeOn/releases/latest") {
                    header("Accept", "application/vnd.github+json")
                    header("User-Agent", "VibeOn")
                }
                if (!response.status.isSuccess()) return@runCatching null
                val body = json.decodeFromString<kotlinx.serialization.json.JsonObject>(response.bodyAsText())
                body["tag_name"]?.jsonPrimitive?.contentOrNull
            }.getOrNull()
        }
}