package com.receiptedger.app.domain.model

/** A single line on a receipt. [id] is 0L until persisted. */
data class LineItem(
    val id: Long = 0L,
    val name: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val category: String = "",
    val taxCode: String? = null,
    val rawText: String? = null,
)
