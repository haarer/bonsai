package com.receiptedger.app.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptedger.app.domain.model.LineItem
import com.receiptedger.app.domain.model.Receipt
import com.receiptedger.app.di.CapturedImageHolder
import com.receiptedger.app.domain.model.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiLineItem(
    val name: String = "",
    val quantityText: String = "1",
    val unitPriceText: String = "",
    val category: String = "",
) {
    val computedTotal: Double
        get() = (quantityText.toDoubleOrNull() ?: 0.0) * (unitPriceText.toDoubleOrNull() ?: 0.0)
}

data class ReviewState(
    val receiptId: Long = 0L,
    val imagePath: String? = null,
    val storeName: String = "",
    val branch: String = "",
    val dateText: String = "",
    val receiptNumber: String = "",
    val currency: String = "EUR",
    val items: List<UiLineItem> = emptyList(),
    val subtotalText: String = "",
    val taxText: String = "",
    val totalText: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/** Emitted once after save (positive id) or delete (negative id). */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReceiptRepository,
    private val capturedImage: CapturedImageHolder,
) : ViewModel() {

    private val receiptId: Long = savedStateHandle.get<Long>("receiptId") ?: 0L
    private val navImagePath: String? = if (receiptId == 0L) capturedImage.consume() else null

    private val _state = MutableStateFlow(
        ReviewState(receiptId = receiptId, imagePath = navImagePath)
    )
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    private val _savedId = MutableStateFlow<Long?>(null)
    val savedId: StateFlow<Long?> = _savedId.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        if (receiptId != 0L) {
            viewModelScope.launch {
                repository.getReceipt(receiptId)?.let { receipt ->
                    _state.value = receipt.toState(navImagePath ?: receipt.imagePath)
                }
            }
        }
    }

    fun onStoreNameChange(value: String) = _state.update { it.copy(storeName = value) }

    fun onBranchChange(value: String) = _state.update { it.copy(branch = value) }

    fun onDateChange(value: String) = _state.update { it.copy(dateText = value) }

    fun onReceiptNumberChange(value: String) = _state.update { it.copy(receiptNumber = value) }

    fun onCurrencyChange(value: String) = _state.update { it.copy(currency = value) }

    fun onSubtotalChange(value: String) = _state.update { it.copy(subtotalText = value) }

    fun onTaxChange(value: String) = _state.update { it.copy(taxText = value) }

    fun onTotalChange(value: String) = _state.update { it.copy(totalText = value) }

    fun addLineItem() = _state.update { it.copy(items = it.items + UiLineItem()) }

    fun onLineItemChange(index: Int, transform: (UiLineItem) -> UiLineItem) =
        _state.update { s ->
            s.copy(items = s.items.mapIndexed { i, item -> if (i == index) transform(item) else item })
        }

    fun removeLineItem(index: Int) =
        _state.update { s -> s.copy(items = s.items.filterIndexed { i, _ -> i != index }) }

    fun save() {
        if (_isSaving.value) return
        val s = _state.value
        val items = s.items.mapNotNull { item ->
            val quantity = item.quantityText.toDoubleOrNull() ?: 1.0
            val unitPrice = item.unitPriceText.toDoubleOrNull() ?: 0.0
            if (item.name.isBlank()) null
            else LineItem(
                name = item.name.trim(),
                quantity = quantity,
                unitPrice = unitPrice,
                totalPrice = quantity * unitPrice,
                category = item.category.trim(),
            )
        }
        val receipt = Receipt(
            id = s.receiptId,
            storeName = s.storeName.trim().ifEmpty { "Unknown" },
            branch = s.branch.trim().ifBlank { null },
            date = s.dateText.parseDateOrNull(),
            receiptNumber = s.receiptNumber.trim().ifBlank { null },
            items = items,
            subtotal = s.subtotalText.toDoubleOrNull(),
            tax = s.taxText.toDoubleOrNull(),
            total = s.totalText.toDoubleOrNull(),
            currency = s.currency.trim().ifBlank { "EUR" },
            imagePath = s.imagePath,
            createdAt = Instant.ofEpochMilli(s.createdAtMillis),
        )
        _isSaving.value = true
        viewModelScope.launch {
            val id = repository.saveReceipt(receipt)
            _isSaving.value = false
            _savedId.value = id
        }
    }

    fun deleteAndFinish() {
        if (receiptId == 0L) return
        viewModelScope.launch {
            repository.deleteReceipt(receiptId)
            _savedId.value = -1L
        }
    }

    private fun Receipt.toState(imagePath: String?): ReviewState = ReviewState(
        receiptId = id,
        imagePath = imagePath,
        storeName = storeName,
        branch = branch.orEmpty(),
        dateText = date?.atZone(ZoneOffset.UTC)?.toLocalDate()?.toString().orEmpty(),
        receiptNumber = receiptNumber.orEmpty(),
        currency = currency,
        items = items.map {
            UiLineItem(
                name = it.name,
                quantityText = it.quantity.toString(),
                unitPriceText = it.unitPrice.toString(),
                category = it.category,
            )
        },
        subtotalText = subtotal?.toString().orEmpty(),
        taxText = tax?.toString().orEmpty(),
        totalText = total?.toString().orEmpty(),
        createdAtMillis = createdAt.toEpochMilli(),
    )
}

private fun String.parseDateOrNull(): Instant? {
    if (isBlank()) return null
    return runCatching {
        LocalDate.parse(trim()).atStartOfDay(ZoneOffset.UTC).toInstant()
    }.getOrNull()
}
