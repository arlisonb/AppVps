package com.vpsguardian.app.di

import android.content.Context
import androidx.room.Room
import com.vpsguardian.app.data.local.VpsDatabase
import com.vpsguardian.app.data.local.dao.AlertDao
import com.vpsguardian.app.data.local.dao.HistoryDao
import com.vpsguardian.app.data.local.dao.ServiceDao
import com.vpsguardian.app.data.local.dao.VpsDao
import com.vpsguardian.app.data.repository.ServiceRepositoryImpl
import com.vpsguardian.app.data.repository.VpsRepositoryImpl
import com.vpsguardian.app.domain.repository.ServiceRepository
import com.vpsguardian.app.domain.repository.VpsRepository
import com.vpsguardian.app.security.AesEncryption
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VpsDatabase =
        Room.databaseBuilder(context, VpsDatabase::class.java, "vps_guardian.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideVpsDao(db: VpsDatabase): VpsDao = db.vpsDao()
    @Provides fun provideServiceDao(db: VpsDatabase): ServiceDao = db.serviceDao()
    @Provides fun provideHistoryDao(db: VpsDatabase): HistoryDao = db.historyDao()
    @Provides fun provideAlertDao(db: VpsDatabase): AlertDao = db.alertDao()
}

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideAesEncryption(@ApplicationContext context: Context): AesEncryption =
        AesEncryption(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVpsRepository(impl: VpsRepositoryImpl): VpsRepository

    @Binds
    @Singleton
    abstract fun bindServiceRepository(impl: ServiceRepositoryImpl): ServiceRepository
}
