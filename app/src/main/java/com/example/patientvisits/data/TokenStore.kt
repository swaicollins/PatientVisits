package com.example.patientvisits.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

interface TokenStore {
    val tokenFlow: Flow<String?>
    suspend fun token(): String?
    suspend fun save(token: String)
    suspend fun clear()
}

class DataStoreTokenStore(private val context: Context) : TokenStore {

    private val key = stringPreferencesKey("access_token")

    override val tokenFlow: Flow<String?> = context.sessionDataStore.data.map { it[key] }

    override suspend fun token(): String? = tokenFlow.first()

    override suspend fun save(token: String) {
        context.sessionDataStore.edit { it[key] = token }
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { it.remove(key) }
    }
}
