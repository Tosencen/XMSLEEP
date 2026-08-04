package org.xmsleep.app.audio

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class AggregatePlayer : SimpleBasePlayer(Looper.getMainLooper()) {

    private var isPlaying = false
    private var subtitle: String = "XMSLEEP"

    // 封面 artwork（logo）字节数据
    var artworkData: ByteArray? = null
        set(value) {
            field = value
            itemUid++
            invalidateState()
        }

    var onPlayPauseRequested: (() -> Unit)? = null
    var onStopRequested: (() -> Unit)? = null

    // 需要暴露 GET_* 命令，Media3 才会把标题/副标题/封面发布到系统媒体卡片
    private val availableCommands: Player.Commands = Player.Commands.Builder()
        .add(Player.COMMAND_PLAY_PAUSE)
        .add(Player.COMMAND_STOP)
        .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
        .add(Player.COMMAND_GET_TIMELINE)
        .add(Player.COMMAND_GET_METADATA)
        .build()

    // 播放项唯一标识。混音内容（副标题）或封面变化时递增，
    // 让 Media3 认为发生了媒体项切换（onMediaItemTransition），
    // 从而把标题/副标题/封面推送到系统媒体卡片（否则平台 metadata 一直是 null）。
    private var itemUid: Int = 0

    private fun buildPlaceholderItem(): MediaItemData {
        val mediaId = "xmsleep-$itemUid"
        val metadata = MediaMetadata.Builder()
            .setTitle("XMSLEEP")
            .setSubtitle(subtitle)
            // artist 映射到系统卡片第二行（description.subtitle）
            .setArtist(subtitle)
            .apply {
                artworkData?.let { setArtworkData(it) }
            }
            .build()
        // 注意：SimpleBasePlayer 发布的元数据取自 MediaItem.mediaMetadata，
        // 因此必须把标题/副标题/封面放到 MediaItem 上，而不是 MediaItemData 上。
        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()
        return MediaItemData.Builder("xmsleep-$itemUid")
            .setMediaItem(mediaItem)
            .setMediaMetadata(metadata)
            .setIsSeekable(false)
            .setDurationUs(C.TIME_UNSET)
            .build()
    }

    /**
     * 更新播放状态
     * @param playing 是否正在播放
     * @param subtitle 已根据当前 locale 格式化好的副标题（由 MusicService 通过 string resource 生成）
     */
    fun onPlaybackChanged(playing: Boolean, subtitle: String) {
        isPlaying = playing
        if (this.subtitle != subtitle) {
            this.subtitle = subtitle
            itemUid++
        }
        invalidateState()
    }

    override fun getState(): State {
        return State.Builder()
            .setAvailableCommands(availableCommands)
            .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(Player.STATE_READY)
            .setPlaylist(listOf(buildPlaceholderItem()))
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

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        onPlayPauseRequested?.invoke()
        return com.google.common.util.concurrent.Futures.immediateFuture(getState())
    }

    override fun handleStop(): ListenableFuture<*> {
        onStopRequested?.invoke()
        return com.google.common.util.concurrent.Futures.immediateFuture(getState())
    }
}
