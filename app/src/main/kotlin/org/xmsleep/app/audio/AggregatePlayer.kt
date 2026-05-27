package org.xmsleep.app.audio

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi

@UnstableApi
class AggregatePlayer : SimpleBasePlayer(Looper.getMainLooper()) {

    private var isPlaying = false
    private var soundDescriptions: List<String> = emptyList()

    private val availableCommands: Player.Commands = Player.Commands.Builder()
        .add(Player.COMMAND_PLAY_PAUSE)
        .add(Player.COMMAND_STOP)
        .build()

    private val placeholderItem: MediaItemData = MediaItemData.Builder("xmsleep")
        .setMediaItem(MediaItem.fromUri(""))
        .setMediaMetadata(MediaMetadata.Builder().build())
        .setIsSeekable(false)
        .setDurationUs(C.TIME_UNSET)
        .build()

    fun onPlaybackChanged(playing: Boolean, descriptions: List<String>) {
        isPlaying = playing
        soundDescriptions = descriptions
        invalidateState()
    }

    override fun getState(): State {
        val subtitle = when {
            soundDescriptions.isEmpty() -> ""
            soundDescriptions.size <= 2 -> soundDescriptions.joinToString(" + ")
            soundDescriptions.size == 3 -> soundDescriptions.joinToString(" + ")
            else -> "${soundDescriptions.take(2).joinToString(" + ")} + 其他 ${soundDescriptions.size - 2} 个"
        }

        return State.Builder()
            .setAvailableCommands(availableCommands)
            .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(Player.STATE_READY)
            .setPlaylist(listOf(placeholderItem))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(0)
            .setPlaylistMetadata(
                MediaMetadata.Builder()
                    .setTitle("XMSLEEP")
                    .setSubtitle(subtitle)
                    .build()
            )
            .build()
    }
}
