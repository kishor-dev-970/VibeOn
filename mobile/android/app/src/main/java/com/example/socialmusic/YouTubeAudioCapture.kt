package com.example.socialmusic

import java.util.LinkedHashMap

object YouTubeAudioCapture {

    private val capturedAudios = LinkedHashMap<String, String>()

    @Synchronized
    fun store(url: String, videoId: String?) {
        capturedAudios[""] = url
        videoId?.let { id ->
            capturedAudios[id.take(11)] = url
            if (id.length > 11) capturedAudios[id] = url
        }
        while (capturedAudios.size > 8) {
            val firstKey = capturedAudios.keys.firstOrNull() ?: break
            capturedAudios.remove(firstKey)
        }
    }

    @Synchronized
    fun capturedFor(videoId: String?): String? {
        if (videoId.isNullOrBlank()) return capturedAudios[""]
        return capturedAudios[videoId] ?: capturedAudios[videoId.take(11)]
    }

    @Synchronized
    fun latest(): String? = capturedAudios[""]

    @Synchronized
    fun hasCaptured(): Boolean = capturedAudios.isNotEmpty()

    @Synchronized
    fun clear() {
        capturedAudios.clear()
    }
}
