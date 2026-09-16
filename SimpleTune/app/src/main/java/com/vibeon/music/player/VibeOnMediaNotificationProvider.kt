package com.vibeon.music.player

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.vibeon.music.R

/**
 * The default media3 provider only renders the "next"/"previous" buttons when the underlying
 * ExoPlayer timeline has more than one item. This app plays a single item and manages the queue
 * itself, so we force the three standard transport buttons to always be shown. Tapping them sends
 * the corresponding media key events, which [PlaybackService] routes to PlaybackManager.
 */
class VibeOnMediaNotificationProvider(
    context: Context,
) : DefaultMediaNotificationProvider(context) {

    override fun getMediaButtons(
        mediaSession: MediaSession,
        availableMediaButtonCommands: Player.Commands,
        customActionButtons: ImmutableList<CommandButton>,
        isPlaying: Boolean,
    ): ImmutableList<CommandButton> {
        return ImmutableList.of(
            transportButton(R.drawable.ic_skip_previous, "Previous", Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM),
            transportButton(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                Player.COMMAND_PLAY_PAUSE,
            ),
            transportButton(R.drawable.ic_skip_next, "Next", Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM),
        )
    }

    private fun transportButton(iconResId: Int, displayName: CharSequence, command: Int): CommandButton {
        return CommandButton.Builder()
            .setIconResId(iconResId)
            .setDisplayName(displayName)
            .setPlayerCommand(command)
            .build()
    }
}