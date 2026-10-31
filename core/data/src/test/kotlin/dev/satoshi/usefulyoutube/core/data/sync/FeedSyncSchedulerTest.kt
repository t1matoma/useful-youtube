package dev.satoshi.usefulyoutube.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FeedSyncSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun initializeWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun `schedules single hourly refresh requiring network`() {
        val scheduler = FeedSyncScheduler(context)

        scheduler.schedule()
        scheduler.schedule()

        val workInfos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(FeedSyncScheduler.WORK_NAME).get()
        assertThat(workInfos).hasSize(1)
        val workInfo = workInfos.single()
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(workInfo.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        assertThat(workInfo.periodicityInfo?.repeatIntervalMillis).isEqualTo(TimeUnit.HOURS.toMillis(1))
    }
}
