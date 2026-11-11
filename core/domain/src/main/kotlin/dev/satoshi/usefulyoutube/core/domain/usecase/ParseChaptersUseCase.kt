package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.model.Chapter
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Reads the chapters of a video from its description, the way YouTube does: lines with a timestamp at their
 * start or end (`0:00 Intro`, `[1:02:03] - Finale`, `Intro 0:00`). The list starts at `0:00`, goes in ascending
 * order and has at least [MINIMUM_CHAPTERS] chapters; a description without such a list has no chapters.
 */
class ParseChaptersUseCase
    @Inject
    constructor() {
        /** With a known [duration] timestamps after the end of the video are ignored. */
        operator fun invoke(
            description: String,
            duration: Duration? = null,
        ): List<Chapter> {
            val chapters = mutableListOf<Chapter>()
            description
                .lineSequence()
                .mapNotNull(::parseLine)
                .dropWhile { it.start != Duration.ZERO }
                .filter { duration == null || !duration.isPositive() || it.start < duration }
                .forEach { chapter ->
                    // A timestamp that goes back is a reference inside the text, not the next chapter.
                    if (chapters.isEmpty() || chapter.start > chapters.last().start) chapters += chapter
                }
            return if (chapters.size >= MINIMUM_CHAPTERS) chapters else emptyList()
        }

        private fun parseLine(rawLine: String): Chapter? {
            val line = rawLine.trim()
            val leading = LEADING_TIMESTAMP.find(line)
            val match = leading ?: TRAILING_TIMESTAMP.find(line) ?: return null
            val start = match.timestamp() ?: return null
            val title =
                if (leading != null) {
                    match.groups[TITLE_GROUP]!!
                        .value
                        .trimStart { it in SEPARATORS }
                        .replace(RANGE_END, "")
                        .trim()
                } else {
                    match.groups[TITLE_GROUP]!!.value.trimEnd { it in SEPARATORS }
                }
            return if (title.isEmpty()) null else Chapter(title, start)
        }

        private fun MatchResult.timestamp(): Duration? {
            val hours = groups[HOURS_GROUP]?.value?.toInt() ?: 0
            val minutes = groups[MINUTES_GROUP]!!.value.toInt()
            val seconds = groups[SECONDS_GROUP]!!.value.toInt()
            if (seconds >= SECONDS_PER_MINUTE) return null
            return hours.hours + minutes.minutes + seconds.seconds
        }

        private companion object {
            const val MINIMUM_CHAPTERS = 3
            const val SECONDS_PER_MINUTE = 60
            const val HOURS_GROUP = "hours"
            const val MINUTES_GROUP = "minutes"
            const val SECONDS_GROUP = "seconds"
            const val TITLE_GROUP = "title"
            const val TIMESTAMP = """(?:(?<hours>\d{1,2}):)?(?<minutes>\d{1,2}):(?<seconds>\d{2})"""
            const val DECORATION = """[^\p{L}\p{N}]*"""

            val LEADING_TIMESTAMP = Regex("""^$DECORATION$TIMESTAMP(?!\d|:\d)(?<title>.*)$""")
            val TRAILING_TIMESTAMP = Regex("""^(?<title>.*?)(?<![\d:])$TIMESTAMP$DECORATION$""")

            /** The end of a range such as `0:00 - 1:30 Intro`. */
            val RANGE_END = Regex("""^(?:\d{1,2}:)?\d{1,2}:\d{2}""")
            const val SEPARATORS = " \t-–—:|•*([)].,"
        }
    }
