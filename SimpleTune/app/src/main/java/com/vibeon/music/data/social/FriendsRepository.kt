package com.vibeon.music.data.social

import com.vibeon.music.data.network.ServerFriend
import com.vibeon.music.data.network.ServerNowPlaying
import com.vibeon.music.data.network.ServerStats
import com.vibeon.music.data.network.SocialApiClient
import com.vibeon.music.domain.model.AppUser
import com.vibeon.music.domain.model.FriendActivity
import com.vibeon.music.domain.model.FriendStats
import com.vibeon.music.domain.model.NowPlaying
import com.vibeon.music.domain.model.RecentPlay
import com.vibeon.music.domain.model.Song
import com.vibeon.music.domain.model.SongStat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

@Singleton
class FriendsRepository @Inject constructor(
    private val api: SocialApiClient,
) {
    private val io: CoroutineDispatcher = Dispatchers.IO

    private val _friendActivity = MutableStateFlow<List<FriendActivity>>(emptyList())
    val friendActivity: StateFlow<List<FriendActivity>> = _friendActivity

    suspend fun fetchFriendsActivity(): List<FriendActivity> =
        withContext(io) {
            val friends = api.fetchFriendsActivity().mapNotNull { it.toDomain() }
            _friendActivity.value = friends
            friends
        }

    suspend fun fetchFriendStats(userId: String): FriendStats? =
        withContext(io) {
            api.fetchFriendStats(userId)?.toDomain()
        }

    suspend fun updateNowPlaying(song: Song, isPlaying: Boolean) {
        api.updateNowPlaying(song, isPlaying)
    }

    suspend fun clearNowPlaying() {
        api.clearNowPlaying()
    }

    suspend fun recordPlay(song: Song) {
        // Listening history is logged server-side on activity updates.
    }
}

private fun ServerFriend.toDomain(): FriendActivity? {
    if (id.isBlank()) return null
    return FriendActivity(
        user = AppUser(
            id = id,
            name = name ?: "",
            code = code ?: "",
            avatarUrl = avatarUrl,
            lastActive = lastActive?.let(::isoToMillis),
        ),
        nowPlaying = nowPlaying?.toDomain(),
    )
}

private fun ServerNowPlaying.toDomain(): NowPlaying = NowPlaying(
    isPlaying = isPlaying,
    songTitle = title,
    artistName = channel,
    artworkUrl = thumbnailUrl,
    videoId = videoId,
    updatedAt = updatedAt?.let(::isoToMillis),
)

private fun ServerStats.toDomain(): FriendStats = FriendStats(
    totalSongs = totalSongs,
    totalPlays = totalPlays,
    estimatedMinutes = estimatedMinutes,
    nowPlaying = nowPlaying?.toDomain(),
    topSongs = songsListened.map {
        SongStat(
            songTitle = it.title ?: "",
            artistName = it.channel,
            artworkUrl = it.thumbnailUrl,
            count = it.playCount,
        )
    },
    recentPlays = recentPlays.map {
        RecentPlay(
            songTitle = it.title ?: "",
            artistName = it.channel,
            artworkUrl = it.thumbnailUrl,
            playedAt = isoToMillis(it.playedAt) ?: 0L,
        )
    },
)

private fun isoToMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        Instant.parse(value).toEpochMilli()
    } catch (_: Exception) {
        null
    }
}