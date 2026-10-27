package com.receiptedger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val storeName: String,
    val branch: String? = null,
    val dateMillis: Long? = null,
    val receiptNumber: String? = null,
    val subtotal: Double? = null,
    val tax: Double? = null,
    val total: Double? = null,
    val currency: String = "EUR",
    val imagePath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
