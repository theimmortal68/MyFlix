package com.myflix.app.di

import com.myflix.app.domain.usecase.GetFeaturedMediaUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import org.jellyfin.sdk.api.client.ApiClient

/**
 * Hilt module providing use cases for the home screen features.
 */
@Module
@InstallIn(ViewModelComponent::class)
object HomeUseCaseModule {
    
    @Provides
    @ViewModelScoped
    fun provideGetFeaturedMediaUseCase(
        apiClient: ApiClient
    ): GetFeaturedMediaUseCase {
        return GetFeaturedMediaUseCase(apiClient)
    }
}

// ============================================================================
// ALTERNATIVE: If you already have a UseCaseModule, add this to it instead:
// ============================================================================

/*
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    // ... your existing use case providers ...
    
    @Provides
    @ViewModelScoped
    fun provideGetFeaturedMediaUseCase(
        apiClient: ApiClient
    ): GetFeaturedMediaUseCase {
        return GetFeaturedMediaUseCase(apiClient)
    }
}
*/

// ============================================================================
// NOTE: Make sure your ApiClient is already provided somewhere in your DI setup.
// The Jellyfin SDK ApiClient is typically provided in your main AppModule or
// NetworkModule. If not, you'll need to add that provider as well.
// ============================================================================
