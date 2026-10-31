package dev.satoshi.usefulyoutube

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.satoshi.usefulyoutube.core.data.sync.FeedSyncScheduler
import javax.inject.Inject

@HiltAndroidApp
class UsefulYouTubeApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var feedSyncScheduler: FeedSyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        feedSyncScheduler.schedule()
    }
}
