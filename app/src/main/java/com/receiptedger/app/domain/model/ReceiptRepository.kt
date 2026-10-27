package com.receiptedger.app.domain.model

import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    /** All receipts, newest first. */
    fun observeReceipts(): Flow<List<Receipt>>

    fun observeReceipt(id: Long): Flow<Receipt?>

    suspend fun getReceipt(id: Long): Receipt?

    /** Inserts when [Receipt.id] is 0, otherwise updates. Returns the stored id. */
    suspend fun saveReceipt(receipt: Receipt): Long

    suspend fun deleteReceipt(id: Long)
}
