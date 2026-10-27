package com.receiptedger.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Parent + children POJO for Room's 1:N [Relation] (entities cannot hold relations). */
data class ReceiptWithItems(
    @Embedded val receipt: ReceiptEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "receiptId",
    )
    val items: List<LineItemEntity> = emptyList(),
)
