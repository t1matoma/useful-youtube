package dev.satoshi.usefulyoutube.feature.player

import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.VideoId

sealed interface PlayerUiEvent {
    data object DescriptionToggled : PlayerUiEvent

    data object SubscribeToChannel : PlayerUiEvent

    data object PreviousClicked : PlayerUiEvent

    data object NextClicked : PlayerUiEvent

    /** The player started or resumed the current video; the first start puts it into the history. */
    data object PlaybackStarted : PlayerUiEvent

    /** The player reports where [videoId] is while it plays; the position is saved from time to time. */
    data class PlaybackProgressed(
        val videoId: VideoId,
        val position: PlaybackPosition,
    ) : PlayerUiEvent

    /** The video was paused or the player was closed; the position is saved. */
    data object PlaybackStopped : PlayerUiEvent

    /** The user jumped to the chapter with this index; the screen seeks the player itself. */
    data class ChapterSelected(
        val index: Int,
    ) : PlayerUiEvent

    /** The player finished the current video; a playlist goes on with the next one. */
    data object VideoEnded : PlayerUiEvent
}
