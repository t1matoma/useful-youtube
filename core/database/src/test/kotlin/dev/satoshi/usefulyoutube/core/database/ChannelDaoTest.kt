package dev.satoshi.usefulyoutube.core.database

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChannelDaoTest : DatabaseTest() {
    private val channelDao get() = database.channelDao()

    @Test
    fun `channels are sorted by title ignoring case`() =
        runTest {
            channelDao.insertOrIgnore(listOf(channel("b", "beta"), channel("a", "Alpha"), channel("c", "Gamma")))

            assertThat(channelDao.getChannels().map { it.title }).containsExactly("Alpha", "beta", "Gamma")
        }

    @Test
    fun `existing channels are ignored on insert`() =
        runTest {
            channelDao.insertOrIgnore(listOf(channel("a", "Original")))

            val rowIds = channelDao.insertOrIgnore(listOf(channel("a", "Duplicate"), channel("b")))

            assertThat(rowIds.map { it == -1L }).containsExactly(true, false)
            assertThat(channelDao.getChannels().map { it.title }).containsExactly("Channel b", "Original")
        }

    @Test
    fun `deleting channel deletes its videos`() =
        runTest {
            channelDao.insertOrIgnore(listOf(channel("a"), channel("b")))
            database.videoDao().insertOrIgnore(listOf(video("v1", "a"), video("v2", "b")))

            channelDao.deleteChannel("a")

            assertThat(channelDao.getChannels().map { it.id }).containsExactly("b")
            database.videoDao().observeVideo("v1").test { assertThat(listOfNotNull(awaitItem())).isEmpty() }
        }

    @Test
    fun `channel changes are observed`() =
        runTest {
            channelDao.observeChannels().test {
                assertThat(awaitItem()).isEmpty()

                channelDao.insertOrIgnore(listOf(channel("a")))
                assertThat(awaitItem().map { it.id }).containsExactly("a")
            }
        }
}
