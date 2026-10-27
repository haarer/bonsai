package com.receiptedger.app.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.receiptedger.app.R
import com.receiptedger.app.domain.model.Receipt
import java.time.ZoneOffset
import java.util.Locale

@Composable
fun LedgerScreen(onOpenReceipt: (Long) -> Unit) {
    val viewModel: LedgerViewModel = hiltViewModel()
    val receipts by viewModel.receipts.collectAsState()

    if (receipts.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.no_receipts),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(receipts, key = { it.id }) { receipt ->
            ReceiptCard(receipt = receipt, onClick = { onOpenReceipt(receipt.id) })
        }
    }
}

@Composable
private fun ReceiptCard(receipt: Receipt, onClick: () -> Unit) {
    val dateText = receipt.date?.atZone(ZoneOffset.UTC)?.toLocalDate()?.toString().orEmpty()
    val totalText = receipt.total?.let {
        String.format(Locale.getDefault(), "%.2f %s", it, receipt.currency)
    }.orEmpty()

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = receipt.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = totalText,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "%d items",
                        receipt.items.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
