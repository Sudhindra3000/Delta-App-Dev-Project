package com.sudhindra.deltaappdevproject.di.modules

import android.content.Context
import com.sudhindra.deltaappdevproject.clients.ShareClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object ShareClientModule {

    @Singleton
    @Provides
    fun provideShareClient(@ApplicationContext context: Context) = ShareClient(context)
}