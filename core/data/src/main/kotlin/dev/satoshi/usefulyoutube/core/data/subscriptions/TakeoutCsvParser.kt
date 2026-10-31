package dev.satoshi.usefulyoutube.core.data.subscriptions

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.ImportError
import javax.inject.Inject

/**
 * Parses `subscriptions.csv` from Google Takeout: `Channel Id, Channel Url, Channel Title`.
 *
 * Columns are read by position because Takeout localizes the header row; rows without a channel id
 * (the header) are skipped.
 */
class TakeoutCsvParser
    @Inject
    constructor() {
        fun parse(content: String): AppResult<List<Channel>, ImportError> {
            val rows =
                content
                    .removePrefix(BYTE_ORDER_MARK)
                    .lineSequence()
                    .filter { it.isNotBlank() }
                    .map(::splitRow)
                    .toList()
            val channels =
                rows
                    .filter(::isChannelRow)
                    .map { row -> Channel(ChannelId(row[ID_COLUMN]), row.getOrNull(TITLE_COLUMN).orEmpty()) }
                    .distinctBy { it.id }
            val hasRowsBesidesHeader = rows.size > 1
            return if (channels.isEmpty() && hasRowsBesidesHeader) {
                AppResult.Failure(ImportError.InvalidFormat)
            } else {
                AppResult.Success(channels)
            }
        }

        private fun isChannelRow(row: List<String>): Boolean = CHANNEL_ID_REGEX.matches(row.first())

        /** Splits a CSV row, supporting quoted fields with commas and doubled quotes. */
        private fun splitRow(line: String): List<String> {
            val fields = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            var index = 0
            while (index < line.length) {
                val char = line[index]
                when {
                    char == QUOTE && inQuotes && line.getOrNull(index + 1) == QUOTE -> {
                        current.append(QUOTE)
                        index++
                    }

                    char == QUOTE -> {
                        inQuotes = !inQuotes
                    }

                    char == SEPARATOR && !inQuotes -> {
                        fields += current.toString().trim()
                        current.clear()
                    }

                    else -> {
                        current.append(char)
                    }
                }
                index++
            }
            fields += current.toString().trim()
            return fields
        }

        private companion object {
            const val ID_COLUMN = 0
            const val TITLE_COLUMN = 2
            const val QUOTE = '"'
            const val SEPARATOR = ','
            const val BYTE_ORDER_MARK = ""
            val CHANNEL_ID_REGEX = Regex("UC[A-Za-z0-9_-]{22}")
        }
    }
