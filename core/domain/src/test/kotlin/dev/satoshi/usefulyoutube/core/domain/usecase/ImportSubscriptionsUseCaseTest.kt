package dev.satoshi.usefulyoutube.core.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ImportError
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportSubscriptionsUseCaseTest {
    private val repository = FakeChannelRepository(initialChannels = listOf(TestData.firstChannel))

    private fun importWith(result: AppResult<List<Channel>, ImportError>) =
        ImportSubscriptionsUseCase(fileReader = { result }, channelRepository = repository)

    @Test
    fun `imports only new channels and returns their count`() =
        runTest {
            val importSubscriptions =
                importWith(AppResult.Success(listOf(TestData.firstChannel, TestData.secondChannel)))

            assertThat(importSubscriptions("content://file")).isEqualTo(AppResult.Success(1))
            assertThat(repository.getChannels()).containsExactly(TestData.firstChannel, TestData.secondChannel)
        }

    @Test
    fun `empty file is an error`() =
        runTest {
            assertThat(importWith(AppResult.Success(emptyList()))("content://file"))
                .isEqualTo(AppResult.Failure(ImportError.NoChannels))
        }

    @Test
    fun `reader error is propagated`() =
        runTest {
            assertThat(importWith(AppResult.Failure(ImportError.InvalidFormat))("content://file"))
                .isEqualTo(AppResult.Failure(ImportError.InvalidFormat))
        }
}
