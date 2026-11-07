package dev.satoshi.usefulyoutube.core.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.satoshi.usefulyoutube.core.auth.AccountConsent
import dev.satoshi.usefulyoutube.core.auth.GoogleAccountAuthorization
import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthModule {
    @Binds
    abstract fun bindAccountAuthorizer(authorization: GoogleAccountAuthorization): AccountAuthorizer

    @Binds
    abstract fun bindAccountConsent(authorization: GoogleAccountAuthorization): AccountConsent
}
