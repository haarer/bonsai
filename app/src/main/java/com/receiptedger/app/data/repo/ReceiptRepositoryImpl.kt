package com.receiptedger.app.data.repo

import androidx.room.withTransaction

import com.receiptedger.app.data.local.ReceiptLedgerDatabase
import com.receiptedger.app.data.local.dao.ReceiptDao
import com.receiptedger.app.data.local.mapper.toDomain
import com.receiptedger.app.data.local.mapper.toEntities
import com.receiptedger.app.data.local.mapper.toEntity
import com.receiptedger.app.domain.model.Receipt
import com.receiptedger.app.domain.model.ReceiptRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val dao: ReceiptDao,
    private val database: ReceiptLedgerDatabase,
) : ReceiptRepository {

    override fun observeReceipts(): Flow<List<Receipt>> =
        dao.observeAll().map { withItems -> withItems.map { it.toDomain() } }

    override fun observeReceipt(id: Long): Flow<Receipt?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getReceipt(id: Long): Receipt? =
        dao.getById(id)?.toDomain()

    override suspend fun saveReceipt(receipt: Receipt): Long =
        database.withTransaction {
            if (receipt.id == 0L) {
                val id = dao.insert(receipt.toEntity())
                dao.insertAll(receipt.items.toEntities(id))
                id
            } else {
                dao.update(receipt.toEntity())
                dao.deleteItems(receipt.id)
                dao.insertAll(receipt.items.toEntities(receipt.id))
                receipt.id
            }
        }

    override suspend fun deleteReceipt(id: Long) {
        dao.deleteById(id)
    }
}
