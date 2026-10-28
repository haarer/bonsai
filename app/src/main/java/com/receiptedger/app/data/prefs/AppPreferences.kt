package com.receiptedger.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "receipt-ledger")

const val DEFAULT_MODEL = "qwen2.5-vl-3b"

data class AppSettings(
    val serverUrl: String = "",
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val settings: Flow<AppSettings> =
        context.appDataStore.data.map { prefs ->
            AppSettings(
                serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
                apiKey = prefs[Keys.API_KEY].orEmpty(),
                model = prefs[Keys.MODEL] ?: DEFAULT_MODEL,
            )
        }

    suspend fun setServerUrl(url: String) {
        context.appDataStore.edit { it[Keys.SERVER_URL] = url }
    }

    suspend fun setApiKey(key: String) {
        context.appDataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun setModel(model: String) {
        context.appDataStore.edit { it[Keys.MODEL] = model }
    }

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
    }

}
