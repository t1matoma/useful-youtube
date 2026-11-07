package dev.satoshi.usefulyoutube.feature.history

import dev.satoshi.usefulyoutube.core.model.Video
import java.time.LocalDate

data class HistoryUiState(
    val isLoading: Boolean = true,
    /** Most recent day first; videos of a day are most recently watched first. */
    val days: List<HistoryDay> = emptyList(),
    val isClearDialogVisible: Boolean = false,
)

data class HistoryDay(
    val day: HistoryDayLabel,
    val videos: List<Video>,
)

sealed interface HistoryDayLabel {
    data object Today : HistoryDayLabel

    data object Yesterday : HistoryDayLabel

    data class Date(
        val date: LocalDate,
    ) : HistoryDayLabel
}
