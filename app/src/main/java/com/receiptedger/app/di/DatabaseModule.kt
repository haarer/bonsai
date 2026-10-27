package com.receiptedger.app.di

import android.content.Context
import androidx.room.Room
import com.receiptedger.app.data.local.ReceiptLedgerDatabase
import com.receiptedger.app.data.local.dao.ReceiptDao
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
    fun provideReceiptLedgerDatabase(@ApplicationContext context: Context): ReceiptLedgerDatabase =
        Room.databaseBuilder(context, ReceiptLedgerDatabase::class.java, ReceiptLedgerDatabase.NAME)
            .build()

    @Provides
    fun provideReceiptDao(db: ReceiptLedgerDatabase): ReceiptDao = db.receiptDao()
}
