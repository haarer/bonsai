package com.receiptedger.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.receiptedger.app.R
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel: ReviewViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val savedId by viewModel.savedId.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(savedId) {
        savedId?.let {
            if (it < 0L) onDeleted() else onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.receiptId == 0L) R.string.review_new_receipt
                            else R.string.review_receipt
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.receiptId != 0L) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    TextButton(onClick = { viewModel.save() }, enabled = !isSaving) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.imagePath?.let { path ->
                AsyncImage(
                    model = File(path.removePrefix("file://")),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = state.storeName,
                        onValueChange = viewModel::onStoreNameChange,
                        label = { Text(stringResource(R.string.store_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.branch,
                            onValueChange = viewModel::onBranchChange,
                            label = { Text(stringResource(R.string.branch)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.dateText,
                            onValueChange = viewModel::onDateChange,
                            label = { Text(stringResource(R.string.date)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.receiptNumber,
                            onValueChange = viewModel::onReceiptNumberChange,
                            label = { Text(stringResource(R.string.receipt_number)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.currency,
                            onValueChange = viewModel::onCurrencyChange,
                            label = { Text(stringResource(R.string.currency)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Text(
                        stringResource(R.string.items),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                itemsIndexed(state.items) { index, item ->
                    LineItemCard(
                        item = item,
                        currency = state.currency,
                        onNameChange = { value ->
                            viewModel.onLineItemChange(index) { item -> item.copy(name = value) }
                        },
                        onQuantityChange = { value ->
                            viewModel.onLineItemChange(index) { item -> item.copy(quantityText = value) }
                        },
                        onPriceChange = { value ->
                            viewModel.onLineItemChange(index) { item -> item.copy(unitPriceText = value) }
                        },
                        onCategoryChange = { value ->
                            viewModel.onLineItemChange(index) { item -> item.copy(category = value) }
                        },
                        onRemove = { viewModel.removeLineItem(index) },
                    )
                }
                item {
                    TextButton(onClick = { viewModel.addLineItem() }) {
                        Text(stringResource(R.string.add_item))
                    }
                }
                item {
                    Text(
                        stringResource(R.string.total),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.subtotalText,
                            onValueChange = viewModel::onSubtotalChange,
                            label = { Text(stringResource(R.string.subtotal)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.taxText,
                            onValueChange = viewModel::onTaxChange,
                            label = { Text(stringResource(R.string.tax)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.totalText,
                            onValueChange = viewModel::onTotalChange,
                            label = { Text(stringResource(R.string.total)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_receipt)) },
            text = { Text(stringResource(R.string.delete_receipt_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAndFinish()
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun LineItemCard(
    item: UiLineItem,
    currency: String,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.item_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(R.string.quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = item.unitPriceText,
                    onValueChange = onPriceChange,
                    label = { Text(stringResource(R.string.price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.category,
                    onValueChange = onCategoryChange,
                    label = { Text(stringResource(R.string.category)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "%.2f %s",
                        item.computedTotal,
                        currency,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        }
    }
}
