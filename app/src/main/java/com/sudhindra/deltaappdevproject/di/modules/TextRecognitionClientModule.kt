package com.sudhindra.deltaappdevproject.di.modules

import android.content.Context
import com.sudhindra.deltaappdevproject.clients.TextRecognitionClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object TextRecognitionClientModule {

    @Singleton
    @Provides
    fun provideTextRecognitionClient(@ApplicationContext context: Context) = TextRecognitionClient(context)
}