package com.receiptedger.app.ui.review

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptedger.app.domain.model.ExtractedReceipt
import com.receiptedger.app.domain.model.LineItem
import com.receiptedger.app.domain.model.Receipt
import com.receiptedger.app.di.CapturedImageHolder
import com.receiptedger.app.domain.model.ReceiptRepository
import com.receiptedger.app.domain.model.VisionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
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
    val errorMessage: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

enum class ExtractionStatus { IDLE, RUNNING, DONE, FAILED }

/** Emitted once after save (positive id) or delete (negative id). */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReceiptRepository,
    private val capturedImage: CapturedImageHolder,
    private val vision: VisionProvider,
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

    private val _extractionStatus = MutableStateFlow(ExtractionStatus.IDLE)
    val extractionStatus: StateFlow<ExtractionStatus> = _extractionStatus.asStateFlow()

    private var currencyTouched = false

    init {
        if (receiptId != 0L) {
            viewModelScope.launch {
                repository.getReceipt(receiptId)?.let { receipt ->
                    _state.value = receipt.toState(navImagePath ?: receipt.imagePath)
                }
            }
        } else {
            navImagePath?.let { runExtraction(it) }
        }
    }

    private fun runExtraction(imagePath: String) {
        _extractionStatus.value = ExtractionStatus.RUNNING
        _state.update { it.copy(errorMessage = "") }
        viewModelScope.launch {
            try {
                val file = File(imagePath)
                if (!file.exists()) throw IOException("image file missing: $imagePath")
                val extracted = vision.extract(file, null)
                val touched = currencyTouched
                _state.update { it.applyExtraction(extracted, touched) }
                _extractionStatus.value = ExtractionStatus.DONE
            } catch (e: Exception) {
                Log.e("ReviewViewModel", "extraction failed", e)
                _state.update { it.copy(errorMessage = e.message ?: e.javaClass.simpleName) }
                _extractionStatus.value = ExtractionStatus.FAILED
            }
        }
    }

    fun retryExtraction() {
        if (receiptId == 0L) navImagePath?.let { runExtraction(it) }
    }

    fun onStoreNameChange(value: String) = _state.update { it.copy(storeName = value) }

    fun onBranchChange(value: String) = _state.update { it.copy(branch = value) }

    fun onDateChange(value: String) = _state.update { it.copy(dateText = value) }

    fun onReceiptNumberChange(value: String) = _state.update { it.copy(receiptNumber = value) }

    fun onCurrencyChange(value: String) {
        currencyTouched = true
        _state.update { it.copy(currency = value) }
    }

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

    private companion object {
        val ISO_DATE = Regex("^\\d{4}-\\d{2}-\\d{2}")
        val GERMAN_DATE = Regex("^(\\d{1,2})[./](\\d{1,2})[./](\\d{4})$")
    }

    private fun normalizeDate(raw: String): String {
        val s = raw.trim()
        ISO_DATE.find(s)?.let { return s.take(10) }
        GERMAN_DATE.find(s)?.let {
            val (day, month, year) = it.destructured
            return "${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
        }
        return s
    }

    private fun Double.displayText(): String = if (this == toLong().toDouble()) toLong().toString() else toString()

    /** Pre-fills fields still at their defaults; never clobbers user input. */
    private fun ReviewState.applyExtraction(e: ExtractedReceipt, currencyTouched: Boolean): ReviewState {
        val date = normalizeDate(e.date)
        return copy(
            storeName = storeName.ifBlank { e.storeName },
            branch = branch.ifBlank { e.branch },
            dateText = dateText.ifBlank { date },
            receiptNumber = receiptNumber.ifBlank { e.receiptNumber },
            currency = if (e.currency.isNotBlank() && (!currencyTouched || currency == "EUR")) e.currency else currency,
            items = if (items.isEmpty()) {
                e.items.map { UiLineItem(it.name, it.quantity.displayText(), it.unitPrice.displayText(), it.category) }
            } else {
                items
            },
            subtotalText = subtotalText.ifBlank { e.subtotal?.toString().orEmpty() },
            taxText = taxText.ifBlank { e.tax?.toString().orEmpty() },
            totalText = totalText.ifBlank { e.total?.toString().orEmpty() },
        )
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
