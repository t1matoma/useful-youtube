package dev.satoshi.usefulyoutube.core.data.subscriptions

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.ImportError
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentResolverSubscriptionsFileReaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val reader =
        ContentResolverSubscriptionsFileReader(
            context = ApplicationProvider.getApplicationContext(),
            parser = TakeoutCsvParser(),
            ioDispatcher = testDispatcher,
        )

    @Test
    fun `reads and parses file`() =
        runTest(testDispatcher) {
            val file = temporaryFolder.newFile("subscriptions.csv")
            file.writeText("Channel Id,Channel Url,Channel Title\nUC_x5XG1OV2P6uZZ5FSM9Ttw,url,Google")

            val result = reader.read(Uri.fromFile(file).toString())

            assertThat(
                result,
            ).isEqualTo(AppResult.Success(listOf(Channel(ChannelId("UC_x5XG1OV2P6uZZ5FSM9Ttw"), "Google"))))
        }

    @Test
    fun `missing file is unreadable`() =
        runTest(testDispatcher) {
            val missing = Uri.fromFile(temporaryFolder.root.resolve("missing.csv")).toString()

            assertThat(reader.read(missing)).isEqualTo(AppResult.Failure(ImportError.UnreadableFile))
        }
}
