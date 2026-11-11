package dev.satoshi.usefulyoutube.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.satoshi.usefulyoutube.core.model.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PreferencesSettingsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = PreferencesSettingsRepository(context, dispatcher)

    @Test
    fun `recommendations are enabled until the user turns them off`() =
        runTest(dispatcher) {
            assertThat(repository.observeSettings().first()).isEqualTo(AppSettings(recommendationsEnabled = true))
        }

    @Test
    fun `a changed setting is emitted and survives a new repository`() =
        runTest(dispatcher) {
            repository.observeSettings().test {
                assertThat(awaitItem().recommendationsEnabled).isTrue()

                repository.setRecommendationsEnabled(false)

                assertThat(awaitItem().recommendationsEnabled).isFalse()

                repository.setRecommendationsEnabled(true)

                assertThat(awaitItem().recommendationsEnabled).isTrue()
            }

            repository.setRecommendationsEnabled(false)

            val reopened = PreferencesSettingsRepository(context, dispatcher)
            assertThat(reopened.observeSettings().first().recommendationsEnabled).isFalse()
        }
}
