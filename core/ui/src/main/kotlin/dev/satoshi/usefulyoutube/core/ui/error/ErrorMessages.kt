package dev.satoshi.usefulyoutube.core.ui.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.satoshi.usefulyoutube.core.model.AppError
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.ImportError
import dev.satoshi.usefulyoutube.core.ui.R

/** Human-readable description of the error for the user. */
@Composable
fun AppError.message(): String =
    when (this) {
        is DataError -> dataErrorMessage(this)
        is ImportError -> importErrorMessage(this)
        is ChannelError -> channelErrorMessage(this)
        is AuthError -> authErrorMessage(this)
    }

@Composable
private fun dataErrorMessage(error: DataError): String =
    when (error) {
        DataError.NoNetwork -> stringResource(R.string.core_ui_error_no_network)
        DataError.InvalidApiKey -> stringResource(R.string.core_ui_error_invalid_api_key)
        DataError.Unauthorized -> stringResource(R.string.core_ui_error_unauthorized)
        DataError.QuotaExceeded -> stringResource(R.string.core_ui_error_quota_exceeded)
        DataError.NotFound -> stringResource(R.string.core_ui_error_not_found)
        is DataError.Http -> stringResource(R.string.core_ui_error_http, error.code)
        DataError.Parsing -> stringResource(R.string.core_ui_error_parsing)
        DataError.Unknown -> stringResource(R.string.core_ui_error_unknown)
    }

@Composable
private fun importErrorMessage(error: ImportError): String =
    when (error) {
        ImportError.UnreadableFile -> stringResource(R.string.core_ui_error_unreadable_file)
        ImportError.InvalidFormat -> stringResource(R.string.core_ui_error_invalid_format)
        ImportError.NoChannels -> stringResource(R.string.core_ui_error_no_channels)
        is ImportError.Auth -> authErrorMessage(error.cause)
        is ImportError.Remote -> dataErrorMessage(error.cause)
    }

@Composable
private fun authErrorMessage(error: AuthError): String =
    when (error) {
        AuthError.ConsentRequired -> stringResource(R.string.core_ui_error_auth_consent_required)
        AuthError.Cancelled -> stringResource(R.string.core_ui_error_auth_cancelled)
        AuthError.Unavailable -> stringResource(R.string.core_ui_error_auth_unavailable)
        AuthError.Failed -> stringResource(R.string.core_ui_error_auth_failed)
    }

@Composable
private fun channelErrorMessage(error: ChannelError): String =
    when (error) {
        ChannelError.InvalidInput -> stringResource(R.string.core_ui_error_invalid_channel_input)
        ChannelError.AlreadySubscribed -> stringResource(R.string.core_ui_error_already_subscribed)
        ChannelError.NotFound -> stringResource(R.string.core_ui_error_channel_not_found)
        is ChannelError.Remote -> dataErrorMessage(error.cause)
    }
