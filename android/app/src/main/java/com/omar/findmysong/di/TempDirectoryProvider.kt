package com.omar.findmysong.di

import android.content.Context
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TempDirectory

@Module
@InstallIn(SingletonComponent::class)
object TempDirectoryProvider {

    @TempDirectory
    @Provides
    fun provideTempDirectory(
        @ApplicationContext context: Context
    ): File = context.externalCacheDir!!

    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context
    ) = WorkManager.getInstance(context)

}