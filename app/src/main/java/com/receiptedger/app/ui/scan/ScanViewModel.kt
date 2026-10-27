package com.receiptedger.app.ui.scan
import com.receiptedger.app.di.CapturedImageHolder
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val capturedImage: CapturedImageHolder,
) : ViewModel() {
    data class CapturedImage(val uri: String)

    private val _captured = MutableStateFlow<CapturedImage?>(null)
    val captured: StateFlow<CapturedImage?> = _captured

    fun onImageCaptured(uri: String) {
        _captured.value = CapturedImage(uri)
        capturedImage.set(uri)
    }

    fun clearCapture() {
        _captured.value = null
    }
}
