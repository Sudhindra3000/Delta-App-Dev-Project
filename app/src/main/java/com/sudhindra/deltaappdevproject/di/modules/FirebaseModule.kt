package com.sudhindra.deltaappdevproject.di.modules

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object FirebaseModule {

    @Provides
    @Singleton
    fun getFirebaseAuth() = FirebaseAuth.getInstance()
}
