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
    private var subtitle: String = "XMSLEEP"

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

    /**
     * 更新播放状态
     * @param playing 是否正在播放
     * @param subtitle 已根据当前 locale 格式化好的副标题（由 MusicService 通过 string resource 生成）
     */
    fun onPlaybackChanged(playing: Boolean, subtitle: String) {
        isPlaying = playing
        this.subtitle = subtitle
        invalidateState()
    }

    override fun getState(): State {
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
