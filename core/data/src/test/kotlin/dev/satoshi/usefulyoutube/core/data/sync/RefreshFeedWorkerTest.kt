package dev.satoshi.usefulyoutube.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.domain.usecase.ClassifyVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshFeedUseCase
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
class RefreshFeedWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val videoRepository = FakeVideoRepository()
    private val refreshFeed =
        RefreshFeedUseCase(
            FakeChannelRepository(listOf(TestData.firstChannel)),
            videoRepository,
            ClassifyVideoUseCase(DurationShortsDetector()),
        )

    private fun buildWorker(): RefreshFeedWorker =
        TestListenableWorkerBuilder<RefreshFeedWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = RefreshFeedWorker(appContext, workerParameters, refreshFeed)
                },
            ).build()

    @Test
    fun `successful refresh stores new videos and succeeds`() =
        runTest {
            val video = TestData.video(id = "new", duration = 20.minutes, contentKind = VideoContentKind.UNCLASSIFIED)
            videoRepository.remoteFeeds[TestData.firstChannel.id] = listOf(video)

            val result = buildWorker().doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            assertThat(videoRepository.currentVideos.single().contentKind).isEqualTo(VideoContentKind.REGULAR)
        }

    @Test
    fun `temporary failure is retried`() =
        runTest {
            videoRepository.failingChannels[TestData.firstChannel.id] = DataError.NoNetwork

            assertThat(buildWorker().doWork()).isEqualTo(ListenableWorker.Result.retry())
        }

    @Test
    fun `permanent failure is not retried`() =
        runTest {
            videoRepository.detailsError = DataError.InvalidApiKey

            assertThat(buildWorker().doWork()).isEqualTo(ListenableWorker.Result.failure())
        }
}
