package com.receiptedger.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "line_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("receiptId")]
)
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val receiptId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val category: String = "",
    val taxCode: String? = null,
    val rawText: String? = null,
)
