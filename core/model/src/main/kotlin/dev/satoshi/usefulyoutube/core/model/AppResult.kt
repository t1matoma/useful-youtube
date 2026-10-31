package dev.satoshi.usefulyoutube.core.model

/**
 * Explicit result of an operation that can fail with a typed [AppError] instead of throwing.
 */
sealed interface AppResult<out T, out E : AppError> {
    data class Success<out T>(
        val value: T,
    ) : AppResult<T, Nothing>

    data class Failure<out E : AppError>(
        val error: E,
    ) : AppResult<Nothing, E>
}

inline fun <T, E : AppError, R> AppResult<T, E>.map(transform: (T) -> R): AppResult<R, E> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(value))
        is AppResult.Failure -> this
    }

inline fun <T, E : AppError, R : AppError> AppResult<T, E>.mapError(transform: (E) -> R): AppResult<T, R> =
    when (this) {
        is AppResult.Success -> this
        is AppResult.Failure -> AppResult.Failure(transform(error))
    }

fun <T> AppResult<T, AppError>.getOrNull(): T? = (this as? AppResult.Success)?.value

fun AppResult<*, AppError>.errorOrNull(): AppError? = (this as? AppResult.Failure)?.error
