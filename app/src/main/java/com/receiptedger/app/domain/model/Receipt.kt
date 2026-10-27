package com.receiptedger.app.domain.model

import java.time.Instant

/** A stored receipt. [id] is 0L until persisted. */
data class Receipt(
    val id: Long = 0L,
    val storeName: String,
    val branch: String? = null,
    val date: Instant? = null,
    val receiptNumber: String? = null,
    val items: List<LineItem> = emptyList(),
    val subtotal: Double? = null,
    val tax: Double? = null,
    val total: Double? = null,
    val currency: String = "EUR",
    val imagePath: String? = null,
    val createdAt: Instant = Instant.now(),
)
