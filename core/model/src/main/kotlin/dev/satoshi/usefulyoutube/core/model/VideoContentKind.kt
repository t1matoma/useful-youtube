package dev.satoshi.usefulyoutube.core.model

enum class VideoContentKind {
    REGULAR,
    SHORT,
    LIVE_OR_PREMIERE,

    /** Details are not loaded yet or the kind could not be determined; such videos are hidden from the feed. */
    UNCLASSIFIED,
}
