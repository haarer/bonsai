package com.receiptedger.app.data.local.mapper

import com.receiptedger.app.data.local.entity.LineItemEntity
import com.receiptedger.app.data.local.entity.ReceiptEntity
import com.receiptedger.app.data.local.entity.ReceiptWithItems
import com.receiptedger.app.domain.model.LineItem
import com.receiptedger.app.domain.model.Receipt
import java.time.Instant

fun ReceiptWithItems.toDomain(): Receipt =
    receipt.toDomain(items.map { it.toDomain() })

fun ReceiptEntity.toDomain(items: List<LineItem> = emptyList()): Receipt = Receipt(
    id = id,
    storeName = storeName,
    branch = branch,
    date = dateMillis?.let { Instant.ofEpochMilli(it) },
    receiptNumber = receiptNumber,
    items = items,
    subtotal = subtotal,
    tax = tax,
    total = total,
    currency = currency,
    imagePath = imagePath,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun LineItemEntity.toDomain(): LineItem = LineItem(
    id = id,
    name = name,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    category = category,
    taxCode = taxCode,
    rawText = rawText,
)

fun Receipt.toEntity(): ReceiptEntity = ReceiptEntity(
    id = id,
    storeName = storeName,
    branch = branch,
    dateMillis = date?.toEpochMilli(),
    receiptNumber = receiptNumber,
    subtotal = subtotal,
    tax = tax,
    total = total,
    currency = currency,
    imagePath = imagePath,
    createdAtMillis = createdAt.toEpochMilli(),
)

fun LineItem.toEntity(receiptId: Long): LineItemEntity = LineItemEntity(
    id = id,
    receiptId = receiptId,
    name = name,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    category = category,
    taxCode = taxCode,
    rawText = rawText,
)

fun List<LineItem>.toEntities(receiptId: Long): List<LineItemEntity> =
    map { it.toEntity(receiptId) }
