package com.receiptedger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.receiptedger.app.data.local.dao.ReceiptDao
import com.receiptedger.app.data.local.entity.LineItemEntity
import com.receiptedger.app.data.local.entity.ReceiptEntity

@Database(
    entities = [ReceiptEntity::class, LineItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ReceiptLedgerDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao

    companion object {
        const val NAME = "receipt-ledger.db"
    }
}
