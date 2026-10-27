package com.receiptedger.app.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptedger.app.domain.model.Receipt
import com.receiptedger.app.domain.model.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LedgerViewModel @Inject constructor(
    repository: ReceiptRepository,
) : ViewModel() {

    val receipts: StateFlow<List<Receipt>> =
        repository.observeReceipts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
