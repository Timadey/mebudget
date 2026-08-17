package com.mebudget.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Persists the auth session (PocketBase token + user profile) in DataStore so
 * the user stays signed in across app launches.
 */
class UserPreferences(private val context: Context) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    val authToken: Flow<String?> = context.authDataStore.data.map { it[AUTH_TOKEN_KEY] }

    val userId: Flow<String?> = context.authDataStore.data.map { it[USER_ID_KEY] }

    val userEmail: Flow<String?> = context.authDataStore.data.map { it[USER_EMAIL_KEY] }

    val userName: Flow<String?> = context.authDataStore.data.map { it[USER_NAME_KEY] }

    suspend fun saveAuthData(token: String, userId: String, email: String, name: String) {
        context.authDataStore.edit { prefs ->
            prefs[AUTH_TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_NAME_KEY] = name
        }
    }

    suspend fun clearAuthData() {
        context.authDataStore.edit { it.clear() }
    }
}
