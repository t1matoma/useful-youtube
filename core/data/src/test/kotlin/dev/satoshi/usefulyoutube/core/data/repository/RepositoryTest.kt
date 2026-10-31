package dev.satoshi.usefulyoutube.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource
import dev.satoshi.usefulyoutube.core.database.UsefulYouTubeDatabase
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
abstract class RepositoryTest {
    protected val database: UsefulYouTubeDatabase =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), UsefulYouTubeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    protected val network = FakeYouTubeNetworkDataSource()
    protected val testDispatcher = StandardTestDispatcher()

    @After
    fun closeDatabase() {
        database.close()
    }
}
