package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId

/** The stored copy of the YouTube account's playlists; it is shown through [PlaylistRepository]. */
interface MirroredPlaylistRepository {
    /** Kinds of the videos stored in mirrored playlists, to avoid classifying them again. */
    suspend fun getMirroredContentKinds(): Map<VideoId, VideoContentKind>

    /**
     * Replaces the mirror: playlists keep their local ids, missing ones are deleted and the videos of each
     * playlist are replaced.
     */
    suspend fun replaceMirroredPlaylists(playlists: List<AccountPlaylist>)
}
