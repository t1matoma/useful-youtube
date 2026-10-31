package dev.satoshi.usefulyoutube.core.data.subscriptions

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.domain.repository.SubscriptionsFileReader
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ImportError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

internal class ContentResolverSubscriptionsFileReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val parser: TakeoutCsvParser,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SubscriptionsFileReader {
        override suspend fun read(fileUri: String): AppResult<List<Channel>, ImportError> {
            val content =
                withContext(ioDispatcher) {
                    try {
                        context.contentResolver
                            .openInputStream(Uri.parse(fileUri))
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    } catch (_: IOException) {
                        null
                    } catch (_: SecurityException) {
                        null
                    }
                } ?: return AppResult.Failure(ImportError.UnreadableFile)
            return parser.parse(content)
        }
    }
