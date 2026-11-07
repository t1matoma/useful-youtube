package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylist
import dev.satoshi.usefulyoutube.core.domain.repository.MirroredPlaylistRepository
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId

class FakeMirroredPlaylistRepository(
    /** Kinds of videos that are already stored in the mirror. */
    val knownKinds: MutableMap<VideoId, VideoContentKind> = mutableMapOf(),
) : MirroredPlaylistRepository {
    /** The playlists of the last [replaceMirroredPlaylists] call; `null` before it. */
    var mirroredPlaylists: List<AccountPlaylist>? = null
        private set

    override suspend fun getMirroredContentKinds(): Map<VideoId, VideoContentKind> = knownKinds.toMap()

    override suspend fun replaceMirroredPlaylists(playlists: List<AccountPlaylist>) {
        mirroredPlaylists = playlists
    }
}
