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

private const val DEFAULT_SERVER_URL = "http://10.0.2.2:8000"

data class AppSettings(
    val serverUrl: String = DEFAULT_SERVER_URL,
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val settings: Flow<AppSettings> =
        context.appDataStore.data.map { prefs ->
            AppSettings(serverUrl = prefs[Keys.SERVER_URL] ?: AppSettings().serverUrl)
        }

    suspend fun setServerUrl(url: String) {
        context.appDataStore.edit { it[Keys.SERVER_URL] = url }
    }

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
    }

}
