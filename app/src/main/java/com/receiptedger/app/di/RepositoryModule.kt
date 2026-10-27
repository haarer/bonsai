package com.receiptedger.app.di

import com.receiptedger.app.data.repo.ReceiptRepositoryImpl
import com.receiptedger.app.domain.model.ReceiptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): ReceiptRepository
}
