package dev.satoshi.usefulyoutube.core.model

data class AppSettings(
    /** Shows the "For you" tab with videos picked on the device from the watch history and the likes. */
    val recommendationsEnabled: Boolean = true,
)
