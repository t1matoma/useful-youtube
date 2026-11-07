package dev.satoshi.usefulyoutube.core.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.repository.AccountSubscriptionsReader
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.ImportError
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeAccountAuthorizer
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportAccountSubscriptionsUseCaseTest {
    private val repository = FakeChannelRepository(initialChannels = listOf(TestData.firstChannel))
    private val authorizer = FakeAccountAuthorizer("stale", "fresh")
    private val requestedTokens = mutableListOf<String>()

    private fun importWith(
        reader: (String) -> AppResult<List<Channel>, DataError>,
    ): ImportAccountSubscriptionsUseCase =
        ImportAccountSubscriptionsUseCase(
            authorizer = authorizer,
            subscriptionsReader =
                AccountSubscriptionsReader { token ->
                    requestedTokens += token
                    reader(token)
                },
            channelRepository = repository,
        )

    @Test
    fun `imports only new channels and returns their count`() =
        runTest {
            val importSubscriptions =
                importWith { AppResult.Success(listOf(TestData.firstChannel, TestData.secondChannel)) }

            assertThat(importSubscriptions()).isEqualTo(AppResult.Success(1))
            assertThat(repository.getChannels()).containsExactly(TestData.firstChannel, TestData.secondChannel)
            assertThat(requestedTokens).containsExactly("stale")
        }

    @Test
    fun `account without subscriptions is an error`() =
        runTest {
            assertThat(importWith { AppResult.Success(emptyList()) }())
                .isEqualTo(AppResult.Failure(ImportError.NoChannels))
        }

    @Test
    fun `authorization error is propagated without requests`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired

            val result = importWith { AppResult.Success(listOf(TestData.secondChannel)) }()

            assertThat(result).isEqualTo(AppResult.Failure(ImportError.Auth(AuthError.ConsentRequired)))
            assertThat(requestedTokens).isEmpty()
        }

    @Test
    fun `rejected token is cleared and replaced once`() =
        runTest {
            val importSubscriptions =
                importWith { token ->
                    if (token == "stale") {
                        AppResult.Failure(DataError.Unauthorized)
                    } else {
                        AppResult.Success(listOf(TestData.secondChannel))
                    }
                }

            assertThat(importSubscriptions()).isEqualTo(AppResult.Success(1))
            assertThat(authorizer.clearedTokens).containsExactly("stale")
            assertThat(requestedTokens).containsExactly("stale", "fresh")
        }

    @Test
    fun `token rejected twice is reported`() =
        runTest {
            val result = importWith { AppResult.Failure(DataError.Unauthorized) }()

            assertThat(result).isEqualTo(AppResult.Failure(ImportError.Remote(DataError.Unauthorized)))
            assertThat(requestedTokens).containsExactly("stale", "fresh")
        }

    @Test
    fun `network error is propagated`() =
        runTest {
            assertThat(importWith { AppResult.Failure(DataError.QuotaExceeded) }())
                .isEqualTo(AppResult.Failure(ImportError.Remote(DataError.QuotaExceeded)))
        }
}
