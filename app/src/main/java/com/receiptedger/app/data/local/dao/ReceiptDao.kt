package com.receiptedger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.receiptedger.app.data.local.entity.LineItemEntity
import com.receiptedger.app.data.local.entity.ReceiptEntity
import com.receiptedger.app.data.local.entity.ReceiptWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Transaction
    @Query("SELECT * FROM receipts ORDER BY createdAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<ReceiptWithItems>>

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<ReceiptWithItems?>

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): ReceiptWithItems?

    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Insert
    suspend fun insertAll(items: List<LineItemEntity>)

    @Update
    suspend fun update(receipt: ReceiptEntity)

    @Query("DELETE FROM line_items WHERE receiptId = :receiptId")
    suspend fun deleteItems(receiptId: Long)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
