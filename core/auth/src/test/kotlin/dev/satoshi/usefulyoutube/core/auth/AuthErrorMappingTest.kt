package dev.satoshi.usefulyoutube.core.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import dev.satoshi.usefulyoutube.core.model.AuthError
import org.junit.Test

class AuthErrorMappingTest {
    @Test
    fun `closed consent screen is a cancellation`() {
        assertThat(authErrorOf(CommonStatusCodes.CANCELED)).isEqualTo(AuthError.Cancelled)
    }

    @Test
    fun `missing or outdated play services are unavailable`() {
        listOf(
            ConnectionResult.SERVICE_MISSING,
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
            ConnectionResult.SERVICE_DISABLED,
            ConnectionResult.SERVICE_INVALID,
            CommonStatusCodes.API_NOT_CONNECTED,
        ).forEach { code -> assertThat(authErrorOf(code)).isEqualTo(AuthError.Unavailable) }
    }

    @Test
    fun `other failures are generic`() {
        assertThat(authErrorOf(CommonStatusCodes.DEVELOPER_ERROR)).isEqualTo(AuthError.Failed)
        assertThat(authErrorOf(CommonStatusCodes.NETWORK_ERROR)).isEqualTo(AuthError.Failed)
    }
}
