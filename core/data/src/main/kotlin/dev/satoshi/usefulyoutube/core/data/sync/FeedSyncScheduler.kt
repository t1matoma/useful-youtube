package dev.satoshi.usefulyoutube.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Schedules the hourly background feed refresh that runs only when the network is available. */
class FeedSyncScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun schedule() {
            val request =
                PeriodicWorkRequestBuilder<RefreshFeedWorker>(REFRESH_INTERVAL_HOURS, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        companion object {
            const val WORK_NAME = "feed_refresh"
            const val REFRESH_INTERVAL_HOURS = 1L
        }
    }
