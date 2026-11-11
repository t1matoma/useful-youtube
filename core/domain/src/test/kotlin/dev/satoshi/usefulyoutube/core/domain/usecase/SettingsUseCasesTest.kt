package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SettingsUseCasesTest {
    private val repository = FakeSettingsRepository()

    @Test
    fun `recommendations are on by default and can be turned off`() =
        runTest {
            ObserveSettingsUseCase(repository)().test {
                assertThat(awaitItem().recommendationsEnabled).isTrue()

                SetRecommendationsEnabledUseCase(repository)(false)

                assertThat(awaitItem().recommendationsEnabled).isFalse()
            }
        }
}
