package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video

/** A playlist of the YouTube account with the details of its available videos; kinds are not determined yet. */
data class AccountPlaylist(
    val remoteId: String,
    val title: String,
    val videos: List<Video>,
)

/** Reads all playlists of the authorized YouTube account. */
fun interface AccountPlaylistsReader {
    suspend fun read(accessToken: String): AppResult<List<AccountPlaylist>, DataError>
}
