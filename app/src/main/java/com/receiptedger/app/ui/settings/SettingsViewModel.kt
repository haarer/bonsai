package com.receiptedger.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptedger.app.data.prefs.AppSettings
import com.receiptedger.app.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        preferences.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun onServerUrlChange(url: String) {
        viewModelScope.launch { preferences.setServerUrl(url) }
    }

    fun onApiKeyChange(key: String) {
        viewModelScope.launch { preferences.setApiKey(key) }
    }

    fun onModelChange(model: String) {
        viewModelScope.launch { preferences.setModel(model) }
    }
}
