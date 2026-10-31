package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.model.ApiErrorResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.XmlException
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection

/** Converts exceptions and HTTP statuses of network calls into [DataError]. */
internal class NetworkErrorMapper(
    private val json: Json,
) {
    @Suppress("TooGenericExceptionCaught")
    inline fun <T> catching(block: () -> AppResult<T, DataError>): AppResult<T, DataError> =
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(fromException(exception))
        }

    fun fromException(exception: Exception): DataError =
        when (exception) {
            is HttpException -> fromHttpException(exception)
            is SerializationException, is IllegalArgumentException, is XmlException -> DataError.Parsing
            is IOException -> DataError.NoNetwork
            else -> DataError.Unknown
        }

    fun fromStatus(code: Int): DataError =
        when (code) {
            HttpURLConnection.HTTP_NOT_FOUND -> DataError.NotFound
            else -> DataError.Http(code)
        }

    private fun fromHttpException(exception: HttpException): DataError {
        val reasons = errorReasons(exception)
        return when {
            reasons.any { it in INVALID_KEY_REASONS } -> DataError.InvalidApiKey
            reasons.any { it in QUOTA_REASONS } -> DataError.QuotaExceeded
            else -> fromStatus(exception.code())
        }
    }

    private fun errorReasons(exception: HttpException): List<String> {
        val body = exception.response()?.errorBody()?.string() ?: return emptyList()
        return try {
            json.decodeFromString<ApiErrorResponseDto>(body).error.let { error ->
                (error.errors + error.details).map { it.reason }
            }
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private companion object {
        val INVALID_KEY_REASONS = setOf("keyInvalid", "keyExpired", "API_KEY_INVALID")
        val QUOTA_REASONS = setOf("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded")
    }
}
