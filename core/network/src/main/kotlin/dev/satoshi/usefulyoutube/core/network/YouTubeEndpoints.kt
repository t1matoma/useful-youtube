package dev.satoshi.usefulyoutube.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Base URLs of the public YouTube endpoints; replaced with a local server in tests. */
data class YouTubeEndpoints(
    val dataApiBaseUrl: HttpUrl = "https://www.googleapis.com/youtube/v3/".toHttpUrl(),
    val websiteBaseUrl: HttpUrl = "https://www.youtube.com/".toHttpUrl(),
)
