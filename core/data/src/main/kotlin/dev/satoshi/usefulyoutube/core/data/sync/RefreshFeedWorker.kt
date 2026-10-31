package dev.satoshi.usefulyoutube.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshFeedUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError

/** Periodically refreshes the feed in the background. */
@HiltWorker
class RefreshFeedWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val refreshFeed: RefreshFeedUseCase,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result =
            when (val result = refreshFeed()) {
                is AppResult.Success -> Result.success()
                is AppResult.Failure -> if (result.error.isTemporary) Result.retry() else Result.failure()
            }

        /** Retrying cannot fix an invalid key or an exhausted quota before the next periodic run. */
        private val DataError.isTemporary: Boolean
            get() =
                when (this) {
                    DataError.InvalidApiKey, DataError.QuotaExceeded, DataError.NotFound, DataError.Parsing -> false
                    DataError.NoNetwork, is DataError.Http, DataError.Unknown -> true
                }
    }
