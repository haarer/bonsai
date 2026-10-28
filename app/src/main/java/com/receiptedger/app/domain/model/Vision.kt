package com.receiptedger.app.domain.model

import java.io.File

data class ExtractedItem(
    val name: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val category: String = "",
)

data class ExtractedReceipt(
    val storeName: String = "",
    val branch: String = "",
    val date: String = "",
    val receiptNumber: String = "",
    val currency: String = "",
    val items: List<ExtractedItem> = emptyList(),
    val subtotal: Double? = null,
    val tax: Double? = null,
    val total: Double? = null,
)

interface VisionProvider {
    /** Extracts structured receipt data from [image] via the configured backend. */
    suspend fun extract(image: File, storeHint: String?): ExtractedReceipt
}
